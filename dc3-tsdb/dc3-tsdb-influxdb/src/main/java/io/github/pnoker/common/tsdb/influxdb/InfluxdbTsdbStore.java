/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.tsdb.influxdb;

import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.CorrelationResult;
import io.github.pnoker.common.tsdb.model.TsdbModel.Cursor;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.DimensionCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.GroupDimension;
import io.github.pnoker.common.tsdb.model.TsdbModel.LatencyBin;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesLastSeen;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbQueryTimeout;
import io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate;
import io.github.pnoker.common.tsdb.spi.TsdbStore;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * InfluxDB 3 (Core/Enterprise, SQL interface) adapter for the TsdbStore port
 * (docs/design/tsdb-abstraction.md §7). Talks the documented v3 HTTP APIs
 * directly — line protocol for writes, query_sql for reads — with no client
 * library: the wire surface is two endpoints and a CSV dialect.
 *
 * <p>Mapping and dialect facts, each verified against influxdb:3.11.2-core:
 * <ul>
 *   <li>one measurement {@code point_value}; tags tenant_id/device_id/point_id;
 *       everything else is a field. Integer fields MUST carry the line-protocol
 *       {@code i} suffix from the very first write — a bare number binds the
 *       column to Float64 forever and destroys ns-precision timestamps.</li>
 *   <li>duplicate (series, time): line protocol keeps the later write — the
 *       port's last-write-wins policy.</li>
 *   <li>queries return CSV; timestamps are selected as
 *       {@code CAST(... AS BIGINT)} nanoseconds so integer precision survives
 *       the wire (JSON/scientific notation does not).</li>
 *   <li>bucketing via {@code date_bin} anchored to the epoch; window
 *       row_number for last-per-series; ordered {@code array_agg}[1] for
 *       deterministic FIRST/LAST (plain first_value has no order parameter).</li>
 *   <li>PERCENTILE exists only as an approximation — declared false; the
 *     analytics facade computes exact percentiles from bounded pulls. Row
 *     DELETE is unsupported in Core — deleteRange declared false; tenant
 *     offboarding falls back to store-native partition tooling (§4).</li>
 * </ul>
 *
 * @author pnoker
 * @since 2026.8.21
 */
@Slf4j
public final class InfluxdbTsdbStore implements TsdbStore {

    private static final int APPEND_CHUNK = 2000;

    private final String baseUrl;
    private final String token;
    private final String database;
    private final HttpClient http;

    public InfluxdbTsdbStore(String baseUrl, String token, String database) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.database = database;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        health();
        log.info("InfluxDB 3 store ready ({} db {})", baseUrl, database);
    }

    private void health() {
        try {
            HttpRequest request = request("/health").GET().build();
            http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("InfluxDB 3 unreachable at " + baseUrl + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String type() {
        return "influxdb";
    }

    @Override
    public TsdbCapabilities capabilities() {
        return new TsdbCapabilities(
                false, true, true, true, false,
                RollupSupport.NONE, APPEND_CHUNK,
                false, OrderingGuarantee.PER_SERIES, Precision.NANO, true, false);
    }

    // ===== 写入 =====

    @Override
    public int append(List<PointValueSample> samples) {
        if (samples.isEmpty()) {
            return 0;
        }
        int written = 0;
        for (int start = 0; start < samples.size(); start += APPEND_CHUNK) {
            List<PointValueSample> chunk = samples.subList(start, Math.min(start + APPEND_CHUNK, samples.size()));
            StringBuilder body = new StringBuilder(chunk.size() * 200);
            for (PointValueSample sample : chunk) {
                SeriesKey series = sample.series();
                body.append("point_value,tenant_id=").append(series.tenantId())
                        .append(",device_id=").append(series.deviceId())
                        .append(",point_id=").append(series.pointId())
                        .append(" raw=").append(escape(sample.rawValue()))
                        .append(",cal=").append(escape(sample.calValue()));
                if (Objects.nonNull(sample.numericValue())) {
                    // line protocol has no null literal — skip the field; the
                    // column simply reads back as null for this row.
                    body.append(",num=").append(sample.numericValue());
                }
                body.append(",quality=").append(sample.quality()).append('i')
                        .append(",message_id=").append(escape(sample.messageId()))
                        .append(",schema_version=").append(sample.schemaVersion()).append('i')
                        .append(",driver_node=").append(escape(sample.driverNode()))
                        .append(",sequence=").append(sample.sequence()).append('i')
                        .append(",fencing_token=").append(sample.fencingToken()).append('i')
                        .append(",driver_id=").append(sample.driverId()).append('i')
                        .append(",operate_time=").append(nanosOf(sample.receiveTime())).append('i')
                        .append(' ').append(nanosOf(sample.deviceTime()))
                        .append('\n');
            }
            execute(httpRequest("/api/v3/write_lp?db=" + database + "&precision=ns")
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build());
            written += chunk.size();
        }
        return written;
    }

    private static String escape(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    // ===== 读取 =====

    private static final String SAMPLE_COLUMNS = "tenant_id, device_id, point_id, raw, cal, num, quality, "
            + "message_id, schema_version, driver_node, sequence, fencing_token, driver_id, "
            + "CAST(time AS BIGINT) AS create_time_ns, operate_time";

    @Override
    public Map<SeriesKey, List<PointValueSample>> last(SeriesFilter filter, int limit, TsdbDeadline deadline) {
        String sql = """
                SELECT * FROM (
                    SELECT %s, ROW_NUMBER() OVER (
                        PARTITION BY tenant_id, device_id, point_id
                        ORDER BY CAST(time AS BIGINT) DESC, message_id DESC) AS rn
                    FROM point_value WHERE %s
                ) WHERE rn <= %d ORDER BY tenant_id, device_id, point_id, create_time_ns DESC"""
                .formatted(SAMPLE_COLUMNS, seriesWhere(filter), limit);
        Map<SeriesKey, List<PointValueSample>> result = new LinkedHashMap<>();
        for (CsvRow row : query(sql, deadline)) {
            PointValueSample sample = sampleOf(row);
            result.computeIfAbsent(sample.series(), key -> new ArrayList<>()).add(sample);
        }
        return result;
    }

    @Override
    public CursorPage<PointValueSample> history(SeriesFilter filter, TimeWindow window,
                                                Cursor cursor, int pageSize, TsdbDeadline deadline) {
        StringBuilder where = new StringBuilder(seriesWhere(filter))
                .append(" AND time >= ").append(literal(window.from()))
                .append(" AND time < ").append(literal(window.toExclusive()));
        if (Objects.nonNull(cursor)) {
            where.append(" AND (time < ").append(literal(cursor.deviceTime()))
                    .append(" OR (time = ").append(literal(cursor.deviceTime()))
                    .append(" AND message_id < ").append('\'').append(cursor.messageId().replace("'", "\\'")).append("'))");
        }
        String sql = "SELECT %s FROM point_value WHERE %s ORDER BY create_time_ns DESC, message_id DESC LIMIT %d"
                .formatted(SAMPLE_COLUMNS, where, pageSize + 1);
        List<PointValueSample> page = new ArrayList<>();
        for (CsvRow row : query(sql, deadline)) {
            page.add(sampleOf(row));
        }
        Cursor next = null;
        if (page.size() > pageSize) {
            page = new ArrayList<>(page.subList(0, pageSize));
            PointValueSample newest = page.get(pageSize - 1);
            next = new Cursor(newest.deviceTime(), newest.messageId());
        }
        return new CursorPage<>(page, next);
    }

    @Override
    public Map<SeriesKey, WindowAggregate> aggregate(SeriesFilter filter, AggregateFunction fn,
                                                     TimeWindow window, Double percentile,
                                                     TsdbDeadline deadline) {
        if (fn == AggregateFunction.PERCENTILE) {
            throw new UnsupportedOperationException("InfluxDB 3 has approximate percentiles only; "
                    + "the facade computes exact ones from bounded pulls");
        }
        String sql = """
                SELECT tenant_id, device_id, point_id, %s AS agg_value, count(*) AS sample_count
                FROM point_value WHERE %s AND time >= %s AND time < %s
                GROUP BY tenant_id, device_id, point_id"""
                .formatted(aggregateExpression(fn), seriesWhere(filter),
                        literal(window.from()), literal(window.toExclusive()));
        Map<SeriesKey, WindowAggregate> result = new LinkedHashMap<>();
        for (CsvRow row : query(sql, deadline)) {
            result.put(new SeriesKey(row.getLong("tenant_id"), row.getLong("device_id"), row.getLong("point_id")),
                    new WindowAggregate(row.getDouble("agg_value"), row.getLong("sample_count")));
        }
        return result;
    }

    @Override
    public Map<SeriesKey, List<BucketAggregate>> bucketedAggregate(SeriesFilter filter, AggregateFunction fn,
                                                                   TimeWindow window, Duration bucketWidth,
                                                                   Double percentile, TsdbDeadline deadline) {
        if (fn == AggregateFunction.PERCENTILE) {
            throw new UnsupportedOperationException("InfluxDB 3 has approximate percentiles only");
        }
        String sql = """
                SELECT tenant_id, device_id, point_id, CAST(%s AS BIGINT) AS bucket_ns,
                       %s AS agg_value, count(*) AS sample_count
                FROM point_value WHERE %s AND time >= %s AND time < %s
                GROUP BY tenant_id, device_id, point_id, bucket_ns
                ORDER BY tenant_id, device_id, point_id, bucket_ns"""
                .formatted(bucketExpression(bucketWidth), aggregateExpression(fn), seriesWhere(filter),
                        literal(window.from()), literal(window.toExclusive()));
        Map<SeriesKey, List<BucketAggregate>> result = new LinkedHashMap<>();
        for (CsvRow row : query(sql, deadline)) {
            result.computeIfAbsent(new SeriesKey(row.getLong("tenant_id"), row.getLong("device_id"),
                            row.getLong("point_id")), key -> new ArrayList<>())
                    .add(new BucketAggregate(instantOfNanos(row.getLong("bucket_ns")),
                            row.getDouble("agg_value"), row.getLong("sample_count")));
        }
        return result;
    }

    @Override
    public long count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline) {
        String sql = "SELECT count(*) AS c FROM point_value WHERE %s AND time >= %s AND time < %s"
                .formatted(seriesWhere(filter), literal(window.from()), literal(window.toExclusive()));
        List<CsvRow> rows = query(sql, deadline);
        return rows.isEmpty() ? 0 : rows.getFirst().getLong("c");
    }

    // ===== S13：租户级分析面 =====

    @Override
    public List<BucketAggregate> bucketedCount(long tenantId, TimeWindow window,
                                               Duration bucketWidth, TsdbDeadline deadline) {
        String sql = """
                SELECT CAST(%s AS BIGINT) AS bucket_ns, count(*) AS sample_count
                FROM point_value WHERE tenant_id = '%d' AND time >= %s AND time < %s
                GROUP BY bucket_ns ORDER BY bucket_ns"""
                .formatted(bucketExpression(bucketWidth), tenantId,
                        literal(window.from()), literal(window.toExclusive()));
        List<BucketAggregate> result = new ArrayList<>();
        for (CsvRow row : query(sql, deadline)) {
            result.add(new BucketAggregate(instantOfNanos(row.getLong("bucket_ns")), null, row.getLong("sample_count")));
        }
        return result;
    }

    @Override
    public List<DimensionCount> countByDimension(long tenantId, TimeWindow window,
                                                 GroupDimension dimension, int limit, TsdbDeadline deadline) {
        String column = switch (dimension) {
            case DEVICE -> "device_id";
            case POINT -> "point_id";
            case DRIVER -> "driver_id";
        };
        String sql = """
                SELECT %s AS entity_id, count(*) AS sample_count
                FROM point_value WHERE tenant_id = '%d' AND time >= %s AND time < %s
                GROUP BY entity_id ORDER BY sample_count DESC LIMIT %d"""
                .formatted(column, tenantId, literal(window.from()), literal(window.toExclusive()), limit);
        List<DimensionCount> result = new ArrayList<>();
        for (CsvRow row : query(sql, deadline)) {
            result.add(new DimensionCount(dimension, Long.parseLong(row.getString("entity_id")),
                    row.getLong("sample_count")));
        }
        return result;
    }

    @Override
    public List<SeriesCount> seriesCounts(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = """
                SELECT tenant_id, device_id, point_id, count(*) AS sample_count
                FROM point_value WHERE tenant_id = '%d' AND time >= %s AND time < %s
                GROUP BY tenant_id, device_id, point_id"""
                .formatted(tenantId, literal(window.from()), literal(window.toExclusive()));
        List<SeriesCount> result = new ArrayList<>();
        for (CsvRow row : query(sql, deadline)) {
            result.add(new SeriesCount(new SeriesKey(row.getLong("tenant_id"), row.getLong("device_id"),
                    row.getLong("point_id")), row.getLong("sample_count")));
        }
        return result;
    }

    @Override
    public List<SeriesLastSeen> lastSeenPerSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = """
                SELECT tenant_id, device_id, point_id, CAST(max(time) AS BIGINT) AS last_seen_ns
                FROM point_value WHERE tenant_id = '%d' AND time >= %s AND time < %s
                GROUP BY tenant_id, device_id, point_id"""
                .formatted(tenantId, literal(window.from()), literal(window.toExclusive()));
        List<SeriesLastSeen> result = new ArrayList<>();
        for (CsvRow row : query(sql, deadline)) {
            result.add(new SeriesLastSeen(new SeriesKey(row.getLong("tenant_id"), row.getLong("device_id"),
                    row.getLong("point_id")), instantOfNanos(row.getLong("last_seen_ns"))));
        }
        result.sort((a, b) -> b.lastSeen().compareTo(a.lastSeen()));
        return result;
    }

    @Override
    public List<LatencyBin> latencyHistogram(long tenantId, TimeWindow window,
                                             List<Long> binEdgesMs, TsdbDeadline deadline) {
        // operate_time and CAST(time AS BIGINT) are both ns integers; the diff
        // divided by 1e6 is the pipeline latency in whole milliseconds.
        StringBuilder bins = new StringBuilder("CASE ");
        for (int i = 0; i < binEdgesMs.size(); i++) {
            bins.append("WHEN (operate_time - CAST(time AS BIGINT)) / 1000000 < ").append(binEdgesMs.get(i))
                    .append(" THEN ").append(i).append(' ');
        }
        bins.append("ELSE ").append(binEdgesMs.size()).append(" END");
        String sql = """
                SELECT %s AS bin, count(*) AS sample_count
                FROM point_value WHERE tenant_id = '%d' AND time >= %s AND time < %s
                GROUP BY bin ORDER BY bin"""
                .formatted(bins, tenantId, literal(window.from()), literal(window.toExclusive()));
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (CsvRow row : query(sql, deadline)) {
            counts.put((int) row.getLong("bin"), row.getLong("sample_count"));
        }
        List<LatencyBin> result = new ArrayList<>();
        long lower = 0;
        List<Long> edges = new ArrayList<>(binEdgesMs);
        edges.add(Long.MAX_VALUE);
        for (int i = 0; i < edges.size(); i++) {
            result.add(new LatencyBin(lower, edges.get(i), counts.getOrDefault(i, 0L)));
            lower = edges.get(i);
        }
        return result;
    }

    // ===== 运维 =====

    @Override
    public List<SeriesKey> listSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = """
                SELECT DISTINCT tenant_id, device_id, point_id FROM point_value
                WHERE tenant_id = '%d' AND time >= %s AND time < %s"""
                .formatted(tenantId, literal(window.from()), literal(window.toExclusive()));
        List<SeriesKey> result = new ArrayList<>();
        for (CsvRow row : query(sql, deadline)) {
            result.add(new SeriesKey(row.getLong("tenant_id"), row.getLong("device_id"), row.getLong("point_id")));
        }
        return result;
    }

    @Override
    public void deleteRange(SeriesKey series, TimeWindow window) {
        throw new UnsupportedOperationException("InfluxDB 3 Core cannot delete rows; tenant offboarding "
                + "falls back to store-native partition tooling");
    }

    @Override
    public CorrelationResult correlation(SeriesKey a, SeriesKey b, TimeWindow window,
                                         Duration alignBucket, TsdbDeadline deadline) {
        throw new UnsupportedOperationException("InfluxDB 3 adapter declares correlation=false; "
                + "facades compute from bucketed pulls");
    }

    // ===== SQL 装配 =====

    private String seriesWhere(SeriesFilter filter) {
        if (filter.tenantWide()) {
            return "tenant_id = '" + filter.tenantId() + "'";
        }
        StringBuilder out = new StringBuilder("tenant_id = '" + filter.tenantId() + "' AND (");
        for (int i = 0; i < filter.series().size(); i++) {
            SeriesKey series = filter.series().get(i);
            out.append(i > 0 ? " OR " : "")
                    .append("(device_id = '").append(series.deviceId())
                    .append("' AND point_id = '").append(series.pointId()).append("')");
        }
        return out.append(")").toString();
    }

    private String aggregateExpression(AggregateFunction fn) {
        return switch (fn) {
            case AVG -> "avg(num)";
            case MIN -> "min(num)";
            case MAX -> "max(num)";
            case SUM -> "sum(num)";
            case COUNT -> "CAST(count(*) AS DOUBLE)";
            // first_value/last_value have no ORDER BY form; the ordered array
            // aggregate is the deterministic variant.
            case FIRST -> "(array_agg(num ORDER BY time))[1]";
            case LAST -> "(array_agg(num ORDER BY time DESC))[1]";
            case PERCENTILE -> throw new IllegalArgumentException("percentile unsupported on influxdb");
        };
    }

    private static String bucketExpression(Duration bucketWidth) {
        return "date_bin(%s, time, TIMESTAMP '1970-01-01T00:00:00Z')".formatted(intervalLiteral(bucketWidth));
    }

    /** Largest exactly-dividing unit; DataFusion intervals lack a generic millisecond form. */
    private static String intervalLiteral(Duration width) {
        long nanos = width.toNanos();
        if (nanos % 3_600_000_000_000L == 0) {
            return "INTERVAL '" + (nanos / 3_600_000_000_000L) + " hour'";
        }
        if (nanos % 60_000_000_000L == 0) {
            return "INTERVAL '" + (nanos / 60_000_000_000L) + " minute'";
        }
        if (nanos % 1_000_000_000L == 0) {
            return "INTERVAL '" + (nanos / 1_000_000_000L) + " second'";
        }
        return "INTERVAL '" + (nanos / 1_000_000L) + " millisecond'";
    }

    /** RFC3339 literal — InfluxDB 3 compares timestamps against string literals. */
    private static String literal(Instant instant) {
        return "TIMESTAMP '" + instant + "'";
    }

    private static long nanosOf(Instant instant) {
        return TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
    }

    private static Instant instantOfNanos(long nanos) {
        return Instant.ofEpochSecond(Math.floorDiv(nanos, 1_000_000_000L),
                Math.floorMod(nanos, 1_000_000_000L));
    }

    private PointValueSample sampleOf(CsvRow row) {
        return new PointValueSample(
                new SeriesKey(row.getLong("tenant_id"), row.getLong("device_id"), row.getLong("point_id")),
                instantOfNanos(row.getLong("create_time_ns")),
                instantOfNanos(row.getLong("operate_time")),
                row.getString("raw"), row.getString("cal"),
                row.getNullableDouble("num"), (int) row.getLong("quality"),
                row.getString("message_id"), (int) row.getLong("schema_version"),
                row.getString("driver_node"), row.getLong("sequence"),
                row.getLong("fencing_token"), row.getLong("driver_id"));
    }

    // ===== HTTP + CSV =====

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token);
    }

    private HttpRequest.Builder httpRequest(String path) {
        return request(path);
    }

    private String execute(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("InfluxDB 3 error [" + response.statusCode() + "]: "
                        + response.body().substring(0, Math.min(300, response.body().length())));
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("InfluxDB 3 request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("InfluxDB 3 request interrupted", e);
        }
    }

    /** CSV response: header row then data rows, RFC-4180 quoting. */
    private List<CsvRow> query(String sql, TsdbDeadline deadline) {
        String body = "{\"db\":\"" + database + "\",\"q\":"
                + com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.textNode(sql).toString()
                + ",\"format\":\"csv\"}";
        HttpRequest request = request("/api/v3/query_sql")
                .timeout(deadline.maxWait())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        long start = System.nanoTime();
        String response;
        try {
            HttpResponse<String> httpResponse = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() >= 400) {
                throw new IllegalStateException("InfluxDB 3 query error [" + httpResponse.statusCode() + "]: "
                        + httpResponse.body().substring(0, Math.min(300, httpResponse.body().length())));
            }
            response = httpResponse.body();
        } catch (java.net.http.HttpTimeoutException e) {
            throw new TsdbQueryTimeout("influxdb query exceeded " + deadline.maxWait(), e);
        } catch (IOException e) {
            throw new IllegalStateException("InfluxDB 3 query failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("InfluxDB 3 query interrupted", e);
        }
        log.debug("influxdb query took {}ms: {}", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), sql);
        return CsvRows.parse(response);
    }

    /** Minimal CSV row with typed accessors; values arrive as text and parse on demand. */
    static final class CsvRow {

        private final Map<String, String> values;

        CsvRow(Map<String, String> values) {
            this.values = values;
        }

        String getString(String column) {
            return values.get(column);
        }

        long getLong(String column) {
            String value = values.get(column);
            if (Objects.isNull(value) || value.isBlank()) {
                return 0L;
            }
            // Integer text must stay integer: routing exact int64 digits through
            // a double rounds at ~1.7e18 (the ns timestamps live there).
            if (value.matches("-?\\d+")) {
                return Long.parseLong(value);
            }
            return (long) Double.parseDouble(value);
        }

        Double getDouble(String column) {
            String value = values.get(column);
            if (Objects.isNull(value) || value.isBlank()) {
                return null;
            }
            return Double.parseDouble(value);
        }

        Double getNullableDouble(String column) {
            return getDouble(column);
        }
    }

    static final class CsvRows {

        private CsvRows() {
        }

        static List<CsvRow> parse(String csv) {
            List<CsvRow> rows = new ArrayList<>();
            List<String> lines = split(csv);
            if (lines.isEmpty()) {
                return rows;
            }
            List<String> header = splitLine(lines.getFirst());
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) {
                    continue;
                }
                List<String> fields = splitLine(lines.get(i));
                Map<String, String> row = new LinkedHashMap<>();
                for (int c = 0; c < header.size() && c < fields.size(); c++) {
                    row.put(header.get(c), fields.get(c));
                }
                rows.add(new CsvRow(row));
            }
            return rows;
        }

        private static List<String> split(String csv) {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean quoted = false;
            for (int i = 0; i < csv.length(); i++) {
                char c = csv.charAt(i);
                if (c == '"') {
                    quoted = !quoted;
                    current.append(c);
                } else if (c == '\n' && !quoted) {
                    lines.add(current.toString());
                    current.setLength(0);
                } else if (c != '\r') {
                    current.append(c);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private static List<String> splitLine(String line) {
            List<String> fields = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean quoted = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (quoted) {
                    if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else if (c == '"') {
                        quoted = false;
                    } else {
                        current.append(c);
                    }
                } else if (c == '"') {
                    quoted = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            fields.add(current.toString());
            return fields;
        }
    }
}

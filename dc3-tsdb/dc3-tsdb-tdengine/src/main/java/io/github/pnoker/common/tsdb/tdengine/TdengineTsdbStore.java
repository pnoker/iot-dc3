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

package io.github.pnoker.common.tsdb.tdengine;

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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TDengine 3.x adapter for the TsdbStore port (docs/design/tsdb-abstraction.md §7):
 * one supertable {@code point_value} keyed by the series tags
 * (tenant_id, device_id, point_id); one deterministic subtable
 * {@code pv_<tenant>_<device>_<point>} per series, auto-created on insert via
 * {@code USING ... TAGS}. The database is created with {@code PRECISION 'us'} so
 * microsecond instants round-trip exactly.
 *
 * <p><b>Timestamps never touch string form.</b> The REST driver serializes
 * {@code Timestamp} parameters in the client JVM zone while the server parses
 * them as UTC — a moving offset that would shift every write by the deployment
 * zone (and corrupt cursor pagination). Instants therefore travel as epoch-micro
 * integer literals (the database precision) both ways, and reads come back as
 * {@code CAST(ts AS BIGINT)} — verified symmetric against the 3.3.6.13 image.
 *
 * <ul>
 *   <li>duplicate (series, deviceTime): TDengine keeps the later write — the port's
 *       last-write-wins policy
 *   <li>bucketed aggregates via {@code INTERVAL}; empty buckets are omitted
 *       ({@code gapFill=false}); the duration literal is a bare number, which the
 *       database reads in its own (microsecond) precision
 *   <li>{@code latencyHistogram} and {@code correlation} are declared false — the
 *       REST driver has no reliable expression binning / SQL-side Pearson;
 *       facades degrade per the design
 *   <li>rollups: NONE in this extraction (stream computing arrives with S16)
 * </ul>
 *
 * @author pnoker
 * @since 2026.8.21
 */
@Slf4j
public final class TdengineTsdbStore implements TsdbStore {

    private static final int APPEND_CHUNK = 1000;

    private final String database;
    private final String stable;
    private final JdbcTemplate jdbc;

    private static final RowMapper<PointValueSample> SAMPLE_MAPPER = (rs, i) -> new PointValueSample(
            new SeriesKey(rs.getLong("tenant_id"), rs.getLong("device_id"), rs.getLong("point_id")),
            instantOfMicros(rs.getLong("create_time")), instantOfMicros(rs.getLong("operate_time")),
            rs.getString("raw_value"), rs.getString("cal_value"),
            Objects.nonNull(rs.getObject("num_value")) ? rs.getDouble("num_value") : null,
            rs.getInt("quality"),
            rs.getString("message_id"), rs.getInt("schema_version"),
            rs.getString("driver_node"), rs.getLong("sequence"),
            rs.getLong("fencing_token"), rs.getLong("driver_id"));

    public TdengineTsdbStore(DataSource dataSource, String database) {
        this.database = database;
        this.stable = database + ".point_value";
        this.jdbc = new JdbcTemplate(dataSource);
        bootstrap();
    }

    private void bootstrap() {
        jdbc.execute("CREATE DATABASE IF NOT EXISTS " + database + " PRECISION 'us' KEEP 180");
        jdbc.execute("""
                CREATE STABLE IF NOT EXISTS %s (
                    ts             TIMESTAMP,
                    raw_value      NCHAR(2048),
                    cal_value      NCHAR(2048),
                    num_value      DOUBLE,
                    quality        INT,
                    message_id     NCHAR(128),
                    schema_version INT,
                    driver_node    NCHAR(128),
                    sequence       BIGINT,
                    fencing_token  BIGINT,
                    driver_id      BIGINT,
                    operate_time   TIMESTAMP
                ) TAGS (
                    tenant_id BIGINT,
                    device_id BIGINT,
                    point_id  BIGINT
                )""".formatted(stable));
        log.info("TDengine store ready (stable {})", stable);
    }

    @Override
    public String type() {
        return "tdengine";
    }

    @Override
    public TsdbCapabilities capabilities() {
        return new TsdbCapabilities(
                false, true, true, false, true,
                RollupSupport.NONE, APPEND_CHUNK,
                true, OrderingGuarantee.PER_SERIES, Precision.MICRO, true, false);
    }

    // ===== writes =====

    @Override
    public int append(List<PointValueSample> samples) {
        if (samples.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int start = 0; start < samples.size(); start += APPEND_CHUNK) {
            List<PointValueSample> chunk = samples.subList(start, Math.min(start + APPEND_CHUNK, samples.size()));
            total += appendChunk(chunk);
        }
        return total;
    }

    /**
     * One multi-value INSERT per distinct subtable — the deterministic subtable name
     * plus {@code USING ... TAGS} auto-creates it on first write, so series discovery
     * needs no DDL bookkeeping. Duplicate timestamps keep the later row (TDengine
     * update semantics), matching the port's last-write-wins duplicate policy.
     */
    private int appendChunk(List<PointValueSample> chunk) {
        Map<SeriesKey, List<PointValueSample>> bySeries = new LinkedHashMap<>();
        for (PointValueSample sample : chunk) {
            bySeries.computeIfAbsent(sample.series(), key -> new ArrayList<>()).add(sample);
        }
        int written = 0;
        for (Map.Entry<SeriesKey, List<PointValueSample>> entry : bySeries.entrySet()) {
            List<PointValueSample> rows = entry.getValue();
            // Timestamps inline as epoch-micro literals (see class javadoc); the
            // remaining columns stay bound parameters.
            StringBuilder tuples = new StringBuilder();
            for (int i = 0; i < rows.size(); i++) {
                PointValueSample sample = rows.get(i);
                if (i > 0) {
                    tuples.append(' ');
                }
                tuples.append('(').append(microsOf(sample.deviceTime()))
                        .append(", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ")
                        .append(microsOf(sample.receiveTime())).append(')');
            }
            String sql = "INSERT INTO %s USING %s TAGS(%d, %d, %d) VALUES %s"
                    .formatted(subtable(entry.getKey()), stable,
                            entry.getKey().tenantId(), entry.getKey().deviceId(), entry.getKey().pointId(),
                            tuples);
            written += jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                int index = 1;
                for (PointValueSample sample : rows) {
                    ps.setString(index++, sample.rawValue());
                    ps.setString(index++, sample.calValue());
                    if (Objects.nonNull(sample.numericValue())) {
                        ps.setDouble(index++, sample.numericValue());
                    } else {
                        ps.setObject(index++, null);
                    }
                    ps.setInt(index++, sample.quality());
                    ps.setString(index++, sample.messageId());
                    ps.setInt(index++, sample.schemaVersion());
                    ps.setString(index++, sample.driverNode());
                    ps.setLong(index++, sample.sequence());
                    ps.setLong(index++, sample.fencingToken());
                    ps.setLong(index++, sample.driverId());
                }
                return ps;
            });
        }
        return written;
    }

    // ===== reads =====

    @Override
    public Map<SeriesKey, List<PointValueSample>> last(SeriesFilter filter, int limit, TsdbDeadline deadline) {
        requireSeriesOrScan(filter);
        String sql = """
                SELECT tenant_id, device_id, point_id, message_id, schema_version, driver_node,
                       sequence, fencing_token, raw_value, cal_value, num_value, quality,
                       driver_id, CAST(ts AS BIGINT) AS create_time, CAST(operate_time AS BIGINT) AS operate_time
                FROM %s WHERE %s PARTITION BY tbname ORDER BY ts DESC, message_id DESC LIMIT ?"""
                .formatted(stable, seriesWhere(filter));
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        args.add(limit);
        Map<SeriesKey, List<PointValueSample>> result = new LinkedHashMap<>();
        for (PointValueSample sample : timed(deadline, () -> jdbc.query(sql, SAMPLE_MAPPER, args.toArray()))) {
            result.computeIfAbsent(sample.series(), key -> new ArrayList<>()).add(sample);
        }
        return result;
    }

    @Override
    public CursorPage<PointValueSample> history(SeriesFilter filter, TimeWindow window,
                                                Cursor cursor, int pageSize, TsdbDeadline deadline) {
        requireSeriesOrScan(filter);
        StringBuilder sql = new StringBuilder("""
                SELECT tenant_id, device_id, point_id, message_id, schema_version, driver_node,
                       sequence, fencing_token, raw_value, cal_value, num_value, quality,
                       driver_id, CAST(ts AS BIGINT) AS create_time, CAST(operate_time AS BIGINT) AS operate_time
                FROM %s WHERE %s AND ts >= %d AND ts < %d"""
                .formatted(stable, seriesWhere(filter), microsOf(window.from()), microsOf(window.toExclusive())));
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        if (Objects.nonNull(cursor)) {
            sql.append(" AND (ts < ").append(microsOf(cursor.deviceTime()))
                    .append(" OR (ts = ").append(microsOf(cursor.deviceTime()))
                    .append(" AND message_id < ?))");
            args.add(cursor.messageId());
        }
        sql.append(" ORDER BY ts DESC, message_id DESC LIMIT ?");
        args.add(pageSize + 1);
        List<PointValueSample> page = timed(deadline,
                () -> jdbc.query(sql.toString(), SAMPLE_MAPPER, args.toArray()));
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
        // TDengine's PERCENTILE refuses supertable queries; the deterministic
        // subtable name lets single-series percentiles run table-direct.
        if (fn == AggregateFunction.PERCENTILE) {
            return percentileAggregates(filter, window, percentile, deadline);
        }
        String sql = """
                SELECT tenant_id, device_id, point_id, %s AS agg_value, COUNT(*) AS sample_count
                FROM %s WHERE %s AND ts >= %d AND ts < %d
                GROUP BY tenant_id, device_id, point_id"""
                .formatted(aggregateExpression(fn, percentile), stable, seriesWhere(filter),
                        microsOf(window.from()), microsOf(window.toExclusive()));
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        Map<SeriesKey, WindowAggregate> result = new LinkedHashMap<>();
        timedVoid(deadline, () -> {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            for (Map<String, Object> row : rows) {
                Number value = (Number) row.get("agg_value");
                result.put(seriesOf(row),
                        new WindowAggregate(Objects.isNull(value) ? null : value.doubleValue(),
                                ((Number) row.get("sample_count")).longValue()));
            }
        });
        return result;
    }

    private Map<SeriesKey, WindowAggregate> percentileAggregates(SeriesFilter filter, TimeWindow window,
                                                                 Double percentile, TsdbDeadline deadline) {
        if (filter.tenantWide()) {
            throw new IllegalArgumentException(
                    "TDengine percentile needs explicit series; tenant-wide percentile is unsupported");
        }
        Map<SeriesKey, WindowAggregate> result = new LinkedHashMap<>();
        for (SeriesKey series : filter.series()) {
            String sql = "SELECT %s AS agg_value, COUNT(*) AS sample_count FROM %s WHERE ts >= %d AND ts < %d"
                    .formatted(aggregateExpression(AggregateFunction.PERCENTILE, percentile), subtable(series),
                            microsOf(window.from()), microsOf(window.toExclusive()));
            timedVoid(deadline, () -> {
                Map<String, Object> row = jdbc.queryForMap(sql);
                Number value = (Number) row.get("agg_value");
                result.put(series, new WindowAggregate(Objects.isNull(value) ? null : value.doubleValue(),
                        ((Number) row.get("sample_count")).longValue()));
            });
        }
        return result;
    }

    @Override
    public Map<SeriesKey, List<BucketAggregate>> bucketedAggregate(SeriesFilter filter, AggregateFunction fn,
                                                                   TimeWindow window, Duration bucketWidth,
                                                                   Double percentile, TsdbDeadline deadline) {
        // TDengine PERCENTILE refuses supertable queries (including INTERVAL
        // windows); per-series subtable scans keep it exact on the raw path —
        // this store declares rollupSupport=NONE, so tier reads never apply.
        if (fn == AggregateFunction.PERCENTILE) {
            Map<SeriesKey, List<BucketAggregate>> result = new LinkedHashMap<>();
            if (filter.tenantWide()) {
                throw new IllegalArgumentException(
                        "TDengine percentile needs explicit series; tenant-wide percentile is unsupported");
            }
            for (SeriesKey key : filter.series()) {
                String sql = "SELECT CAST(_wstart AS BIGINT) AS bucket, %s AS agg_value, COUNT(*) AS sample_count FROM %s WHERE ts >= %d AND ts < %d INTERVAL(%d)"
                        .formatted(aggregateExpression(fn, percentile), subtable(key),
                                microsOf(window.from()), microsOf(window.toExclusive()),
                                bucketWidth.toNanos() / 1000);
                timedVoid(deadline, () -> {
                    List<Map<String, Object>> rows = jdbc.queryForList(sql);
                    List<BucketAggregate> buckets = new ArrayList<>();
                    for (Map<String, Object> row : rows) {
                        buckets.add(new BucketAggregate(instantOfMicros(((Number) row.get("bucket")).longValue()),
                                Objects.nonNull(row.get("agg_value")) ? ((Number) row.get("agg_value")).doubleValue() : null,
                                ((Number) row.get("sample_count")).longValue()));
                    }
                    if (!buckets.isEmpty()) {
                        result.put(key, buckets);
                    }
                });
            }
            return result;
        }
        String sql = """
                SELECT tenant_id, device_id, point_id, CAST(_wstart AS BIGINT) AS bucket,
                       %s AS agg_value, COUNT(*) AS sample_count
                FROM %s WHERE %s AND ts >= %d AND ts < %d
                PARTITION BY tbname INTERVAL(%d)"""
                .formatted(aggregateExpression(fn, percentile), stable, seriesWhere(filter),
                        microsOf(window.from()), microsOf(window.toExclusive()), bucketWidth.toNanos() / 1000);
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        Map<SeriesKey, List<BucketAggregate>> result = new LinkedHashMap<>();
        timedVoid(deadline, () -> {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            for (Map<String, Object> row : rows) {
                result.computeIfAbsent(seriesOf(row), key -> new ArrayList<>())
                        .add(new BucketAggregate(instantOfMicros(((Number) row.get("bucket")).longValue()),
                                Objects.nonNull(row.get("agg_value")) ? ((Number) row.get("agg_value")).doubleValue() : null,
                                ((Number) row.get("sample_count")).longValue()));
            }
        });
        return result;
    }

    @Override
    public long count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline) {
        String sql = "SELECT COUNT(*) FROM " + stable + " WHERE " + seriesWhere(filter)
                + " AND ts >= " + microsOf(window.from()) + " AND ts < " + microsOf(window.toExclusive());
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        Long value = timed(deadline, () -> jdbc.queryForObject(sql, Long.class, args.toArray()));
        return Objects.requireNonNullElse(value, 0L);
    }

    // ===== S13: tenant-level analytics =====

    @Override
    public List<BucketAggregate> bucketedCount(long tenantId, TimeWindow window,
                                               Duration bucketWidth, TsdbDeadline deadline) {
        String sql = """
                SELECT CAST(_wstart AS BIGINT) AS bucket, COUNT(*) AS sample_count FROM %s
                WHERE tenant_id = ? AND ts >= %d AND ts < %d INTERVAL(%d)"""
                .formatted(stable, microsOf(window.from()), microsOf(window.toExclusive()),
                        bucketWidth.toNanos() / 1000);
        Object[] args = {tenantId};
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new BucketAggregate(
                instantOfMicros(rs.getLong(1)), null, rs.getLong(2)), args));
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
                SELECT %s AS entity_id, COUNT(*) AS sample_count FROM %s
                WHERE tenant_id = ? AND ts >= %d AND ts < %d
                GROUP BY %s ORDER BY sample_count DESC LIMIT ?"""
                .formatted(column, stable, microsOf(window.from()), microsOf(window.toExclusive()), column);
        Object[] args = {tenantId, limit};
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new DimensionCount(dimension,
                rs.getLong(1), rs.getLong(2)), args));
    }

    @Override
    public List<SeriesCount> seriesCounts(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = """
                SELECT tenant_id, device_id, point_id, COUNT(*) AS sample_count FROM %s
                WHERE tenant_id = ? AND ts >= %d AND ts < %d
                GROUP BY tenant_id, device_id, point_id"""
                .formatted(stable, microsOf(window.from()), microsOf(window.toExclusive()));
        Object[] args = {tenantId};
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new SeriesCount(
                new SeriesKey(rs.getLong(1), rs.getLong(2), rs.getLong(3)), rs.getLong(4)), args));
    }

    @Override
    public List<SeriesLastSeen> lastSeenPerSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = """
                SELECT tenant_id, device_id, point_id, CAST(LAST(ts) AS BIGINT) AS last_seen FROM %s
                WHERE tenant_id = ? AND ts >= %d AND ts < %d
                GROUP BY tenant_id, device_id, point_id"""
                .formatted(stable, microsOf(window.from()), microsOf(window.toExclusive()));
        Object[] args = {tenantId};
        List<SeriesLastSeen> rows = timed(deadline, () -> jdbc.query(sql, (rs, i) -> new SeriesLastSeen(
                new SeriesKey(rs.getLong(1), rs.getLong(2), rs.getLong(3)), instantOfMicros(rs.getLong(4))), args));
        // The port contract orders by recency; TDengine cannot ORDER BY an aggregate
        // across partitions, so fold it here.
        rows.sort((a, b) -> b.lastSeen().compareTo(a.lastSeen()));
        return rows;
    }

    @Override
    public List<LatencyBin> latencyHistogram(long tenantId, TimeWindow window,
                                             List<Long> binEdgesMs, TsdbDeadline deadline) {
        throw new UnsupportedOperationException(
                "TDengine adapter declares latencyHistogram=false; facades degrade to zero-filled bins");
    }

    // ===== operations =====

    @Override
    public List<SeriesKey> listSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = """
                SELECT DISTINCT tenant_id, device_id, point_id FROM %s
                WHERE tenant_id = ? AND ts >= %d AND ts < %d""".formatted(stable,
                microsOf(window.from()), microsOf(window.toExclusive()));
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new SeriesKey(
                rs.getLong(1), rs.getLong(2), rs.getLong(3)), tenantId));
    }

    @Override
    public void deleteRange(SeriesKey series, TimeWindow window) {
        // DELETE bounds in TDengine are inclusive; emulate [from, toExclusive) by
        // pulling the upper bound back one microsecond (the database precision).
        jdbc.update("DELETE FROM " + subtable(series)
                + " WHERE ts >= " + microsOf(window.from())
                + " AND ts <= " + microsOf(window.toExclusive().minusNanos(1000)));
    }

    @Override
    public CorrelationResult correlation(SeriesKey a, SeriesKey b, TimeWindow window,
                                         Duration alignBucket, TsdbDeadline deadline) {
        throw new UnsupportedOperationException(
                "TDengine adapter declares correlation=false; facades compute from bucketed pulls");
    }

    // ===== helpers =====

    private String subtable(SeriesKey series) {
        return database + ".pv_" + series.tenantId() + "_" + series.deviceId() + "_" + series.pointId();
    }

    private static SeriesKey seriesOf(Map<String, Object> row) {
        return new SeriesKey(((Number) row.get("tenant_id")).longValue(),
                ((Number) row.get("device_id")).longValue(),
                ((Number) row.get("point_id")).longValue());
    }

    private static long microsOf(Instant instant) {
        return instant.getEpochSecond() * 1_000_000L + instant.getNano() / 1_000L;
    }

    private static Instant instantOfMicros(long micros) {
        return Instant.ofEpochSecond(Math.floorDiv(micros, 1_000_000L),
                Math.floorMod(micros, 1_000_000L) * 1000L);
    }

    private void requireSeriesOrScan(SeriesFilter filter) {
        if (filter.tenantWide() && !capabilities().tenantWideScan()) {
            throw new IllegalArgumentException("tenant-wide scan not supported by this store");
        }
    }

    private String seriesWhere(SeriesFilter filter) {
        if (filter.tenantWide()) {
            return "tenant_id = ?";
        }
        StringBuilder out = new StringBuilder("tenant_id = ? AND (");
        for (int i = 0; i < filter.series().size(); i++) {
            out.append(i > 0 ? " OR " : "").append("(device_id = ? AND point_id = ?)");
        }
        return out.append(")").toString();
    }

    private List<Object> seriesArgs(SeriesFilter filter) {
        List<Object> args = new ArrayList<>();
        args.add(filter.tenantId());
        if (!filter.tenantWide()) {
            filter.series().forEach(s -> {
                args.add(s.deviceId());
                args.add(s.pointId());
            });
        }
        return args;
    }

    private String aggregateExpression(AggregateFunction fn, Double percentile) {
        return switch (fn) {
            case AVG -> "AVG(num_value)";
            case MIN -> "MIN(num_value)";
            case MAX -> "MAX(num_value)";
            case SUM -> "SUM(num_value)";
            case COUNT -> "CAST(COUNT(*) AS DOUBLE)";
            case FIRST -> "FIRST(num_value)";
            case LAST -> "LAST(num_value)";
            case PERCENTILE -> "PERCENTILE(num_value, " + (Objects.requireNonNull(percentile,
                    "percentile required for PERCENTILE") * 100) + ")";
        };
    }

    private void timedVoid(TsdbDeadline deadline, Runnable query) {
        timed(deadline, () -> {
            query.run();
            return null;
        });
    }

    private <T> T timed(TsdbDeadline deadline, java.util.function.Supplier<T> query) {
        try {
            jdbc.setQueryTimeout((int) Math.max(1, deadline.maxWait().toSeconds()));
        } catch (DataAccessException ignored) {
            // the REST statement path may not support server-side timeouts; the
            // deadline contract stays wall-clock bounded
        }
        try {
            return query.get();
        } catch (DataAccessException e) {
            if (Objects.nonNull(e.getCause()) && String.valueOf(e.getCause()).toLowerCase().contains("timeout")) {
                throw new TsdbQueryTimeout("tdengine query exceeded " + deadline.maxWait(), e);
            }
            throw e;
        }
    }
}

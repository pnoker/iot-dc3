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

package io.github.pnoker.common.tsdb.iotdb;

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
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.read.common.RowRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Apache IoTDB 2.x adapter for the TsdbStore port (docs/design/tsdb-abstraction.md
 * §7): one tree path per series — {@code root.dc3.t<tenant>.d<device>.p<point>}
 * (the t/d/p prefixes work around IoTDB rejecting purely numeric node names) —
 * with the sample envelope as measurements. The server MUST run with
 * {@code timestamp_precision=us} (a one-line iotdb-system.properties override);
 * the adapter reads and writes epoch-micro timestamps in that unit.
 *
 * <p>Honest capability notes (each verified against apache/iotdb:2.0.10):
 * <ul>
 *   <li>duplicate (series, time): the later write overwrites — the port's
 *       last-write-wins policy.</li>
 *   <li>ORDER BY supports time only — cursor pagination and last() cannot
 *       tie-break on message id; samples sharing one timestamp within a series
 *       are avoided by the platform's (series, time) uniqueness.</li>
 *   <li>window GROUP BY anchors windows at the query start, so the adapter
 *       aligns the start down to a multiple of the bucket width; empty window
 *       rows come back as null and are dropped.</li>
 *   <li>COUNT rides on {@code message_id} (never null) so it counts rows, not
 *       just numeric values; AVG/MIN/MAX/SUM read {@code num} and skip nulls.</li>
 *   <li>countByDimension: DEVICE and POINT map to GROUP BY LEVEL=3/4 on the path
 *       tree; DRIVER is not a path level and is refused rather than faked.</li>
 *   <li>latencyHistogram and correlation are declared false; PERCENTILE does not
 *     exist — the analytics facade computes both from bounded pulls.</li>
 * </ul>
 *
 * @author pnoker
 * @since 2026.8.21
 */
@Slf4j
public final class IotdbTsdbStore implements TsdbStore, AutoCloseable {

    private static final int APPEND_CHUNK = 2000;

    private static final long TTL_MS = Duration.ofDays(180).toMillis();

    private static final String ROOT = "root.dc3";

    private static final List<String> MEASUREMENTS = List.of("raw", "cal", "num", "quality",
            "message_id", "schema_version", "driver_node", "sequence", "fencing_token",
            "driver_id", "operate_time");

    private static final List<TSDataType> TYPES = List.of(TSDataType.TEXT, TSDataType.TEXT,
            TSDataType.DOUBLE, TSDataType.INT32, TSDataType.TEXT, TSDataType.INT32,
            TSDataType.TEXT, TSDataType.INT64, TSDataType.INT64, TSDataType.INT64, TSDataType.INT64);

    private static final Pattern SERIES_IN_COLUMN = Pattern.compile(
            "root\\.dc3\\.t(\\d+)(?:\\.d(\\d+))?(?:\\.p(\\d+))?");

    private final Session session;

    public IotdbTsdbStore(String host, int port, String username, String password) {
        this.session = new Session.Builder().host(host).port(port)
                .username(username).password(password)
                .fetchSize(10000)
                // pinned to the given endpoint: IoTDB 2.x node discovery hands
                // out internal cluster addresses, which breaks behind port
                // mappings and single-node deployments alike
                .enableRedirection(false)
                .build();
        try {
            session.open();
            // Idempotent: raw retention 180 days on the whole dc3 subtree; the
            // adapter has no rollup tiers, so raw is all there is.
            session.executeNonQueryStatement("SET TTL TO " + ROOT + " " + TTL_MS);
        } catch (Exception e) {
            throw new IllegalStateException("IoTDB session setup failed: " + e.getMessage(), e);
        }
        log.info("IoTDB store ready ({}:{})", host, port);
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (IoTDBConnectionException e) {
            log.warn("IoTDB session close failed: {}", e.getMessage());
        }
    }

    @Override
    public String type() {
        return "iotdb";
    }

    @Override
    public TsdbCapabilities capabilities() {
        return new TsdbCapabilities(
                false, true, true, false, false,
                RollupSupport.NONE, APPEND_CHUNK,
                true, OrderingGuarantee.PER_SERIES, Precision.MICRO, true, false);
    }

    // ===== writes =====

    @Override
    public int append(List<PointValueSample> samples) {
        if (samples.isEmpty()) {
            return 0;
        }
        int written = 0;
        for (int start = 0; start < samples.size(); start += APPEND_CHUNK) {
            List<PointValueSample> chunk = samples.subList(start, Math.min(start + APPEND_CHUNK, samples.size()));
            List<String> devices = new ArrayList<>(chunk.size());
            List<Long> times = new ArrayList<>(chunk.size());
            List<List<String>> measurements = new ArrayList<>(chunk.size());
            List<List<TSDataType>> types = new ArrayList<>(chunk.size());
            List<List<Object>> values = new ArrayList<>(chunk.size());
            for (PointValueSample sample : chunk) {
                devices.add(path(sample.series()));
                times.add(microsOf(sample.deviceTime()));
                // Null measurements must be omitted per record — a null value in
                // insertRecords makes the whole record (and batch tail) vanish.
                List<String> recordMeasurements = new ArrayList<>(MEASUREMENTS);
                List<TSDataType> recordTypes = new ArrayList<>(TYPES);
                List<Object> recordValues = new ArrayList<>(List.of(sample.rawValue(),
                        sample.calValue(), (Object) sample.quality(), sample.messageId(),
                        (Object) sample.schemaVersion(), sample.driverNode(),
                        (Object) sample.sequence(), (Object) sample.fencingToken(),
                        (Object) sample.driverId(), (Object) microsOf(sample.receiveTime())));
                if (Objects.nonNull(sample.numericValue())) {
                    recordValues.add(2, sample.numericValue());
                } else {
                    recordMeasurements.remove("num");
                    recordTypes.remove(2);
                }
                measurements.add(recordMeasurements);
                types.add(recordTypes);
                values.add(recordValues);
            }
            try {
                session.insertRecords(devices, times, measurements, types, values);
            } catch (IoTDBConnectionException | StatementExecutionException e) {
                throw new IllegalStateException("IoTDB insert failed: " + e.getMessage(), e);
            }
            written += chunk.size();
        }
        return written;
    }

    // ===== reads =====

    @Override
    public Map<SeriesKey, List<PointValueSample>> last(SeriesFilter filter, int limit, TsdbDeadline deadline) {
        Map<SeriesKey, List<PointValueSample>> result = new LinkedHashMap<>();
        for (SeriesKey series : seriesOf(filter)) {
            String sql = "SELECT * FROM " + path(series) + " ORDER BY TIME DESC LIMIT " + limit;
            for (Row row : query(sql, deadline)) {
                result.computeIfAbsent(series, key -> new ArrayList<>()).add(sampleOf(series, row));
            }
        }
        return result;
    }

    @Override
    public CursorPage<PointValueSample> history(SeriesFilter filter, TimeWindow window,
                                                Cursor cursor, int pageSize, TsdbDeadline deadline) {
        // Per-series newest pages merged globally; IoTDB cannot tie-break equal
        // timestamps across paths, so the merge is by time only.
        List<PointValueSample> merged = new ArrayList<>();
        long cutoff = Objects.nonNull(cursor) ? microsOf(cursor.deviceTime()) : Long.MAX_VALUE;
        for (SeriesKey series : seriesOf(filter)) {
            String sql = "SELECT * FROM " + path(series)
                    + " WHERE time >= " + timeLiteral(window.from())
                    + " AND time < " + timeLiteral(window.toExclusive())
                    + " AND time < " + timeLiteral(instantOfMicros(cutoff))
                    + " ORDER BY TIME DESC LIMIT " + (pageSize + 1);
            for (Row row : query(sql, deadline)) {
                merged.add(sampleOf(series, row));
            }
        }
        merged.sort(Comparator.comparing(PointValueSample::deviceTime).reversed());
        Cursor next = null;
        if (merged.size() > pageSize) {
            PointValueSample edge = merged.get(pageSize - 1);
            next = new Cursor(edge.deviceTime(), edge.messageId());
        }
        return new CursorPage<>(merged.size() > pageSize
                ? new ArrayList<>(merged.subList(0, pageSize)) : merged, next);
    }

    @Override
    public Map<SeriesKey, WindowAggregate> aggregate(SeriesFilter filter, AggregateFunction fn,
                                                     TimeWindow window, Double percentile,
                                                     TsdbDeadline deadline) {
        String expr = switch (fn) {
            case AVG -> "AVG(num)";
            case MIN -> "MIN_VALUE(num)";
            case MAX -> "MAX_VALUE(num)";
            case SUM -> "SUM(num)";
            case COUNT -> "COUNT(quality)";
            case FIRST -> "FIRST_VALUE(num)";
            case LAST -> "LAST_VALUE(num)";
            case PERCENTILE -> throw new UnsupportedOperationException(
                    "IoTDB has no percentile; the facade computes exact ones from bounded pulls");
        };
        // One result row, one column per (path, function); pair the value and the
        // row-count columns by the path embedded in each column name. COUNT is its
        // own row count — a second COUNT column would pair against itself.
        String sql = "SELECT " + expr + (fn == AggregateFunction.COUNT ? "" : ", COUNT(quality)")
                + " FROM " + pathsOf(filter)
                + " WHERE time >= " + timeLiteral(window.from())
                + " AND time < " + timeLiteral(window.toExclusive());
        Map<SeriesKey, WindowAggregate> result = new LinkedHashMap<>();
        boolean counting = fn == AggregateFunction.COUNT;
        for (Row row : query(sql, deadline)) {
            row.cells().forEach((series, cell) -> {
                if (counting) {
                    // the single COUNT column is both the value and the count
                    result.put(series, new WindowAggregate(cell.countColumn() || Objects.isNull(cell.value())
                            ? cell.count() * 1.0 : cell.value(), cell.count()));
                } else {
                    result.put(series, new WindowAggregate(cell.value(), cell.count()));
                }
            });
        }
        return result;
    }

    @Override
    public Map<SeriesKey, List<BucketAggregate>> bucketedAggregate(SeriesFilter filter, AggregateFunction fn,
                                                                   TimeWindow window, Duration bucketWidth,
                                                                   Double percentile, TsdbDeadline deadline) {
        if (fn == AggregateFunction.PERCENTILE) {
            throw new UnsupportedOperationException("IoTDB has no percentile");
        }
        String expr = switch (fn) {
            case AVG -> "AVG(num)";
            case MIN -> "MIN_VALUE(num)";
            case MAX -> "MAX_VALUE(num)";
            case SUM -> "SUM(num)";
            case COUNT -> "COUNT(quality)";
            case FIRST -> "FIRST_VALUE(num)";
            case LAST -> "LAST_VALUE(num)";
            default -> throw new UnsupportedOperationException("IoTDB has no percentile");
        };
        long width = bucketWidth.toMillis();
        long alignedFrom = alignDown(microsOf(window.from()), width * 1000);
        // COUNT buckets: the single column is both the aggregate value and the
        // sample count; other functions read the (value, count) pair per series.
        boolean counting = fn == AggregateFunction.COUNT;
        String sql = "SELECT " + expr + (counting ? "" : ", COUNT(quality)") + " FROM " + pathsOf(filter)
                + " WHERE time >= " + timeLiteral(instantOfMicros(alignedFrom))
                + " AND time < " + timeLiteral(window.toExclusive())
                + " GROUP BY ([" + alignedFrom + ", " + microsOf(window.toExclusive()) + "), " + width + "ms)";
        Map<SeriesKey, List<BucketAggregate>> result = new LinkedHashMap<>();
        for (Row row : query(sql, deadline)) {
            if (row.timestamp() < microsOf(window.from())) {
                continue;
            }
            row.cells().forEach((series, cell) -> {
                // COUNT buckets carry one count-only column; its value is the count
                long sampleCount = counting ? cell.count() : cell.count();
                Double value = counting ? sampleCount * 1.0 : cell.value();
                if (Objects.isNull(value)) {
                    return;
                }
                result.computeIfAbsent(series, key -> new ArrayList<>())
                        .add(new BucketAggregate(instantOfMicros(row.timestamp()), value, sampleCount));
            });
        }
        return result;
    }

    @Override
    public long count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline) {
        String sql = "SELECT COUNT(quality) FROM " + pathsOf(filter)
                + " WHERE time >= " + timeLiteral(window.from())
                + " AND time < " + timeLiteral(window.toExclusive());
        long total = 0;
        for (Row row : query(sql, deadline)) {
            total += row.totalCounts();
        }
        return total;
    }

    // ===== S13: tenant-level analytics =====

    @Override
    public List<BucketAggregate> bucketedCount(long tenantId, TimeWindow window,
                                               Duration bucketWidth, TsdbDeadline deadline) {
        long width = bucketWidth.toMillis();
        long alignedFrom = alignDown(microsOf(window.from()), width * 1000);
        String sql = "SELECT COUNT(quality) FROM " + tenantRoot(tenantId)
                + " WHERE time >= " + alignedFrom
                + " AND time < " + microsOf(window.toExclusive())
                + " GROUP BY ([" + alignedFrom + ", " + microsOf(window.toExclusive()) + "), " + width + "ms)";
        Map<Long, Long> perBucket = new LinkedHashMap<>();
        for (Row row : query(sql, deadline)) {
            if (row.timestamp() < microsOf(window.from()) || row.totalCounts() == 0) {
                continue;
            }
            perBucket.merge(row.timestamp(), row.totalCounts(), Long::sum);
        }
        List<BucketAggregate> result = new ArrayList<>(perBucket.size());
        perBucket.forEach((bucket, total) -> result.add(new BucketAggregate(instantOfMicros(bucket), null, total)));
        return result;
    }

    @Override
    public List<DimensionCount> countByDimension(long tenantId, TimeWindow window,
                                                 GroupDimension dimension, int limit, TsdbDeadline deadline) {
        // Path levels: root=0, dc3=1, tenant=2, device=3, point=4. driver_id is a
        // measurement, not a path level — refusing beats misreporting.
        if (dimension == GroupDimension.DRIVER) {
            throw new UnsupportedOperationException(
                    "IoTDB cannot group by driver — it is a measurement, not a path level");
        }
        int level = dimension == GroupDimension.DEVICE ? 3 : 4;
        String sql = "SELECT COUNT(quality) FROM " + tenantRoot(tenantId)
                + " WHERE time >= " + timeLiteral(window.from())
                + " AND time < " + timeLiteral(window.toExclusive())
                + " GROUP BY LEVEL=" + level;
        // GROUP BY LEVEL collapses paths to the requested level and the column
        // names turn wildcard-shaped (root.*.*.d777.*); the literal d/p token IS
        // the entity id, one column per entity.
        java.util.regex.Pattern token = java.util.regex.Pattern.compile(
                dimension == GroupDimension.DEVICE ? "d(\\d+)" : "p(\\d+)");
        List<DimensionCount> counts = new ArrayList<>();
        for (Row row : query(sql, deadline)) {
            List<String> names = row.columnNames();
            for (int i = 0; i < names.size(); i++) {
                Matcher matcher = token.matcher(names.get(i));
                if (!matcher.find()) {
                    continue;
                }
                counts.add(new DimensionCount(dimension, Long.parseLong(matcher.group(1)),
                        row.longAt(i)));
            }
        }
        counts.sort(Comparator.comparingLong(DimensionCount::count).reversed());
        return counts.size() > limit ? new ArrayList<>(counts.subList(0, limit)) : counts;
    }

    @Override
    public List<SeriesCount> seriesCounts(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = "SELECT COUNT(quality) FROM " + tenantRoot(tenantId)
                + " WHERE time >= " + timeLiteral(window.from())
                + " AND time < " + timeLiteral(window.toExclusive());
        List<SeriesCount> result = new ArrayList<>();
        for (Row row : query(sql, deadline)) {
            row.cells().forEach((series, cell) -> {
                if (cell.count() > 0) {
                    result.add(new SeriesCount(series, cell.count()));
                }
            });
        }
        return result;
    }

    @Override
    public List<SeriesLastSeen> lastSeenPerSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = "SELECT MAX_TIME(message_id) FROM " + tenantRoot(tenantId)
                + " WHERE time >= " + timeLiteral(window.from())
                + " AND time < " + timeLiteral(window.toExclusive());
        List<SeriesLastSeen> result = new ArrayList<>();
        for (Row row : query(sql, deadline)) {
            // MAX_TIME's value is the field; the result row itself has no time
            List<String> names = row.columnNames();
            for (int i = 0; i < names.size(); i++) {
                SeriesKey series = seriesOfPath(names.get(i));
                long lastSeen = row.longAt(i);
                if (Objects.nonNull(series) && lastSeen > 0) {
                    result.add(new SeriesLastSeen(series, instantOfMicros(lastSeen)));
                }
            }
        }
        result.sort((a, b) -> b.lastSeen().compareTo(a.lastSeen()));
        return result;
    }

    @Override
    public List<LatencyBin> latencyHistogram(long tenantId, TimeWindow window,
                                             List<Long> binEdgesMs, TsdbDeadline deadline) {
        throw new UnsupportedOperationException("IoTDB adapter declares latencyHistogram=false; "
                + "facades degrade to zero-filled bins");
    }

    // ===== operations =====

    @Override
    public List<SeriesKey> listSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        return lastSeenPerSeries(tenantId, window, deadline).stream().map(SeriesLastSeen::series).toList();
    }

    @Override
    public void deleteRange(SeriesKey series, TimeWindow window) {
        try {
            // IoTDB deleteData bounds are inclusive; pull the upper bound back one
            // microsecond (the server precision) to keep [from, toExclusive).
            session.deleteData(List.of(path(series)), microsOf(window.from()),
                    microsOf(window.toExclusive()) - 1);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new IllegalStateException("IoTDB deleteData failed: " + e.getMessage(), e);
        }
    }

    @Override
    public CorrelationResult correlation(SeriesKey a, SeriesKey b, TimeWindow window,
                                         Duration alignBucket, TsdbDeadline deadline) {
        throw new UnsupportedOperationException("IoTDB adapter declares correlation=false; "
                + "facades compute from bucketed pulls");
    }

    // ===== helpers =====

    private static String path(SeriesKey series) {
        return ROOT + ".t" + series.tenantId() + ".d" + series.deviceId() + ".p" + series.pointId();
    }

    private static String tenantRoot(long tenantId) {
        return ROOT + ".t" + tenantId + ".**";
    }

    private String pathsOf(SeriesFilter filter) {
        if (filter.tenantWide()) {
            return tenantRoot(filter.tenantId());
        }
        StringBuilder out = new StringBuilder();
        for (SeriesKey series : filter.series()) {
            if (!out.isEmpty()) {
                out.append(", ");
            }
            out.append(path(series));
        }
        return out.toString();
    }

    private static List<SeriesKey> seriesOf(SeriesFilter filter) {
        return filter.tenantWide() ? List.of() : filter.series();
    }

    private static long alignDown(long value, long unit) {
        return Math.floorDiv(value, unit) * unit;
    }

    private static long microsOf(Instant instant) {
        return instant.getEpochSecond() * 1_000_000L + instant.getNano() / 1_000L;
    }

    /**
     * WHERE time literal. A bare epoch number is parsed by IoTDB as milliseconds
     * regardless of the server's timestamp precision — verified against
     * 2.0.10-standalone: a microsecond literal in a WHERE clause silently
     * matches nothing. The offset-qualified ISO form carries the exact instant.
     */
    private static String timeLiteral(Instant instant) {
        return instant.toString().replace("Z", "+00:00");
    }

    private static Instant instantOfMicros(long micros) {
        return Instant.ofEpochSecond(Math.floorDiv(micros, 1_000_000L),
                Math.floorMod(micros, 1_000_000L) * 1000L);
    }

    private static SeriesKey seriesOfPath(String path) {
        Matcher matcher = SERIES_IN_COLUMN.matcher(path);
        if (!matcher.find()) {
            return null;
        }
        return new SeriesKey(Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)),
                Long.parseLong(matcher.group(3)));
    }

    /**
     * SELECT * column order is IoTDB's (alphabetical), not the insert order —
     * always resolve sample fields by measurement name.
     */
    private PointValueSample sampleOf(SeriesKey series, Row row) {
        Map<String, org.apache.tsfile.read.common.Field> fields = row.measurements();
        return new PointValueSample(series, instantOfMicros(row.timestamp()),
                instantOfMicros(row.longOf(fields, "operate_time")),
                row.stringOf(fields, "raw"), row.stringOf(fields, "cal"),
                row.doubleOf(fields, "num"), (int) row.longOf(fields, "quality"),
                row.stringOf(fields, "message_id"), (int) row.longOf(fields, "schema_version"),
                row.stringOf(fields, "driver_node"), row.longOf(fields, "sequence"),
                row.longOf(fields, "fencing_token"), row.longOf(fields, "driver_id"));
    }

    /**
     * Executes a query and folds the multi-column-per-series result shape into
     * rows keyed by the series embedded in the column name. IoTDB returns one
     * column per (path, aggregate); the sample-shaped SELECT * layout is fixed
     * by the adapter, so index-based access is safe there.
     */
    private List<Row> query(String sql, TsdbDeadline deadline) {
        long deadlineAt = System.nanoTime() + deadline.maxWait().toNanos();
        List<Row> rows = new ArrayList<>();
        try (SessionDataSet dataset = session.executeQueryStatement(sql)) {
            List<String> columns = dataset.getColumnNames();
            while (dataset.hasNext()) {
                if (System.nanoTime() > deadlineAt) {
                    throw new TsdbQueryTimeout("iotdb query exceeded " + deadline.maxWait());
                }
                RowRecord record = dataset.next();
                rows.add(new Row(record.getTimestamp(), columns, record));
            }
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new IllegalStateException("IoTDB query failed: " + e.getMessage(), e);
        } catch (Exception e) {
            // hasNext/next declare Exception on the 2.x dataset interface
            throw new IllegalStateException("IoTDB query failed: " + e.getMessage(), e);
        }
        return rows;
    }

    /**
     * One result row: timestamp plus typed accessors over the fixed sample layout
     * or name-derived series/value views for aggregate shapes.
     */
    static final class Row {

        private final long timestamp;
        /**
         * Result column names; IoTDB prefixes a "Time" entry the field list omits.
         */
        private final List<String> columns;
        private final List<String> fieldColumns;
        private final RowRecord record;

        Row(long timestamp, List<String> columns, RowRecord record) {
            this.timestamp = timestamp;
            this.columns = columns;
            this.fieldColumns = !columns.isEmpty() && "Time".equals(columns.getFirst())
                    ? columns.subList(1, columns.size()) : columns;
            this.record = record;
        }

        long timestamp() {
            return timestamp;
        }

        List<String> columnNames() {
            return fieldColumns;
        }

        String stringAt(int index) {
            Object value = valueAt(index);
            return Objects.isNull(value) ? null : String.valueOf(value);
        }

        long longAt(int index) {
            Number value = numberOf(valueAt(index));
            return Objects.isNull(value) ? 0L : value.longValue();
        }

        Double nullableDoubleAt(int index) {
            Number value = numberOf(valueAt(index));
            return Objects.isNull(value) ? null : value.doubleValue();
        }

        /**
         * Aggregate columns may come back typed differently per function; the
         * typed getters on Field throw when the backing slot is unset.
         */
        private Object valueAt(int index) {
            org.apache.tsfile.read.common.Field field = fieldAt(index);
            if (Objects.isNull(field)) {
                return null;
            }
            try {
                return field.getObjectValue(field.getDataType());
            } catch (Exception e) {
                return null;
            }
        }

        private org.apache.tsfile.read.common.Field fieldAt(int index) {
            if (index < 0 || index >= fieldColumns.size() || index >= record.getFields().size()) {
                return null;
            }
            org.apache.tsfile.read.common.Field field = record.getFields().get(index);
            return Objects.isNull(field) || field.getDataType() == TSDataType.UNKNOWN ? null : field;
        }

        /**
         * Measurement-name keyed view of a SELECT * row (last path segment).
         */
        Map<String, org.apache.tsfile.read.common.Field> measurements() {
            Map<String, org.apache.tsfile.read.common.Field> out = new LinkedHashMap<>();
            for (int i = 0; i < fieldColumns.size() && i < record.getFields().size(); i++) {
                String column = fieldColumns.get(i);
                int slash = column.lastIndexOf('.');
                String measurement = slash < 0 ? column : column.substring(slash + 1);
                org.apache.tsfile.read.common.Field field = fieldAt(i);
                if (Objects.nonNull(field)) {
                    out.put(measurement, field);
                }
            }
            return out;
        }

        String stringOf(Map<String, org.apache.tsfile.read.common.Field> fields, String name) {
            Object value = objectOf(fields.get(name));
            return Objects.isNull(value) ? null : String.valueOf(value);
        }

        long longOf(Map<String, org.apache.tsfile.read.common.Field> fields, String name) {
            Number value = (Number) objectOf(fields.get(name));
            return Objects.isNull(value) ? 0L : value.longValue();
        }

        Double doubleOf(Map<String, org.apache.tsfile.read.common.Field> fields, String name) {
            Number value = (Number) objectOf(fields.get(name));
            return Objects.isNull(value) ? null : value.doubleValue();
        }

        private Object objectOf(org.apache.tsfile.read.common.Field field) {
            if (Objects.isNull(field)) {
                return null;
            }
            try {
                return field.getObjectValue(field.getDataType());
            } catch (Exception e) {
                return null;
            }
        }

        private static Number numberOf(Object value) {
            if (value instanceof Number number) {
                return number;
            }
            if (value instanceof String text && text.matches("-?\\d+(\\.\\d+)?")) {
                return new java.math.BigDecimal(text);
            }
            return null;
        }

        /**
         * Series of the first field column, for aggregate result shapes.
         */
        SeriesKey series() {
            return fieldColumns.isEmpty() ? null : seriesOfPath(fieldColumns.getFirst());
        }

        Double value(int fieldIndex) {
            return nullableDoubleAt(fieldIndex);
        }

        /**
         * Second column of the (value, count) aggregate pair, when selected.
         */
        long count() {
            return columns.size() > 1 ? longAt(1) : 1L;
        }

        record Cell(Double value, long count, boolean countColumn) {
        }

        /**
         * Series-keyed cells for aggregate shapes; a series may carry one value
         * column and one count column.
         */
        Map<SeriesKey, Cell> cells() {
            Map<SeriesKey, Cell> cells = new LinkedHashMap<>();
            for (int i = 0; i < fieldColumns.size() && i < record.getFields().size(); i++) {
                SeriesKey series = seriesOfPath(fieldColumns.get(i));
                if (Objects.isNull(series)) {
                    continue;
                }
                org.apache.tsfile.read.common.Field field = fieldAt(i);
                if (Objects.isNull(field)) {
                    continue;
                }
                boolean countColumn = fieldColumns.get(i).startsWith("COUNT(");
                // COUNT over TEXT may surface as a string ("4") — parse it back
                Number number = numberOf(objectOf(field));
                if (Objects.isNull(number)) {
                    continue;
                }
                Cell existing = cells.get(series);
                if (countColumn) {
                    cells.put(series, new Cell(Objects.isNull(existing) ? null : existing.value(),
                            number.longValue(), true));
                } else {
                    cells.put(series, new Cell(number.doubleValue(),
                            Objects.isNull(existing) ? 0L : existing.count(), false));
                }
            }
            return cells;
        }

        Map<SeriesKey, WindowAggregate> aggregateRow() {
            Map<SeriesKey, WindowAggregate> out = new LinkedHashMap<>();
            cells().forEach((series, cell) -> out.put(series,
                    new WindowAggregate(cell.value(), cell.count())));
            return out;
        }

        long totalCounts() {
            long total = 0;
            for (int i = 0; i < record.getFields().size(); i++) {
                Number number = numberOf(valueAt(i));
                if (Objects.nonNull(number)) {
                    total += number.longValue();
                }
            }
            return total;
        }

        long keys(int level) {
            // path levels: tenant group 1, device group 2, point group 3 in the
            // pattern; LEVEL=3 (device) columns stop after the d segment
            for (String column : fieldColumns) {
                Matcher matcher = SERIES_IN_COLUMN.matcher(column);
                if (matcher.find()) {
                    String value = matcher.group(level - 1);
                    if (Objects.nonNull(value)) {
                        return Long.parseLong(value);
                    }
                }
            }
            return 0;
        }
    }
}

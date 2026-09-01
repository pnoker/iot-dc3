package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.tsdb.model.TsdbModel.Cursor;
import io.github.pnoker.common.tsdb.model.TsdbModel.CursorPage;
import io.github.pnoker.common.tsdb.model.TsdbModel.AggregateFunction;
import io.github.pnoker.common.tsdb.model.TsdbModel.BucketAggregate;
import io.github.pnoker.common.tsdb.model.TsdbModel.CorrelationResult;
import io.github.pnoker.common.tsdb.model.TsdbModel.DimensionCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.GroupDimension;
import io.github.pnoker.common.tsdb.model.TsdbModel.LatencyBin;
import io.github.pnoker.common.tsdb.model.TsdbModel.PointValueSample;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesCount;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesLastSeen;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesFilter;
import io.github.pnoker.common.tsdb.model.TsdbModel.SeriesKey;
import io.github.pnoker.common.tsdb.model.TsdbModel.TimeWindow;
import io.github.pnoker.common.tsdb.model.TsdbModel.TsdbDeadline;
import io.github.pnoker.common.tsdb.model.TsdbModel.WindowAggregate;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Native R2DBC TSDB port for the relational/Timescale deployment. */
@Repository
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcTsdbStore implements ReactiveTsdbStore {

    private static final String COLUMNS = "tenant_id,device_id,point_id,message_id,schema_version,driver_node,"
            + "sequence,fencing_token,raw_value,cal_value,num_value,quality,driver_id,create_time,operate_time";
    private static final int MAX_LAST_LIMIT = 500;
    private static final int MAX_HISTORY_PAGE_SIZE = 5000;
    private static final int MAX_DIMENSION_LIMIT = 500;

    private final DatabaseClient databaseClient;
    private final R2dbcDialect dialect;
    private final TransactionalOperator transactionalOperator;

    private String table() {
        return "dc3_history.dc3_point_value";
    }

    @Override
    public String type() {
        return "r2dbc";
    }

    @Override
    public io.github.pnoker.common.tsdb.spi.TsdbStore.TsdbCapabilities capabilities() {
        return new io.github.pnoker.common.tsdb.spi.TsdbStore.TsdbCapabilities(true, true, true, true, true,
                io.github.pnoker.common.tsdb.spi.TsdbStore.RollupSupport.NONE, 5000, true,
                io.github.pnoker.common.tsdb.spi.TsdbStore.OrderingGuarantee.PER_SERIES,
                io.github.pnoker.common.tsdb.spi.TsdbStore.Precision.MICRO, true, true);
    }

    @Override
    public Mono<Integer> append(List<PointValueSample> samples) {
        if (samples == null || samples.isEmpty()) return Mono.error(new IllegalArgumentException("samples must not be empty"));
        if (samples.stream().anyMatch(Objects::isNull)) return Mono.error(new IllegalArgumentException("samples must not contain null"));
        if (samples.size() > capabilities().maxAppendBatch()) {
            return Mono.error(new IllegalArgumentException("samples exceed max append batch of " + capabilities().maxAppendBatch()));
        }
        List<PointValueSample> rows = List.copyOf(samples);
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(table()).append(" (").append(COLUMNS).append(") VALUES ");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sql.append(',');
            sql.append("(:t").append(i).append(",:d").append(i).append(",:p").append(i).append(",:m").append(i)
                    .append(",:sv").append(i).append(",:n").append(i).append(",:s").append(i).append(",:f").append(i)
                    .append(",:raw").append(i).append(",:cal").append(i).append(",:num").append(i).append(",:q").append(i)
                    .append(",:dr").append(i).append(",:ct").append(i).append(",:rt").append(i).append(')');
        }
        if (dialect.name().toLowerCase().contains("mysql")) {
            sql.append(" ON DUPLICATE KEY UPDATE message_id=VALUES(message_id),schema_version=VALUES(schema_version),driver_node=VALUES(driver_node),sequence=VALUES(sequence),fencing_token=VALUES(fencing_token),raw_value=VALUES(raw_value),cal_value=VALUES(cal_value),num_value=VALUES(num_value),quality=VALUES(quality),driver_id=VALUES(driver_id),operate_time=VALUES(operate_time)");
        } else {
            sql.append(" ON CONFLICT (tenant_id,device_id,point_id,create_time) DO UPDATE SET message_id=EXCLUDED.message_id,schema_version=EXCLUDED.schema_version,driver_node=EXCLUDED.driver_node,sequence=EXCLUDED.sequence,fencing_token=EXCLUDED.fencing_token,raw_value=EXCLUDED.raw_value,cal_value=EXCLUDED.cal_value,num_value=EXCLUDED.num_value,quality=EXCLUDED.quality,driver_id=EXCLUDED.driver_id,operate_time=EXCLUDED.operate_time");
        }
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (int i = 0; i < rows.size(); i++) {
            PointValueSample row = rows.get(i);
            spec = spec.bind("t" + i, row.series().tenantId()).bind("d" + i, row.series().deviceId())
                    .bind("p" + i, row.series().pointId()).bind("m" + i, row.messageId())
                    .bind("sv" + i, row.schemaVersion()).bind("n" + i, row.driverNode())
                    .bind("s" + i, row.sequence()).bind("f" + i, row.fencingToken())
                    .bind("raw" + i, row.rawValue()).bind("cal" + i, row.calValue())
                    .bind("q" + i, row.quality()).bind("dr" + i, row.driverId())
                    .bind("ct" + i, dialect.bindInstant(row.deviceTime()));
            spec = bindNullable(spec, "num" + i, row.numericValue(), Double.class);
            Object receiveTime = row.receiveTime() == null ? null : dialect.bindInstant(row.receiveTime());
            spec = receiveTime == null ? spec.bindNull("rt" + i, Instant.class) : spec.bind("rt" + i, receiveTime);
        }
        return spec.fetch().rowsUpdated().map(Long::intValue).as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Map<SeriesKey, List<PointValueSample>>> last(SeriesFilter filter, int limit, TsdbDeadline deadline) {
        if (limit < 1 || limit > MAX_LAST_LIMIT) {
            return Mono.error(new IllegalArgumentException("limit must be between 1 and " + MAX_LAST_LIMIT));
        }
        int bounded = limit;
        String predicates = predicates(filter, null);
        String sql = "SELECT " + COLUMNS + " FROM (SELECT " + COLUMNS + ", ROW_NUMBER() OVER (PARTITION BY tenant_id,device_id,point_id ORDER BY create_time DESC,message_id DESC) AS rn FROM " + table() + " WHERE " + predicates + ") ranked WHERE rn <= :limit ORDER BY create_time DESC,message_id DESC";
        return query(sql, filter, null).bind("limit", bounded).map(this::map).all()
                .collectMultimap(value -> value.series(), value -> value)
                .map(values -> {
                    Map<SeriesKey, List<PointValueSample>> result = new LinkedHashMap<>();
                    values.forEach((key, rows) -> result.put(key, List.copyOf(rows)));
                    return result;
                })
                .timeout(deadline.maxWait());
    }

    @Override
    public Mono<CursorPage<PointValueSample>> history(SeriesFilter filter, TimeWindow window, Cursor cursor,
                                                       int pageSize, TsdbDeadline deadline) {
        if (pageSize < 1 || pageSize > MAX_HISTORY_PAGE_SIZE) {
            return Mono.error(new IllegalArgumentException("pageSize must be between 1 and " + MAX_HISTORY_PAGE_SIZE));
        }
        int bounded = pageSize;
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM ").append(table())
                .append(" WHERE ").append(predicates(filter, window));
        if (cursor != null) {
            if (cursor.series() == null) {
                sql.append(" AND (create_time < :cursor_time OR (create_time = :cursor_time AND message_id < :cursor_id))");
            } else {
                sql.append(" AND (create_time,tenant_id,device_id,point_id,message_id) < (:cursor_time,:cursor_tenant,:cursor_device,:cursor_point,:cursor_id)");
            }
        }
        sql.append(" ORDER BY create_time DESC,tenant_id DESC,device_id DESC,point_id DESC,message_id DESC LIMIT :limit");
        DatabaseClient.GenericExecuteSpec spec = query(sql.toString(), filter, window).bind("limit", bounded + 1);
        if (cursor != null) {
            spec = spec.bind("cursor_time", cursor.deviceTime()).bind("cursor_id", cursor.messageId());
            if (cursor.series() != null) {
                spec = spec.bind("cursor_tenant", cursor.series().tenantId())
                        .bind("cursor_device", cursor.series().deviceId())
                        .bind("cursor_point", cursor.series().pointId());
            }
        }
        return spec.map(this::map).all().collectList().map(rows -> {
                    boolean hasNext = rows.size() > bounded;
                    List<PointValueSample> page = hasNext ? rows.subList(0, bounded) : rows;
                    return new CursorPage<>(List.copyOf(page), hasNext
                            ? new Cursor(page.getLast().deviceTime(), page.getLast().messageId(), page.getLast().series()) : null);
                })
                .timeout(deadline.maxWait());
    }

    @Override
    public Mono<Long> count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline) {
        return query("SELECT COUNT(*) AS value FROM " + table() + " WHERE " + predicates(filter, window), filter, window)
                .map((row, metadata) -> number(row.get("value"))).one().timeout(deadline.maxWait());
    }

    @Override
    public Flux<SeriesCount> seriesCounts(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        if (tenantId <= 0) return Flux.error(new IllegalArgumentException("tenantId must be positive"));
        String sql = "SELECT tenant_id,device_id,point_id,COUNT(*) AS value FROM " + table()
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time AND create_time<:to_time"
                + " GROUP BY tenant_id,device_id,point_id ORDER BY device_id,point_id";
        return databaseClient.sql(sql).bind("tenant_id", tenantId).bind("from_time", dialect.bindInstant(window.from()))
                .bind("to_time", dialect.bindInstant(window.toExclusive())).map((row, metadata) -> new SeriesCount(
                        new SeriesKey(number(row.get("tenant_id")), number(row.get("device_id")), number(row.get("point_id"))),
                number(row.get("value")))).all().timeout(deadline.maxWait());
    }

    @Override
    public Mono<Map<SeriesKey, WindowAggregate>> aggregate(SeriesFilter filter, AggregateFunction fn,
                                                            TimeWindow window, Double percentile,
                                                            TsdbDeadline deadline) {
        io.github.pnoker.common.tsdb.spi.TsdbStore.validatePercentile(fn, percentile);
        String expression = aggregateExpression(fn, percentile);
        String sql = "SELECT tenant_id,device_id,point_id," + expression
                + " AS value,COUNT(*) AS sample_count FROM " + table() + " WHERE "
                + predicates(filter, window) + " GROUP BY tenant_id,device_id,point_id";
        return query(sql, filter, window).map((row, metadata) -> Map.entry(
                new SeriesKey(number(row.get("tenant_id")), number(row.get("device_id")), number(row.get("point_id"))),
                new WindowAggregate(decimal(row.get("value")), number(row.get("sample_count"))))).all()
                .collectMap(Map.Entry::getKey, Map.Entry::getValue, LinkedHashMap::new)
                .timeout(deadline.maxWait());
    }

    @Override
    public Mono<Map<SeriesKey, List<BucketAggregate>>> bucketedAggregate(SeriesFilter filter,
                                                                           AggregateFunction fn,
                                                                           TimeWindow window,
                                                                           Duration bucketWidth,
                                                                           Double percentile,
                                                                           TsdbDeadline deadline) {
        io.github.pnoker.common.tsdb.spi.TsdbStore.validatePercentile(fn, percentile);
        if (bucketWidth == null || bucketWidth.isZero() || bucketWidth.isNegative()) {
            return Mono.error(new IllegalArgumentException("bucketWidth must be positive"));
        }
        String expression = aggregateExpression(fn, percentile);
        String sql = "SELECT tenant_id,device_id,point_id,time_bucket(CAST(:bucket_width AS interval),create_time) AS bucket,"
                + expression + " AS value,COUNT(*) AS sample_count FROM " + table() + " WHERE "
                + predicates(filter, window) + " GROUP BY tenant_id,device_id,point_id,bucket"
                + " ORDER BY bucket ASC";
        DatabaseClient.GenericExecuteSpec spec = query(sql, filter, window)
                .bind("bucket_width", bucketWidth.toMillis() + " milliseconds");
        return spec.map((row, metadata) -> Map.entry(
                new SeriesKey(number(row.get("tenant_id")), number(row.get("device_id")), number(row.get("point_id"))),
                new BucketAggregate(instant(row.get("bucket")), decimal(row.get("value")), number(row.get("sample_count"))))).all()
                .collectMultimap(Map.Entry::getKey, Map.Entry::getValue)
                .map(values -> {
                    Map<SeriesKey, List<BucketAggregate>> result = new LinkedHashMap<>();
                    values.forEach((key, rows) -> result.put(key, List.copyOf(rows)));
                    return result;
                }).timeout(deadline.maxWait());
    }

    @Override
    public Mono<List<BucketAggregate>> bucketedCount(long tenantId, TimeWindow window, Duration bucketWidth,
                                                       TsdbDeadline deadline) {
        if (tenantId <= 0) return Mono.error(new IllegalArgumentException("tenantId must be positive"));
        if (bucketWidth == null || bucketWidth.isZero() || bucketWidth.isNegative()) {
            return Mono.error(new IllegalArgumentException("bucketWidth must be positive"));
        }
        String sql = "SELECT time_bucket(CAST(:bucket_width AS interval),create_time) AS bucket,COUNT(*) AS value FROM " + table()
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time AND create_time<:to_time"
                + " GROUP BY bucket ORDER BY bucket ASC";
        return databaseClient.sql(sql).bind("tenant_id", tenantId)
                .bind("from_time", dialect.bindInstant(window.from()))
                .bind("to_time", dialect.bindInstant(window.toExclusive()))
                .bind("bucket_width", bucketWidth.toMillis() + " milliseconds")
                .map((row, metadata) -> new BucketAggregate(instant(row.get("bucket")),
                        (double) number(row.get("value")), number(row.get("value")))).all()
                .collectList().timeout(deadline.maxWait());
    }

    @Override
    public Mono<List<DimensionCount>> countByDimension(long tenantId, TimeWindow window, GroupDimension dimension,
                                                         int limit, TsdbDeadline deadline) {
        if (tenantId <= 0) return Mono.error(new IllegalArgumentException("tenantId must be positive"));
        if (limit < 1 || limit > MAX_DIMENSION_LIMIT) {
            return Mono.error(new IllegalArgumentException("limit must be between 1 and " + MAX_DIMENSION_LIMIT));
        }
        String column = switch (dimension) {
            case DEVICE -> "device_id";
            case POINT -> "point_id";
            case DRIVER -> "driver_id";
        };
        String nullGuard = dimension == GroupDimension.DRIVER ? " AND driver_id IS NOT NULL" : "";
        String sql = "SELECT " + column + " AS entity_id,COUNT(*) AS value FROM " + table()
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time AND create_time<:to_time" + nullGuard
                + " GROUP BY " + column + " ORDER BY value DESC LIMIT :limit";
        return databaseClient.sql(sql).bind("tenant_id", tenantId)
                .bind("from_time", dialect.bindInstant(window.from())).bind("to_time", dialect.bindInstant(window.toExclusive()))
                .bind("limit", limit)
                .map((row, metadata) -> new DimensionCount(dimension, number(row.get("entity_id")), number(row.get("value"))))
                .all().collectList().timeout(deadline.maxWait());
    }

    @Override
    public Mono<List<SeriesLastSeen>> lastSeenPerSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        if (tenantId <= 0) return Mono.error(new IllegalArgumentException("tenantId must be positive"));
        String sql = "SELECT tenant_id,device_id,point_id,MAX(create_time) AS last_seen FROM " + table()
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time AND create_time<:to_time"
                + " GROUP BY tenant_id,device_id,point_id ORDER BY device_id,point_id";
        return databaseClient.sql(sql).bind("tenant_id", tenantId)
                .bind("from_time", dialect.bindInstant(window.from())).bind("to_time", dialect.bindInstant(window.toExclusive()))
                .map((row, metadata) -> new SeriesLastSeen(
                        new SeriesKey(number(row.get("tenant_id")), number(row.get("device_id")), number(row.get("point_id"))),
                        instant(row.get("last_seen")))).all().collectList().timeout(deadline.maxWait());
    }

    @Override
    public Mono<List<LatencyBin>> latencyHistogram(long tenantId, TimeWindow window, List<Long> binEdgesMs,
                                                     TsdbDeadline deadline) {
        if (tenantId <= 0) return Mono.error(new IllegalArgumentException("tenantId must be positive"));
        if (binEdgesMs == null || binEdgesMs.isEmpty() || binEdgesMs.stream().anyMatch(Objects::isNull)
                || !isStrictlyAscending(binEdgesMs)) {
            return Mono.error(new IllegalArgumentException("binEdgesMs must be strictly ascending and non-empty"));
        }
        StringBuilder sql = new StringBuilder("SELECT receive_latency_bin,COUNT(*) AS value FROM (SELECT CASE ");
        for (int i = 0; i < binEdgesMs.size(); i++) {
            sql.append("WHEN EXTRACT(EPOCH FROM (operate_time-create_time))*1000 < :edge").append(i)
                    .append(" THEN ").append(i).append(' ');
        }
        sql.append("ELSE ").append(binEdgesMs.size()).append(" END AS receive_latency_bin FROM ").append(table())
                .append(" WHERE tenant_id=:tenant_id AND create_time>=:from_time AND create_time<:to_time) grouped GROUP BY receive_latency_bin ORDER BY receive_latency_bin");
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString()).bind("tenant_id", tenantId)
                .bind("from_time", dialect.bindInstant(window.from())).bind("to_time", dialect.bindInstant(window.toExclusive()));
        for (int i = 0; i < binEdgesMs.size(); i++) spec = spec.bind("edge" + i, binEdgesMs.get(i));
        return spec.map((row, metadata) -> Map.entry(integer(row.get("receive_latency_bin")), number(row.get("value")))).all()
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(counts -> {
                    List<LatencyBin> result = new ArrayList<>();
                    long lower = Long.MIN_VALUE;
                    for (int i = 0; i <= binEdgesMs.size(); i++) {
                        long upper = i < binEdgesMs.size() ? binEdgesMs.get(i) : Long.MAX_VALUE;
                        result.add(new LatencyBin(lower, upper, counts.getOrDefault(i, 0L)));
                        lower = upper;
                    }
                    return result;
                }).timeout(deadline.maxWait());
    }

    @Override
    public Mono<List<SeriesKey>> listSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        if (tenantId <= 0) return Mono.error(new IllegalArgumentException("tenantId must be positive"));
        String sql = "SELECT DISTINCT tenant_id,device_id,point_id FROM " + table()
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time AND create_time<:to_time"
                + " ORDER BY device_id,point_id";
        return databaseClient.sql(sql).bind("tenant_id", tenantId)
                .bind("from_time", dialect.bindInstant(window.from())).bind("to_time", dialect.bindInstant(window.toExclusive()))
                .map((row, metadata) -> new SeriesKey(number(row.get("tenant_id")), number(row.get("device_id")), number(row.get("point_id"))))
                .all().collectList().timeout(deadline.maxWait());
    }

    @Override
    public Mono<Void> deleteRange(SeriesKey series, TimeWindow window) {
        if (series == null) return Mono.error(new IllegalArgumentException("series must not be null"));
        return databaseClient.sql("DELETE FROM " + table()
                        + " WHERE tenant_id=:tenant_id AND device_id=:device_id AND point_id=:point_id"
                        + " AND create_time>=:from_time AND create_time<:to_time")
                .bind("tenant_id", series.tenantId()).bind("device_id", series.deviceId()).bind("point_id", series.pointId())
                .bind("from_time", dialect.bindInstant(window.from())).bind("to_time", dialect.bindInstant(window.toExclusive()))
                .fetch().rowsUpdated().then().as(transactionalOperator::transactional);
    }

    @Override
    public Mono<CorrelationResult> correlation(SeriesKey a, SeriesKey b, TimeWindow window,
                                                Duration alignBucket, TsdbDeadline deadline) {
        if (a == null || b == null || a.tenantId() != b.tenantId()) {
            return Mono.error(new IllegalArgumentException("correlation series must share a tenant"));
        }
        if (alignBucket == null || alignBucket.isZero() || alignBucket.isNegative()) {
            return Mono.error(new IllegalArgumentException("alignBucket must be positive"));
        }
        String sql = "SELECT corr(a.value,b.value) AS pearson,COUNT(*) AS buckets FROM "
                + "(SELECT time_bucket(CAST(:bucket AS interval),create_time) AS bucket,AVG(num_value) AS value FROM " + table()
                + " WHERE tenant_id=:ta AND device_id=:da AND point_id=:pa AND create_time>=:from_time AND create_time<:to_time GROUP BY bucket) a JOIN "
                + "(SELECT time_bucket(CAST(:bucket AS interval),create_time) AS bucket,AVG(num_value) AS value FROM " + table()
                + " WHERE tenant_id=:tb AND device_id=:db AND point_id=:pb AND create_time>=:from_time AND create_time<:to_time GROUP BY bucket) b USING(bucket)";
        return databaseClient.sql(sql).bind("bucket", alignBucket.toMillis() + " milliseconds")
                .bind("ta", a.tenantId()).bind("da", a.deviceId()).bind("pa", a.pointId())
                .bind("tb", b.tenantId()).bind("db", b.deviceId()).bind("pb", b.pointId())
                .bind("from_time", dialect.bindInstant(window.from())).bind("to_time", dialect.bindInstant(window.toExclusive()))
                .map((row, metadata) -> new CorrelationResult(Objects.requireNonNullElse(decimal(row.get("pearson")), 0d), number(row.get("buckets"))))
                .one().defaultIfEmpty(new CorrelationResult(0d, 0L)).timeout(deadline.maxWait());
    }

    private static boolean isStrictlyAscending(List<Long> values) {
        for (int i = 1; i < values.size(); i++) if (values.get(i) <= values.get(i - 1)) return false;
        return true;
    }

    private static String aggregateExpression(AggregateFunction fn, Double percentile) {
        return switch (fn) {
            case AVG -> "AVG(num_value)";
            case MIN -> "MIN(num_value)";
            case MAX -> "MAX(num_value)";
            case SUM -> "SUM(num_value)";
            case COUNT -> "COUNT(*)";
            case FIRST -> "(array_agg(num_value ORDER BY create_time ASC,message_id ASC))[1]";
            case LAST -> "(array_agg(num_value ORDER BY create_time DESC,message_id DESC))[1]";
            case PERCENTILE -> "PERCENTILE_CONT(" + percentile + ") WITHIN GROUP (ORDER BY num_value)";
        };
    }

    private DatabaseClient.GenericExecuteSpec query(String sql, SeriesFilter filter, TimeWindow window) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql).bind("tenant_id", filter.tenantId());
        if (window != null) spec = spec.bind("from_time", dialect.bindInstant(window.from()))
                .bind("to_time", dialect.bindInstant(window.toExclusive()));
        if (filter.series() != null && !filter.series().isEmpty()) {
            for (int i = 0; i < filter.series().size(); i++) {
                SeriesKey key = filter.series().get(i);
                spec = spec.bind("series_t" + i, key.tenantId()).bind("series_d" + i, key.deviceId()).bind("series_p" + i, key.pointId());
            }
        }
        return spec;
    }

    private String predicates(SeriesFilter filter, TimeWindow window) {
        StringBuilder where = new StringBuilder("tenant_id=:tenant_id");
        if (window != null) where.append(" AND create_time>=:from_time AND create_time<:to_time");
        if (filter.series() != null && !filter.series().isEmpty()) {
            where.append(" AND (");
            for (int i = 0; i < filter.series().size(); i++) {
                if (i > 0) where.append(" OR ");
                where.append("(tenant_id=:series_t").append(i).append(" AND device_id=:series_d").append(i)
                        .append(" AND point_id=:series_p").append(i).append(')');
            }
            where.append(')');
        }
        return where.toString();
    }

    private PointValueSample map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata ignored) {
        return new PointValueSample(new SeriesKey(number(row.get("tenant_id")), number(row.get("device_id")), number(row.get("point_id"))),
                instant(row.get("create_time")), instant(row.get("operate_time")), row.get("raw_value", String.class),
                row.get("cal_value", String.class), decimal(row.get("num_value")), integer(row.get("quality")),
                row.get("message_id", String.class), integer(row.get("schema_version")), row.get("driver_node", String.class),
                number(row.get("sequence")), number(row.get("fencing_token")), number(row.get("driver_id")));
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime offset) return offset.toInstant();
        if (value instanceof LocalDateTime local) return local.toInstant(ZoneOffset.UTC);
        return null;
    }

    private static long number(Object value) { return value instanceof Number n ? n.longValue() : 0L; }
    private static int integer(Object value) { return value instanceof Number n ? n.intValue() : 0; }
    private static Double decimal(Object value) { return value instanceof Number n ? n.doubleValue() : null; }

    private static <T> DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec,
                                                                        String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }
}

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

package io.github.pnoker.common.tsdb.timescale;

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
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TimescaleDB adapter (embedded or standalone PostgreSQL) for the TsdbStore port —
 * semantics per docs/design/tsdb-abstraction.md §7:
 *
 * <ul>
 *   <li>duplicate (series, deviceTime) policy: update-in-place — a unique index on
 *       (tenant_id, device_id, point_id, create_time) backs
 *       {@code ON CONFLICT DO UPDATE} with last-write-wins
 *   <li>bucketed aggregates via {@code time_bucket}; FIRST/LAST via ordered
 *       {@code array_agg}; PERCENTILE via {@code percentile_cont}
 *   <li>S13 analytics: SQL GROUP BY expressions, latency histogram via CASE bins over
 *       {@code EXTRACT(EPOCH FROM (operate_time - create_time)) * 1000}
 *   <li>rollups: capability NONE in this extraction (Phase 1); continuous aggregates
 *       arrive with S16 in a later phase
 * </ul>
 *
 * <p>Timestamps travel as UTC {@code OffsetDateTime}; the port's epoch-micro Instants
 * round-trip exactly (PG stores microsecond TIMESTAMPTZ).
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Slf4j
public final class TimescaleTsdbStore implements TsdbStore {

    private static final String TABLE = "dc3_point_value";

    private static final int SERIES_IN_CHUNK = 500;

    /**
     * S16 rollup tiers. These are the observability pipeline's continuous
     * aggregates (07-iot-dc3-observability.sql, queried by Grafana) — one
     * structure serves both consumers; the adapter recreates them with an
     * identical shape when it boots standalone.
     */
    private static final String ROLLUP_1M = "cagg_point_value_1m";

    private static final String ROLLUP_1H = "cagg_point_value_1h";

    private static final java.time.Duration TIER_MINUTE = java.time.Duration.ofMinutes(1);

    private static final java.time.Duration TIER_HOUR = java.time.Duration.ofHours(1);

    private static final String COLUMNS = "tenant_id, device_id, point_id, message_id, schema_version, "
            + "driver_node, sequence, fencing_token, raw_value, cal_value, num_value, quality, "
            + "driver_id, create_time, operate_time";

    private final int minuteTierKeepDays;

    private final JdbcTemplate jdbc;

    private static final RowMapper<PointValueSample> SAMPLE_MAPPER = (rs, i) -> new PointValueSample(
            new SeriesKey(rs.getLong("tenant_id"), rs.getLong("device_id"), rs.getLong("point_id")),
            toInstant(rs, "create_time"), toInstant(rs, "operate_time"),
            rs.getString("raw_value"), rs.getString("cal_value"),
            Objects.nonNull(rs.getObject("num_value")) ? rs.getDouble("num_value") : null,
            rs.getInt("quality"),
            rs.getString("message_id"), rs.getInt("schema_version"),
            rs.getString("driver_node"), rs.getLong("sequence"),
            rs.getLong("fencing_token"), rs.getLong("driver_id"));

    public TimescaleTsdbStore(DataSource dataSource) {
        this(dataSource, 365);
    }

    public TimescaleTsdbStore(DataSource dataSource, int minuteTierKeepDays) {
        this.minuteTierKeepDays = minuteTierKeepDays;
        this.jdbc = new JdbcTemplate(dataSource);
        bootstrap();
    }

    private void bootstrap() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                    message_id     TEXT        NOT NULL,
                    schema_version INTEGER     NOT NULL,
                    driver_node    TEXT        NOT NULL,
                    sequence       BIGINT      NOT NULL,
                    fencing_token  BIGINT      NOT NULL,
                    device_id      BIGINT      NOT NULL,
                    point_id       BIGINT      NOT NULL,
                    raw_value      TEXT        NOT NULL,
                    cal_value      TEXT        NOT NULL,
                    num_value      DOUBLE PRECISION,
                    quality        INTEGER     NOT NULL DEFAULT 0,
                    driver_id      BIGINT      NOT NULL,
                    tenant_id      BIGINT      NOT NULL,
                    create_time    TIMESTAMPTZ NOT NULL,
                    operate_time   TIMESTAMPTZ NOT NULL
                )""".formatted(TABLE));
        // Deployments initialized before the port keep a table without the S17
        // quality column; add it in place instead of failing every append.
        jdbc.execute("ALTER TABLE %s ADD COLUMN IF NOT EXISTS quality INTEGER NOT NULL DEFAULT 0".formatted(TABLE));
        try {
            jdbc.execute("SELECT create_hypertable('%s', 'create_time', if_not_exists => TRUE)".formatted(TABLE));
        } catch (DataAccessException e) {
            // timescaledb extension not loaded (plain-PG deployments): the table still
            // works as a plain time-ordered table; log and continue
            log.warn("TimescaleDB hypertable not created, falling back to plain table: {}", e.getMessage());
        }
        // The pre-port unique index on (message_id, create_time, device_id) backed
        // INSERT-side replay dedup. The port's duplicate policy is upsert on
        // (series, deviceTime), and a re-sent event with a corrected timestamp
        // would violate the old index mid-update — retire it here so existing
        // deployments converge on the new access path.
        jdbc.execute("DROP INDEX IF EXISTS uk_point_value_event");
        jdbc.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_point_value_series_time
                ON %s (tenant_id, device_id, point_id, create_time)""".formatted(TABLE));
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS idx_point_value_ts_lookup
                ON %s (tenant_id, device_id, point_id, create_time DESC)""".formatted(TABLE));
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS idx_point_value_tenant_time
                ON %s (tenant_id, create_time DESC)""".formatted(TABLE));
        primeInitialChunk();
        bootstrapRollups();
        log.info("Timescale store ready (table {})", TABLE);
    }

    /**
     * S16 tiered lifecycle: raw → 1-minute rollup → 1-hour rollup. Both tiers are
     * real-time continuous aggregates ({@code materialized_only = FALSE}) so reads
     * are correct immediately after an append — the background refresh policies
     * only move the aggregation work off the read path. Aggregates are chosen for
     * exact composition across tiers: AVG recombines as SUM(sum)/SUM(numeric_count),
     * FIRST/LAST re-select by bucket time; PERCENTILE stays on the raw path.
     * The hour tier groups by the explicit time_bucket expression because its
     * output alias collides with the source column name.
     */
    private void bootstrapRollups() {
        try {
            jdbc.execute("""
                    CREATE MATERIALIZED VIEW IF NOT EXISTS %s
                    WITH (timescaledb.continuous, timescaledb.materialized_only = FALSE) AS
                    SELECT time_bucket(INTERVAL '1 minute', create_time) AS bucket,
                           tenant_id, driver_id, device_id, point_id,
                           COUNT(*) AS sample_count, COUNT(num_value) AS num_count,
                           AVG(num_value) AS num_avg, MIN(num_value) AS num_min,
                           MAX(num_value) AS num_max, SUM(num_value) AS num_sum,
                           FIRST(cal_value, create_time) AS cal_first,
                           LAST(cal_value, create_time) AS cal_last
                    FROM %s
                    GROUP BY time_bucket(INTERVAL '1 minute', create_time),
                             tenant_id, driver_id, device_id, point_id
                    WITH NO DATA""".formatted(ROLLUP_1M, TABLE));
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_cagg_pv_1m_lookup ON %s (tenant_id, device_id, point_id, bucket DESC)".formatted(ROLLUP_1M));
            jdbc.execute("""
                    CREATE MATERIALIZED VIEW IF NOT EXISTS %s
                    WITH (timescaledb.continuous, timescaledb.materialized_only = FALSE) AS
                    SELECT time_bucket(INTERVAL '1 hour', bucket) AS bucket,
                           tenant_id, driver_id, device_id, point_id,
                           SUM(sample_count) AS sample_count, SUM(num_count) AS num_count,
                           SUM(num_sum) / NULLIF(SUM(num_count), 0) AS num_avg,
                           MIN(num_min) AS num_min, MAX(num_max) AS num_max,
                           SUM(num_sum) AS num_sum,
                           FIRST(cal_first, bucket) AS cal_first,
                           LAST(cal_last, bucket) AS cal_last
                    FROM %s
                    GROUP BY time_bucket(INTERVAL '1 hour', bucket),
                             tenant_id, driver_id, device_id, point_id
                    WITH NO DATA""".formatted(ROLLUP_1H, ROLLUP_1M));
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_cagg_pv_1h_lookup ON %s (tenant_id, device_id, point_id, bucket DESC)".formatted(ROLLUP_1H));
            jdbc.execute("""
                    SELECT add_continuous_aggregate_policy('%s', if_not_exists => TRUE,
                            start_offset => NULL, end_offset => INTERVAL '1 minute',
                            schedule_interval => INTERVAL '1 minute')""".formatted(ROLLUP_1M));
            jdbc.execute("""
                    SELECT add_continuous_aggregate_policy('%s', if_not_exists => TRUE,
                            start_offset => NULL, end_offset => INTERVAL '5 minutes',
                            schedule_interval => INTERVAL '5 minutes')""".formatted(ROLLUP_1H));
            jdbc.execute("""
                    SELECT add_retention_policy('%s', INTERVAL '%d days', if_not_exists => TRUE)"""
                    .formatted(ROLLUP_1M, minuteTierKeepDays));
            // The hour tier is kept forever — no retention policy.
        } catch (DataAccessException e) {
            // Plain-PG deployments (no timescaledb extension) skip the tiers; reads
            // stay correct on the raw path.
            log.warn("Rollup tiers not created, reads stay on the raw path: {}", e.getMessage());
        }
    }

    /**
     * TimescaleDB sizes the initial chunk around the first inserted row; multi-row or
     * microsecond-boundary first appends can leave that row outside the chunk's final
     * range (invisible to index scans, present in heap). Inserting a sentinel at a
     * fixed early instant forces initial chunk creation here, at bootstrap, with a
     * controlled boundary — every later append lands on the normal chunk path.
     */
    private void primeInitialChunk() {
        try {
            jdbc.update("""
                    INSERT INTO %s (message_id, schema_version, driver_node, sequence, fencing_token,
                                    tenant_id, device_id, point_id, raw_value, cal_value, quality, driver_id,
                                    create_time, operate_time)
                    VALUES ('dc3-chunk-prime', 1, 'bootstrap', 0, 0, 0, 0, 0, '', '', 0, 0,
                            '2000-01-01T00:00:00Z', '2000-01-01T00:00:00Z')
                            ON CONFLICT DO NOTHING""".formatted(TABLE));
            jdbc.update("DELETE FROM " + TABLE + " WHERE message_id = 'dc3-chunk-prime'");
        } catch (DataAccessException e) {
            log.warn("Initial chunk priming skipped: {}", e.getMessage());
        }
    }

    @Override
    public String type() {
        return "timescale";
    }

    @Override
    public TsdbCapabilities capabilities() {
        return new TsdbCapabilities(
                true, true, true, true, true,
                RollupSupport.NATIVE, 5000,
                true, OrderingGuarantee.PER_SERIES, Precision.MICRO, true, true);
    }

    // ===== 写入 =====

    @Override
    public int append(List<PointValueSample> samples) {
        if (samples.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (List<PointValueSample> chunk : chunk(samples, capabilities().maxAppendBatch())) {
            // TimescaleDB creates the initial chunk keyed off the FIRST row of a
            // multi-row insert; when the earliest timestamp leads, that row can land
            // outside the chunk's final range and become invisible to index scans.
            // Sorting newest-first lets earlier timestamps backfill into the chunk the
            // newest row just created — empirically verified both directions.
            List<PointValueSample> ordered = new ArrayList<>(chunk);
            ordered.sort(java.util.Comparator
                    .comparing(PointValueSample::deviceTime, java.util.Comparator.reverseOrder())
                    .thenComparing(PointValueSample::messageId, java.util.Comparator.reverseOrder()));
            total += appendChunk(ordered);
        }
        return total;
    }

    /**
     * Single-statement multi-row insert via unnest arrays — JDBC batching with
     * ON CONFLICT DO UPDATE proved unreliable on the timescale-ha image (the first
     * batch entry could become invisible to index scans); one statement with array
     * parameters is both correct and one round trip.
     */
    private int appendChunk(List<PointValueSample> chunk) {
        return jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Integer>) connection -> {
            String sql = """
                    INSERT INTO %s (%s)
                    SELECT * FROM unnest(
                        ?::bigint[], ?::bigint[], ?::bigint[], ?::text[], ?::int[],
                        ?::text[], ?::bigint[], ?::bigint[], ?::text[], ?::text[],
                        ?::float8[], ?::int[], ?::bigint[], ?::timestamptz[], ?::timestamptz[])
                    ON CONFLICT (tenant_id, device_id, point_id, create_time) DO UPDATE SET
                        message_id = EXCLUDED.message_id,
                        schema_version = EXCLUDED.schema_version,
                        driver_node = EXCLUDED.driver_node,
                        sequence = EXCLUDED.sequence,
                        fencing_token = EXCLUDED.fencing_token,
                        raw_value = EXCLUDED.raw_value,
                        cal_value = EXCLUDED.cal_value,
                        num_value = EXCLUDED.num_value,
                        quality = EXCLUDED.quality,
                        driver_id = EXCLUDED.driver_id,
                        operate_time = EXCLUDED.operate_time""".formatted(TABLE, COLUMNS);
            java.sql.Array tenant = connection.createArrayOf("bigint", longs(chunk, s -> s.series().tenantId()));
            java.sql.Array device = connection.createArrayOf("bigint", longs(chunk, s -> s.series().deviceId()));
            java.sql.Array point = connection.createArrayOf("bigint", longs(chunk, s -> s.series().pointId()));
            java.sql.Array message = connection.createArrayOf("text", chunk.stream()
                    .map(PointValueSample::messageId).toArray(String[]::new));
            java.sql.Array schema = connection.createArrayOf("int", chunk.stream()
                    .map(PointValueSample::schemaVersion).map(Integer::valueOf).toArray(Integer[]::new));
            java.sql.Array node = connection.createArrayOf("text", chunk.stream()
                    .map(PointValueSample::driverNode).toArray(String[]::new));
            java.sql.Array sequence = connection.createArrayOf("bigint", longs(chunk, PointValueSample::sequence));
            java.sql.Array fencing = connection.createArrayOf("bigint", longs(chunk, PointValueSample::fencingToken));
            java.sql.Array raw = connection.createArrayOf("text", chunk.stream()
                    .map(PointValueSample::rawValue).toArray(String[]::new));
            java.sql.Array cal = connection.createArrayOf("text", chunk.stream()
                    .map(PointValueSample::calValue).toArray(String[]::new));
            java.sql.Array num = connection.createArrayOf("float8", chunk.stream()
                    .map(PointValueSample::numericValue).toArray(Double[]::new));
            java.sql.Array quality = connection.createArrayOf("int", chunk.stream()
                    .map(PointValueSample::quality).map(Integer::valueOf).toArray(Integer[]::new));
            java.sql.Array driver = connection.createArrayOf("bigint", longs(chunk, PointValueSample::driverId));
            java.sql.Array create = connection.createArrayOf("timestamptz", chunk.stream()
                    .map(s -> java.sql.Timestamp.from(s.deviceTime())).toArray(java.sql.Timestamp[]::new));
            java.sql.Array operate = connection.createArrayOf("timestamptz", chunk.stream()
                    .map(s -> java.sql.Timestamp.from(s.receiveTime())).toArray(java.sql.Timestamp[]::new));
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                java.sql.Array[] arrays = {tenant, device, point, message, schema, node, sequence,
                        fencing, raw, cal, num, quality, driver, create, operate};
                for (int i = 0; i < arrays.length; i++) {
                    ps.setArray(i + 1, arrays[i]);
                }
                return ps.executeUpdate();
            }
        });
    }

    private static Long[] longs(List<PointValueSample> chunk,
                                java.util.function.ToLongFunction<PointValueSample> extractor) {
        return chunk.stream().mapToLong(extractor).boxed().toArray(Long[]::new);
    }

    // ===== 读取 =====

    @Override
    public Map<SeriesKey, List<PointValueSample>> last(SeriesFilter filter, int limit, TsdbDeadline deadline) {
        requireSeriesOrScan(filter);
        String sql = """
                SELECT * FROM (
                    SELECT %s, ROW_NUMBER() OVER (
                        PARTITION BY v.tenant_id, v.device_id, v.point_id
                        ORDER BY v.create_time DESC, v.message_id DESC) AS rn
                    FROM %s v WHERE %s
                ) ranked WHERE rn <= ?
                """.formatted(qualified(COLUMNS), TABLE, seriesWhere(filter));
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        args.add(limit);
        Map<SeriesKey, List<PointValueSample>> result = new LinkedHashMap<>();
        for (PointValueSample sample : timed(deadline, () -> jdbc.query(sql, SAMPLE_MAPPER, args.toArray()))) {
            result.computeIfAbsent(sample.series(), k -> new ArrayList<>()).add(sample);
        }
        return result;
    }

    @Override
    public CursorPage<PointValueSample> history(SeriesFilter filter, TimeWindow window,
                                                Cursor cursor, int pageSize, TsdbDeadline deadline) {
        requireSeriesOrScan(filter);
        StringBuilder sql = new StringBuilder(
                "SELECT %s FROM %s v WHERE %s AND v.create_time >= ? AND v.create_time < ?"
                        .formatted(COLUMNS, TABLE, seriesWhere(filter)));
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        args.add(OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC));
        args.add(OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC));
        if (Objects.nonNull(cursor)) {
            sql.append(" AND (v.create_time, v.message_id) < (?, ?)");
            args.add(OffsetDateTime.ofInstant(cursor.deviceTime(), ZoneOffset.UTC));
            args.add(cursor.messageId());
        }
        sql.append(" ORDER BY v.create_time DESC, v.message_id DESC LIMIT ?");
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
        String expr = aggregateExpression(fn, percentile);
        String sql = """
                SELECT tenant_id, device_id, point_id, %s AS value, COUNT(*) AS sample_count
                FROM %s v WHERE %s AND create_time >= ? AND create_time < ?
                GROUP BY tenant_id, device_id, point_id"""
                .formatted(expr, TABLE, seriesWhere(filter));
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        args.add(OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC));
        args.add(OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC));
        Map<SeriesKey, WindowAggregate> result = new LinkedHashMap<>();
        timedVoid(deadline, () -> {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            for (Map<String, Object> row : rows) {
                result.put(new SeriesKey(((Number) row.get("tenant_id")).longValue(),
                                ((Number) row.get("device_id")).longValue(),
                                ((Number) row.get("point_id")).longValue()),
                        new WindowAggregate((Double) row.get("value"),
                                ((Number) row.get("sample_count")).longValue()));
            }
        });
        return result;
    }

    @Override
    public Map<SeriesKey, List<BucketAggregate>> bucketedAggregate(SeriesFilter filter, AggregateFunction fn,
                                                                   TimeWindow window, Duration bucketWidth,
                                                                   Double percentile, TsdbDeadline deadline) {
        String tier = rollupTierFor(fn, bucketWidth);
        if (Objects.nonNull(tier)) {
            return bucketedAggregateFromTier(tier, filter, fn, window, bucketWidth, deadline);
        }
        String expr = aggregateExpression(fn, percentile);
        String sql = """
                SELECT tenant_id, device_id, point_id, time_bucket(?::interval, create_time) AS bucket,
                       %s AS value, COUNT(*) AS sample_count
                FROM %s v WHERE %s AND create_time >= ? AND create_time < ?
                GROUP BY tenant_id, device_id, point_id, bucket ORDER BY bucket ASC"""
                .formatted(expr, TABLE, seriesWhere(filter));
        List<Object> args = new ArrayList<>();
        args.add(bucketWidth.toMillis() + " milliseconds");
        args.addAll(seriesArgs(filter));
        args.add(OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC));
        args.add(OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC));
        Map<SeriesKey, List<BucketAggregate>> result = new LinkedHashMap<>();
        timedVoid(deadline, () -> {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            for (Map<String, Object> row : rows) {
                result.computeIfAbsent(new SeriesKey(((Number) row.get("tenant_id")).longValue(),
                                ((Number) row.get("device_id")).longValue(),
                                ((Number) row.get("point_id")).longValue()), k -> new ArrayList<>())
                        .add(new BucketAggregate(((java.sql.Timestamp) row.get("bucket")).toInstant()
                                .truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                                (Double) row.get("value"),
                                ((Number) row.get("sample_count")).longValue()));
            }
        });
        return result;
    }

    @Override
    public long count(SeriesFilter filter, TimeWindow window, TsdbDeadline deadline) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " v WHERE " + seriesWhere(filter)
                + " AND create_time >= ? AND create_time < ?";
        List<Object> args = new ArrayList<>(seriesArgs(filter));
        args.add(OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC));
        args.add(OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC));
        Long value = timed(deadline, () -> jdbc.queryForObject(sql, Long.class, args.toArray()));
        return Objects.requireNonNullElse(value, 0L);
    }

    // ===== S13：租户级分析面 =====

    @Override
    public List<BucketAggregate> bucketedCount(long tenantId, TimeWindow window,
                                               Duration bucketWidth, TsdbDeadline deadline) {
        String tier = rollupTierFor(AggregateFunction.COUNT, bucketWidth);
        if (Objects.nonNull(tier)) {
            // Tier reads cover whole tier granules inside the window: buckets are
            // precomputed counts summed per coarse bucket.
            String sql = """
                    SELECT time_bucket(?::interval, bucket) AS bucket, SUM(sample_count) AS sample_count
                    FROM %s WHERE tenant_id = ? AND bucket >= ? AND bucket < ?
                    GROUP BY 1 ORDER BY 1 ASC""".formatted(tier);
            Object[] args = {bucketWidth.toMillis() + " milliseconds", tenantId,
                    OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                    OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC)};
            return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new BucketAggregate(
                    toInstant(rs, 1).truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                    null, rs.getLong(2)), args));
        }
        String sql = """
                SELECT time_bucket(?::interval, create_time) AS bucket, COUNT(*) AS sample_count
                FROM %s WHERE tenant_id = ? AND create_time >= ? AND create_time < ?
                GROUP BY bucket ORDER BY bucket ASC""".formatted(TABLE);
        Object[] args = {bucketWidth.toMillis() + " milliseconds", tenantId,
                OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC)};
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new BucketAggregate(
                toInstant(rs, 1).truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                null, rs.getLong(2)), args));
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
                SELECT %s AS entity_id, COUNT(*) AS sample_count
                FROM %s WHERE tenant_id = ? AND create_time >= ? AND create_time < ?
                GROUP BY entity_id ORDER BY sample_count DESC LIMIT ?""".formatted(column, TABLE);
        Object[] args = {tenantId,
                OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC),
                limit};
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new DimensionCount(dimension,
                        rs.getLong(1), rs.getLong(2)), args));
    }

    @Override
    public List<SeriesCount> seriesCounts(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = """
                SELECT tenant_id, device_id, point_id, COUNT(*) AS sample_count
                FROM %s WHERE tenant_id = ? AND create_time >= ? AND create_time < ?
                GROUP BY tenant_id, device_id, point_id""".formatted(TABLE);
        Object[] args = {tenantId,
                OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC)};
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new SeriesCount(
                new SeriesKey(rs.getLong(1), rs.getLong(2), rs.getLong(3)), rs.getLong(4)), args));
    }

    @Override
    public List<SeriesLastSeen> lastSeenPerSeries(long tenantId, TimeWindow window, TsdbDeadline deadline) {
        String sql = """
                SELECT tenant_id, device_id, point_id, MAX(create_time) AS last_seen
                FROM %s WHERE tenant_id = ? AND create_time >= ? AND create_time < ?
                GROUP BY tenant_id, device_id, point_id ORDER BY last_seen DESC""".formatted(TABLE);
        Object[] args = {tenantId,
                OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC)};
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new SeriesLastSeen(
                        new SeriesKey(rs.getLong(1), rs.getLong(2), rs.getLong(3)), toInstant(rs, 4)), args));
    }

    @Override
    public List<LatencyBin> latencyHistogram(long tenantId, TimeWindow window,
                                             List<Long> binEdgesMs, TsdbDeadline deadline) {
        StringBuilder bins = new StringBuilder();
        for (int i = 0; i < binEdgesMs.size() + 1; i++) {
            if (i < binEdgesMs.size()) {
                bins.append("WHEN diff < ? THEN ").append(i).append(' ');
            } else {
                bins.append("ELSE ").append(i);
            }
        }
        String sql = """
                SELECT bin, COUNT(*) FROM (
                    SELECT CASE %s END AS bin
                    FROM (SELECT EXTRACT(EPOCH FROM (operate_time - create_time)) * 1000 AS diff
                          FROM %s
                          WHERE tenant_id = ? AND create_time >= ? AND create_time < ?) deltas
                ) bucketed GROUP BY bin ORDER BY bin"""
                .formatted(bins, TABLE);
        List<Object> args = new ArrayList<>();
        binEdgesMs.forEach(args::add);
        args.add(tenantId);
        args.add(OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC));
        args.add(OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC));
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(sql, args.toArray())) {
            counts.put(((Number) row.get("bin")).intValue(), ((Number) row.get("count")).longValue());
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
                SELECT DISTINCT tenant_id, device_id, point_id FROM %s
                WHERE tenant_id = ? AND create_time >= ? AND create_time < ?""".formatted(TABLE);
        return timed(deadline, () -> jdbc.query(sql, (rs, i) -> new SeriesKey(
                        rs.getLong(1), rs.getLong(2), rs.getLong(3)),
                tenantId,
                OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC)));
    }

    @Override
    public void deleteRange(SeriesKey series, TimeWindow window) {
        jdbc.update("DELETE FROM " + TABLE + " WHERE tenant_id = ? AND device_id = ? AND point_id = ? "
                        + "AND create_time >= ? AND create_time < ?",
                series.tenantId(), series.deviceId(), series.pointId(),
                OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC));
        refreshTiers(window);
    }

    private void refreshTiers(TimeWindow window) {
        // The real-time tier reads reflect the delete immediately; re-materialize
        // so the stored tier rows converge too (tenant offboarding is rare).
        try {
            for (String tier : new String[]{ROLLUP_1M, ROLLUP_1H}) {
                jdbc.query("CALL refresh_continuous_aggregate(?, ?, ?)",
                        ps -> {
                            ps.setString(1, tier);
                            ps.setObject(2, OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC));
                            ps.setObject(3, OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC));
                        }, rs -> null);
            }
        } catch (DataAccessException e) {
            log.warn("Rollup refresh after deleteRange failed; background policies will converge: {}",
                    e.getMessage());
        }
    }

    @Override
    public CorrelationResult correlation(SeriesKey a, SeriesKey b, TimeWindow window,
                                         Duration alignBucket, TsdbDeadline deadline) {
        String sql = """
                SELECT corr(a.v, b.v), count(*)
                FROM (SELECT time_bucket(?::interval, create_time) AS tb, AVG(num_value) AS v FROM %s
                      WHERE tenant_id=? AND device_id=? AND point_id=? AND create_time>=? AND create_time<?
                      GROUP BY tb) a
                JOIN (SELECT time_bucket(?::interval, create_time) AS tb, AVG(num_value) AS v FROM %s
                      WHERE tenant_id=? AND device_id=? AND point_id=? AND create_time>=? AND create_time<?
                      GROUP BY tb) b ON a.tb = b.tb""".formatted(TABLE, TABLE);
        Object[] args = {
                alignBucket.toMillis() + " milliseconds",
                a.tenantId(), a.deviceId(), a.pointId(),
                OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC),
                alignBucket.toMillis() + " milliseconds",
                b.tenantId(), b.deviceId(), b.pointId(),
                OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC)};
        return timed(deadline, () -> jdbc.queryForObject(sql, (rs, i) -> new CorrelationResult(
                rs.getDouble(1), rs.getLong(2)), args));
    }

    // ===== helpers =====

    /**
     * S16 read transparency: bucket widths at or above a materialized tier's
     * granularity are served from that tier (PERCENTILE never — it is not
     * materialized). Tier reads cover whole tier granules inside the window.
     */
    private String rollupTierFor(AggregateFunction fn, Duration bucketWidth) {
        // FIRST/LAST stay raw: the shared observability caggs carry first/last of
        // the textual cal_value only, and serving numeric first/last from tiers
        // would version-skew deployments created before that shape existed.
        if (fn == AggregateFunction.PERCENTILE || fn == AggregateFunction.FIRST
                || fn == AggregateFunction.LAST) {
            return null;
        }
        if (!bucketWidth.minus(TIER_HOUR).isNegative()) {
            return ROLLUP_1H;
        }
        if (!bucketWidth.minus(TIER_MINUTE).isNegative()) {
            return ROLLUP_1M;
        }
        return null;
    }

    private Map<SeriesKey, List<BucketAggregate>> bucketedAggregateFromTier(String tier, SeriesFilter filter,
                                                                            AggregateFunction fn, TimeWindow window,
                                                                            Duration bucketWidth, TsdbDeadline deadline) {
        String sql = """
                SELECT time_bucket(?::interval, bucket) AS bucket, tenant_id, device_id, point_id,
                       %s AS value, SUM(sample_count) AS sample_count
                FROM %s v WHERE %s AND v.bucket >= ? AND v.bucket < ?
                GROUP BY 1, 2, 3, 4
                ORDER BY 1 ASC"""
                .formatted(tierExpression(fn), tier, seriesWhere(filter));
        List<Object> args = new ArrayList<>();
        args.add(bucketWidth.toMillis() + " milliseconds");
        args.addAll(seriesArgs(filter));
        args.add(OffsetDateTime.ofInstant(window.from(), ZoneOffset.UTC));
        args.add(OffsetDateTime.ofInstant(window.toExclusive(), ZoneOffset.UTC));
        Map<SeriesKey, List<BucketAggregate>> result = new LinkedHashMap<>();
        timedVoid(deadline, () -> {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
            for (Map<String, Object> row : rows) {
                result.computeIfAbsent(new SeriesKey(((Number) row.get("tenant_id")).longValue(),
                                ((Number) row.get("device_id")).longValue(),
                                ((Number) row.get("point_id")).longValue()), k -> new ArrayList<>())
                        .add(new BucketAggregate(((java.sql.Timestamp) row.get("bucket")).toInstant()
                                .truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                                (Double) row.get("value"),
                                ((Number) row.get("sample_count")).longValue()));
            }
        });
        return result;
    }

    /** Exactly composable re-aggregation over the shared observability tiers. */
    private static String tierExpression(AggregateFunction fn) {
        return switch (fn) {
            case AVG -> "SUM(num_sum) / NULLIF(SUM(num_count), 0)";
            case MIN -> "MIN(num_min)";
            case MAX -> "MAX(num_max)";
            case SUM -> "SUM(num_sum)";
            case COUNT -> "CAST(SUM(sample_count) AS DOUBLE PRECISION)";
            case FIRST, LAST, PERCENTILE -> throw new IllegalArgumentException("never tiered: " + fn);
        };
    }

    private static Instant toInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return Objects.isNull(ts) ? null : ts.toInstant();
    }

    private static Instant toInstant(ResultSet rs, int column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return Objects.isNull(ts) ? null : ts.toInstant();
    }

    private void requireSeriesOrScan(SeriesFilter filter) {
        if (filter.tenantWide() && !capabilities().tenantWideScan()) {
            throw new IllegalArgumentException("tenant-wide scan not supported by this store");
        }
    }

    private String seriesWhere(SeriesFilter filter) {
        if (filter.tenantWide()) {
            return "v.tenant_id = ?";
        }
        // Row-value IN lists stay parseable for the large series sets the paged
        // history view resolves from relational metadata; 500 pairs per list.
        StringBuilder out = new StringBuilder("v.tenant_id = ? AND (");
        int emitted = 0;
        for (int start = 0; start < filter.series().size(); start += SERIES_IN_CHUNK) {
            if (emitted > 0) {
                out.append(" OR ");
            }
            out.append("(v.device_id, v.point_id) IN (");
            for (int i = start; i < Math.min(start + SERIES_IN_CHUNK, filter.series().size()); i++) {
                out.append(i > start ? ", (?, ?)" : "(?, ?)");
            }
            out.append(')');
            emitted = start + SERIES_IN_CHUNK;
        }
        return out.append(')').toString();
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
            case COUNT -> "CAST(COUNT(*) AS DOUBLE PRECISION)";
            case FIRST -> "(array_agg(num_value ORDER BY create_time, message_id))[1]";
            case LAST -> "(array_agg(num_value ORDER BY create_time DESC, message_id DESC))[1]";
            case PERCENTILE -> "percentile_cont(" + Objects.requireNonNull(percentile,
                    "percentile required for PERCENTILE") + ") WITHIN GROUP (ORDER BY num_value)";
        };
    }

    private String qualified(String columns) {
        StringBuilder out = new StringBuilder();
        for (String column : columns.split(", ")) {
            if (!out.isEmpty()) {
                out.append(", ");
            }
            out.append("v.").append(column);
        }
        return out.toString();
    }

    private static <T> List<List<T>> chunk(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }

    private void timedVoid(TsdbDeadline deadline, Runnable query) {
        timed(deadline, () -> {
            query.run();
            return null;
        });
    }

    private <T> T timed(TsdbDeadline deadline, java.util.function.Supplier<T> query) {
        int seconds = (int) Math.max(1, deadline.maxWait().toSeconds());
        Integer previous = jdbc.getQueryTimeout();
        jdbc.setQueryTimeout(seconds);
        try {
            return query.get();
        } catch (DataAccessException e) {
            if (Objects.nonNull(e.getCause()) && String.valueOf(e.getCause().getClass().getName())
                    .contains("QueryTimeout")) {
                throw new TsdbQueryTimeout("timescale query exceeded " + deadline.maxWait(), e);
            }
            throw e;
        } finally {
            jdbc.setQueryTimeout(previous);
        }
    }
}

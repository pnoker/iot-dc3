package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.bo.dashboard.AlertCountersRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AlertTrendRow;
import io.github.pnoker.common.data.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.data.entity.bo.dashboard.HourCountRow;
import io.github.pnoker.common.data.entity.bo.dashboard.SourceStatsRow;
import io.github.pnoker.common.data.entity.bo.dashboard.ActivityCellRow;
import io.github.pnoker.common.data.entity.bo.dashboard.FlappingRow;
import io.github.pnoker.common.data.entity.bo.dashboard.SourceCountRow;
import io.github.pnoker.common.data.entity.bo.dashboard.CorrelationPairRow;
import io.github.pnoker.common.data.entity.bo.dashboard.PeerAlarmRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AgingBucketRow;
import io.github.pnoker.common.data.entity.bo.dashboard.MttaTrendRow;
import io.github.pnoker.common.data.entity.bo.dashboard.ProtocolHealthRow;
import io.github.pnoker.common.data.entity.bo.dashboard.RecentChangeRow;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/** Explicit SQL adapter for dashboard alert aggregates. */
@Repository
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class})
@RequiredArgsConstructor
public class R2dbcAlertAnalyticsStore implements ReactiveAlertAnalyticsStore {

    private static final String TABLE = "dc3_data.dc3_entity_alarm";

    private final DatabaseClient databaseClient;
    private final R2dbcDialect dialect;

    @Override
    public Mono<AlertCountersRow> countAll(long tenantId) {
        return databaseClient.sql("SELECT COUNT(*) AS total, "
                        + "COALESCE(SUM(CASE WHEN confirm_flag=0 THEN 1 ELSE 0 END),0) AS unconfirmed "
                        + "FROM " + TABLE + " WHERE tenant_id=:tenant_id")
                .bind("tenant_id", tenantId)
                .map((row, metadata) -> {
                    AlertCountersRow value = new AlertCountersRow();
                    value.setTotal(number(row.get("total")));
                    value.setUnconfirmed(number(row.get("unconfirmed")));
                    return value;
                })
                .one();
    }

    @Override
    public Flux<BucketRow> countByType(long tenantId) {
        String typeExpression = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "CAST(alarm_type_flag AS CHAR)";
            default -> "CAST(alarm_type_flag AS TEXT)";
        };
        return databaseClient.sql("SELECT " + typeExpression + " AS bucket_key, COUNT(*) AS count FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id GROUP BY alarm_type_flag ORDER BY count DESC")
                .bind("tenant_id", tenantId)
                .map((row, metadata) -> {
                    BucketRow value = new BucketRow();
                    value.setBucketKey(row.get("bucket_key"));
                    value.setCount(number(row.get("count")));
                    return value;
                })
                .all();
    }

    @Override
    public Flux<SourceStatsRow> countBySource(long tenantId) {
        return sourceStats("SELECT " + sourceExpression() + " AS source, COUNT(*) AS total, "
                + "COALESCE(SUM(CASE WHEN confirm_flag=0 THEN 1 ELSE 0 END),0) AS unconfirmed FROM " + TABLE
                + " WHERE tenant_id=:tenant_id GROUP BY alarm_target_type_flag ORDER BY alarm_target_type_flag", tenantId);
    }

    @Override
    public Flux<HourCountRow> hourlyCounts(long tenantId, LocalDateTime from) {
        String bucket = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "DATE_FORMAT(create_time, '%Y-%m-%d %H:00:00')";
            default -> "date_trunc('hour', create_time)";
        };
        return databaseClient.sql("SELECT " + bucket + " AS bucket, COUNT(*) AS count FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND create_time>=:from_time GROUP BY " + bucket
                        + " ORDER BY bucket ASC")
                .bind("tenant_id", tenantId)
                .bind("from_time", from)
                .map((row, metadata) -> {
                    HourCountRow value = new HourCountRow();
                    value.setBucket(time(row.get("bucket")));
                    value.setCount(number(row.get("count")));
                    return value;
                })
                .all();
    }

    @Override
    public Flux<SourceStatsRow> todayBySource(long tenantId, LocalDateTime from) {
        return sourceStats("SELECT " + sourceExpression() + " AS source, COUNT(*) AS total, "
                + "COALESCE(SUM(CASE WHEN confirm_flag=0 THEN 1 ELSE 0 END),0) AS unconfirmed FROM " + TABLE
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time GROUP BY alarm_target_type_flag"
                + " ORDER BY alarm_target_type_flag", tenantId, from);
    }

    @Override
    public Flux<AlertTrendRow> dailyTrend(long tenantId, LocalDateTime from) {
        String day = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "DATE(create_time)";
            default -> "CAST(create_time AS DATE)";
        };
        return databaseClient.sql("SELECT " + day + " AS day, "
                        + "COALESCE(SUM(CASE WHEN alarm_target_type_flag IN (0,1) THEN 1 ELSE 0 END),0) AS device_count, "
                        + "COALESCE(SUM(CASE WHEN alarm_target_type_flag=2 THEN 1 ELSE 0 END),0) AS driver_count "
                        + "FROM " + TABLE + " WHERE tenant_id=:tenant_id AND create_time>=:from_time "
                        + "GROUP BY " + day + " ORDER BY day ASC")
                .bind("tenant_id", tenantId)
                .bind("from_time", from)
                .map((row, metadata) -> {
                    AlertTrendRow value = new AlertTrendRow();
                    value.setDate(date(row.get("day")));
                    value.setDeviceCount(number(row.get("device_count")));
                    value.setDriverCount(number(row.get("driver_count")));
                    return value;
                })
                .all();
    }

    @Override
    public Flux<SourceCountRow> topSources(long tenantId, LocalDateTime from, int limit) {
        return sourceCount("SELECT " + sourceExpression() + " AS source, entity_id AS source_id, COUNT(*) AS count FROM " + TABLE
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time GROUP BY alarm_target_type_flag, entity_id"
                + " ORDER BY count DESC LIMIT :limit", tenantId, from, limit);
    }

    @Override
    public Flux<ActivityCellRow> activityHeatmap(long tenantId, LocalDateTime from) {
        String dow = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "DAYOFWEEK(create_time)-1";
            default -> "EXTRACT(DOW FROM create_time)::int";
        };
        String hour = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "HOUR(create_time)";
            default -> "EXTRACT(HOUR FROM create_time)::int";
        };
        return databaseClient.sql("SELECT " + dow + " AS dow, " + hour + " AS hour, COUNT(*) AS count FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND create_time>=:from_time GROUP BY " + dow + ", " + hour)
                .bind("tenant_id", tenantId).bind("from_time", from)
                .map((row, metadata) -> {
                    ActivityCellRow value = new ActivityCellRow();
                    value.setDow((int) number(row.get("dow")));
                    value.setHour((int) number(row.get("hour")));
                    value.setCount(number(row.get("count")));
                    return value;
                }).all();
    }

    @Override
    public Flux<BucketRow> typeDistribution(long tenantId, LocalDateTime from) {
        String type = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "JSON_UNQUOTE(JSON_EXTRACT(alarm_ext, '$.type'))";
            default -> "alarm_ext ->> 'type'";
        };
        return databaseClient.sql("SELECT " + type + " AS bucket_key, COUNT(*) AS count FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND create_time>=:from_time AND " + type + " IS NOT NULL"
                        + " GROUP BY " + type + " ORDER BY count DESC")
                .bind("tenant_id", tenantId).bind("from_time", from)
                .map((row, metadata) -> {
                    BucketRow value = new BucketRow();
                    value.setBucketKey(row.get("bucket_key"));
                    value.setCount(number(row.get("count")));
                    return value;
                }).all();
    }

    @Override
    public Flux<SourceCountRow> stormSources(long tenantId, LocalDateTime from, int minCount, int limit) {
        return sourceCount("SELECT " + sourceExpression() + " AS source, entity_id AS source_id, COUNT(*) AS count FROM " + TABLE
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time GROUP BY alarm_target_type_flag, entity_id"
                + " HAVING COUNT(*)>=:min_count ORDER BY count DESC LIMIT :limit", tenantId, from, limit, minCount);
    }

    @Override
    public Flux<FlappingRow> flappingSources(long tenantId, LocalDateTime from, int minCount, int limit) {
        return databaseClient.sql("SELECT " + sourceExpression() + " AS source, entity_id AS source_id, alarm_type_flag, COUNT(*) AS count FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND create_time>=:from_time GROUP BY alarm_target_type_flag, entity_id, alarm_type_flag"
                        + " HAVING COUNT(*)>=:min_count ORDER BY count DESC LIMIT :limit")
                .bind("tenant_id", tenantId).bind("from_time", from).bind("min_count", minCount).bind("limit", limit)
                .map((row, metadata) -> {
                    FlappingRow value = new FlappingRow();
                    value.setSource(text(row.get("source")));
                    value.setSourceId(number(row.get("source_id")));
                    value.setAlarmTypeFlag((int) number(row.get("alarm_type_flag")));
                    value.setCount(number(row.get("count")));
                    return value;
                }).all();
    }

    @Override
    public Flux<CorrelationPairRow> correlationPairs(long tenantId, LocalDateTime from, int windowSec, int limit) {
        String distance = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "ABS(TIMESTAMPDIFF(MICROSECOND, e2.create_time, e1.create_time)) <= :window_micros";
            default -> "ABS(EXTRACT(EPOCH FROM (e1.create_time-e2.create_time))) <= :window_seconds";
        };
        String sql = "WITH ev AS (SELECT " + sourceExpression() + " AS source, entity_id AS source_id, alarm_type_flag, create_time FROM " + TABLE
                + " WHERE tenant_id=:tenant_id AND create_time>=:from_time) SELECT e1.source AS a_source, e1.source_id AS a_source_id,"
                + " e1.alarm_type_flag AS a_event_type, e2.source AS b_source, e2.source_id AS b_source_id,"
                + " e2.alarm_type_flag AS b_event_type, COUNT(*) AS co_count FROM ev e1 JOIN ev e2 ON " + distance
                + " AND (e1.source<>e2.source OR e1.source_id<>e2.source_id)"
                + " AND (e1.source,e1.source_id,e1.alarm_type_flag)<(e2.source,e2.source_id,e2.alarm_type_flag)"
                + " GROUP BY e1.source,e1.source_id,e1.alarm_type_flag,e2.source,e2.source_id,e2.alarm_type_flag"
                + " ORDER BY co_count DESC LIMIT :limit";
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql).bind("tenant_id", tenantId)
                .bind("from_time", from).bind("limit", limit);
        spec = "mysql".equalsIgnoreCase(dialect.name()) || "mariadb".equalsIgnoreCase(dialect.name())
                ? spec.bind("window_micros", windowSec * 1_000_000L)
                : spec.bind("window_seconds", windowSec);
        return spec.map((row, metadata) -> {
            CorrelationPairRow value = new CorrelationPairRow();
            value.setASource(text(row.get("a_source")));
            value.setASourceId(number(row.get("a_source_id")));
            value.setAEventType((int) number(row.get("a_event_type")));
            value.setBSource(text(row.get("b_source")));
            value.setBSourceId(number(row.get("b_source_id")));
            value.setBEventType((int) number(row.get("b_event_type")));
            value.setCoCount(number(row.get("co_count")));
            return value;
        }).all();
    }

    @Override
    public Flux<PeerAlarmRow> peerAlarmCounts(long tenantId, LocalDateTime from) {
        return databaseClient.sql("SELECT d.profile_id, ea.device_id, COUNT(*) AS alarm_count "
                        + "FROM " + TABLE + " ea JOIN dc3_manager.dc3_device d ON d.id=ea.device_id "
                        + "AND d.tenant_id=ea.tenant_id AND d.deleted=0 WHERE ea.tenant_id=:tenant_id "
                        + "AND ea.alarm_target_type_flag IN (0,1) AND ea.create_time>=:from_time "
                        + "GROUP BY d.profile_id, ea.device_id")
                .bind("tenant_id", tenantId).bind("from_time", from)
                .map((row, metadata) -> {
                    PeerAlarmRow value = new PeerAlarmRow();
                    value.setProfileId(number(row.get("profile_id")));
                    value.setDeviceId(number(row.get("device_id")));
                    value.setAlarmCount(number(row.get("alarm_count")));
                    return value;
                }).all();
    }

    @Override
    public Mono<AgingBucketRow> agingBuckets(long tenantId) {
        String age = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "TIMESTAMPDIFF(SECOND, create_time, CURRENT_TIMESTAMP)";
            default -> "EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP-create_time))";
        };
        return databaseClient.sql("SELECT COALESCE(SUM(CASE WHEN age<3600 THEN 1 ELSE 0 END),0) AS under_1h, "
                        + "COALESCE(SUM(CASE WHEN age>=3600 AND age<21600 THEN 1 ELSE 0 END),0) AS h1_to_6, "
                        + "COALESCE(SUM(CASE WHEN age>=21600 AND age<86400 THEN 1 ELSE 0 END),0) AS h6_to_24, "
                        + "COALESCE(SUM(CASE WHEN age>=86400 THEN 1 ELSE 0 END),0) AS over_24h, COUNT(*) AS total "
                        + "FROM (SELECT " + age + " AS age FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND confirm_flag=0) ages")
                .bind("tenant_id", tenantId)
                .map((row, metadata) -> {
                    AgingBucketRow value = new AgingBucketRow();
                    value.setUnder1h(number(row.get("under_1h")));
                    value.setH1to6(number(row.get("h1_to_6")));
                    value.setH6to24(number(row.get("h6_to_24")));
                    value.setOver24h(number(row.get("over_24h")));
                    value.setTotal(number(row.get("total")));
                    return value;
                }).one().defaultIfEmpty(new AgingBucketRow());
    }

    @Override
    public Flux<MttaTrendRow> mttaByDay(long tenantId, LocalDateTime from) {
        String day = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "DATE(create_time)";
            default -> "CAST(create_time AS DATE)";
        };
        String latency = switch (dialect.name().toLowerCase(Locale.ROOT)) {
            case "mysql", "mariadb" -> "TIMESTAMPDIFF(MICROSECOND, create_time, operate_time)/1000.0";
            default -> "EXTRACT(EPOCH FROM (operate_time-create_time))*1000.0";
        };
        String sql = "WITH ranked AS (SELECT " + day + " AS day, " + latency + " AS latency_ms, "
                + "ROW_NUMBER() OVER (PARTITION BY " + day + " ORDER BY " + latency + ") AS rn, "
                + "COUNT(*) OVER (PARTITION BY " + day + ") AS cnt FROM " + TABLE
                + " WHERE tenant_id=:tenant_id AND confirm_flag=1 AND create_time>=:from_time) "
                + "SELECT CAST(day AS VARCHAR) AS day, "
                + "ROUND(MAX(CASE WHEN rn=FLOOR(0.50*(cnt-1)+1) THEN latency_ms END)) AS p50_ms, "
                + "ROUND(MAX(CASE WHEN rn=CEIL(0.95*(cnt-1)+1) THEN latency_ms END)) AS p95_ms, "
                + "MAX(cnt) AS confirmed_count FROM ranked GROUP BY day ORDER BY day ASC";
        return databaseClient.sql(sql).bind("tenant_id", tenantId).bind("from_time", from)
                .map((row, metadata) -> {
                    MttaTrendRow value = new MttaTrendRow();
                    value.setDate(text(row.get("day")));
                    value.setP50Ms(number(row.get("p50_ms")));
                    value.setP95Ms(number(row.get("p95_ms")));
                    value.setConfirmedCount(number(row.get("confirmed_count")));
                    return value;
                }).all();
    }

    @Override
    public Flux<ProtocolHealthRow> protocolHealth(long tenantId) {
        return databaseClient.sql("SELECT d.service_name, COUNT(DISTINCT d.id) AS driver_count, "
                        + "COUNT(DISTINCT CASE WHEN d.enable_flag=1 THEN d.id END) AS enabled_count, "
                        + "COUNT(DISTINCT dev.id) AS device_count FROM dc3_manager.dc3_driver d "
                        + "LEFT JOIN dc3_manager.dc3_device dev ON dev.driver_id=d.id AND dev.tenant_id=d.tenant_id AND dev.deleted=0 "
                        + "WHERE d.deleted=0 AND d.tenant_id=:tenant_id GROUP BY d.service_name "
                        + "ORDER BY device_count DESC, driver_count DESC")
                .bind("tenant_id", tenantId)
                .map((row, metadata) -> {
                    ProtocolHealthRow value = new ProtocolHealthRow();
                    value.setServiceName(text(row.get("service_name")));
                    value.setDriverCount(number(row.get("driver_count")));
                    value.setEnabledCount(number(row.get("enabled_count")));
                    value.setDeviceCount(number(row.get("device_count")));
                    return value;
                }).all();
    }

    @Override
    public Flux<RecentChangeRow> recentChanges(long tenantId, LocalDateTime from, int limit) {
        String sql = "SELECT kind, entity_id, operate_time FROM ("
                + "SELECT 'driver' AS kind, id AS entity_id, operate_time, create_time FROM dc3_manager.dc3_driver "
                + "WHERE deleted=0 AND tenant_id=:tenant_id AND operate_time>create_time AND operate_time>=:from_time "
                + "UNION ALL SELECT 'device', id, operate_time, create_time FROM dc3_manager.dc3_device "
                + "WHERE deleted=0 AND tenant_id=:tenant_id AND operate_time>create_time AND operate_time>=:from_time "
                + "UNION ALL SELECT 'profile', id, operate_time, create_time FROM dc3_manager.dc3_profile "
                + "WHERE deleted=0 AND tenant_id=:tenant_id AND operate_time>create_time AND operate_time>=:from_time"
                + ") changes ORDER BY operate_time DESC LIMIT :limit";
        return databaseClient.sql(sql).bind("tenant_id", tenantId).bind("from_time", from).bind("limit", limit)
                .map((row, metadata) -> {
                    RecentChangeRow value = new RecentChangeRow();
                    value.setKind(text(row.get("kind")));
                    value.setEntityId(number(row.get("entity_id")));
                    value.setOperateTime(time(row.get("operate_time")));
                    return value;
                }).all();
    }

    private Flux<SourceCountRow> sourceCount(String sql, long tenantId, LocalDateTime from, int limit) {
        return sourceCount(sql, tenantId, from, limit, null);
    }

    private Flux<SourceCountRow> sourceCount(String sql, long tenantId, LocalDateTime from, int limit, Integer minCount) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql).bind("tenant_id", tenantId)
                .bind("from_time", from).bind("limit", limit);
        if (minCount != null) spec = spec.bind("min_count", minCount);
        return spec.map((row, metadata) -> {
            SourceCountRow value = new SourceCountRow();
            value.setSource(text(row.get("source")));
            value.setSourceId(number(row.get("source_id")));
            value.setCount(number(row.get("count")));
            return value;
        }).all();
    }

    private Flux<SourceStatsRow> sourceStats(String sql, long tenantId) {
        return sourceStats(sql, tenantId, null);
    }

    private Flux<SourceStatsRow> sourceStats(String sql, long tenantId, LocalDateTime from) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql).bind("tenant_id", tenantId);
        if (from != null) {
            spec = spec.bind("from_time", from);
        }
        return spec.map((row, metadata) -> {
            SourceStatsRow value = new SourceStatsRow();
            value.setSource(text(row.get("source")));
            value.setTotal(number(row.get("total")));
            value.setUnconfirmed(number(row.get("unconfirmed")));
            return value;
        }).all();
    }

    private String sourceExpression() {
        return "CASE alarm_target_type_flag WHEN 0 THEN 'point' WHEN 1 THEN 'device' "
                + "WHEN 2 THEN 'driver' WHEN 3 THEN 'event' ELSE 'unknown' END";
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String text(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offset) return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (value instanceof String string) {
            try {
                return LocalDateTime.parse(string.replace(' ', 'T'));
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private String date(Object value) {
        if (value instanceof LocalDate date) return date.toString();
        if (value instanceof LocalDateTime dateTime) return dateTime.toLocalDate().toString();
        if (value instanceof OffsetDateTime offset) return offset.toLocalDate().toString();
        return text(value);
    }
}

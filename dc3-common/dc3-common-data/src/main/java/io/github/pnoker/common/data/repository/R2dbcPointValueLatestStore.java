package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/** Explicit SQL adapter for the tenant-scoped latest-value projection. */
@Repository
@ConditionalOnClass(DatabaseClient.class)
@RequiredArgsConstructor
public class R2dbcPointValueLatestStore implements ReactivePointValueLatestStore {

    private static final String TABLE = "dc3_history.dc3_point_latest";
    private static final String COLUMNS = "tenant_id, device_id, point_id, message_id, schema_version, driver_node, "
            + "sequence, fencing_token, raw_value, cal_value, num_value, driver_id, create_time, operate_time";

    private final DatabaseClient databaseClient;
    private final R2dbcDialect dialect;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<PointValueDO> latest(Long tenantId, Long deviceId, Long pointId) {
        if (!validKey(tenantId, deviceId, pointId)) return Mono.empty();
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND device_id=:device_id AND point_id=:point_id LIMIT 1")
                .bind("tenant_id", tenantId).bind("device_id", deviceId).bind("point_id", pointId)
                .map(this::map).one();
    }

    @Override
    public Flux<PointValueDO> listLatest(Long tenantId, Long deviceId, List<Long> pointIds) {
        List<Long> ids = pointIds == null ? List.of() : pointIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!validKey(tenantId, deviceId) || ids.isEmpty()) return Flux.empty();
        String placeholders = IntStream.range(0, ids.size()).mapToObj(i -> ":point_id_" + i)
                .reduce((left, right) -> left + "," + right).orElseThrow();
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id AND device_id=:device_id AND point_id IN (" + placeholders + ")")
                .bind("tenant_id", tenantId).bind("device_id", deviceId);
        for (int i = 0; i < ids.size(); i++) spec = spec.bind("point_id_" + i, ids.get(i));
        return spec.map(this::map).all();
    }

    @Override
    public Flux<PointValueDO> listLatestStream(Long tenantId, int limit) {
        if (tenantId == null || tenantId <= 0 || limit < 1) return Flux.empty();
        int bounded = Math.min(limit, 500);
        return databaseClient.sql("SELECT " + COLUMNS + " FROM " + TABLE
                        + " WHERE tenant_id=:tenant_id ORDER BY create_time DESC, device_id ASC, point_id ASC LIMIT :limit")
                .bind("tenant_id", tenantId).bind("limit", bounded).map(this::map).all();
    }

    @Override
    public Mono<Integer> upsertBatch(List<PointValueDO> values) {
        List<PointValueDO> rows = values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
        if (rows.isEmpty()) return Mono.just(0);
        return transactionalOperator.transactional(Flux.fromIterable(rows).concatMap(this::upsert).reduce(0, Integer::sum));
    }

    private Mono<Integer> upsert(PointValueDO value) {
        requireKey(value);
        String sql = "postgres".equalsIgnoreCase(dialect.name()) ? postgresUpsert() : mysqlUpsert();
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind("tenant_id", value.getTenantId()).bind("device_id", value.getDeviceId())
                .bind("point_id", value.getPointId()).bind("message_id", value.getMessageId())
                .bind("schema_version", value.getSchemaVersion()).bind("driver_node", value.getDriverNode())
                .bind("sequence", value.getSequence()).bind("fencing_token", value.getFencingToken())
                .bind("raw_value", value.getRawValue()).bind("cal_value", value.getCalValue())
                .bind("driver_id", value.getDriverId());
        spec = bindTime(spec, "create_time", value.getCreateTime());
        spec = bindTime(spec, "operate_time", value.getOperateTime());
        spec = bindNullable(spec, "num_value", value.getNumValue(), Double.class);
        return spec.fetch().rowsUpdated().map(Long::intValue);
    }

    private String postgresUpsert() {
        return "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (:tenant_id,:device_id,:point_id,:message_id,"
                + ":schema_version,:driver_node,:sequence,:fencing_token,:raw_value,:cal_value,:num_value,:driver_id,:create_time,:operate_time) "
                + "ON CONFLICT (tenant_id,device_id,point_id) DO UPDATE SET message_id=EXCLUDED.message_id,"
                + "schema_version=EXCLUDED.schema_version,driver_node=EXCLUDED.driver_node,sequence=EXCLUDED.sequence,"
                + "fencing_token=EXCLUDED.fencing_token,raw_value=EXCLUDED.raw_value,cal_value=EXCLUDED.cal_value,"
                + "num_value=EXCLUDED.num_value,driver_id=EXCLUDED.driver_id,create_time=EXCLUDED.create_time,operate_time=EXCLUDED.operate_time "
                + "WHERE (EXCLUDED.fencing_token,EXCLUDED.create_time,EXCLUDED.sequence,EXCLUDED.message_id) > "
                + "(dc3_point_latest.fencing_token,dc3_point_latest.create_time,dc3_point_latest.sequence,dc3_point_latest.message_id)";
    }

    private String mysqlUpsert() {
        return "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (:tenant_id,:device_id,:point_id,:message_id,"
                + ":schema_version,:driver_node,:sequence,:fencing_token,:raw_value,:cal_value,:num_value,:driver_id,:create_time,:operate_time) "
                + "ON DUPLICATE KEY UPDATE message_id=IF((VALUES(fencing_token),VALUES(create_time),VALUES(sequence),VALUES(message_id)) > "
                + "(fencing_token,create_time,sequence,message_id),VALUES(message_id),message_id), "
                + "schema_version=IF(VALUES(message_id)=message_id,VALUES(schema_version),schema_version), "
                + "driver_node=IF(VALUES(message_id)=message_id,VALUES(driver_node),driver_node), "
                + "sequence=IF(VALUES(message_id)=message_id,VALUES(sequence),sequence), "
                + "fencing_token=IF(VALUES(message_id)=message_id,VALUES(fencing_token),fencing_token), "
                + "raw_value=IF(VALUES(message_id)=message_id,VALUES(raw_value),raw_value), "
                + "cal_value=IF(VALUES(message_id)=message_id,VALUES(cal_value),cal_value), "
                + "num_value=IF(VALUES(message_id)=message_id,VALUES(num_value),num_value), "
                + "driver_id=IF(VALUES(message_id)=message_id,VALUES(driver_id),driver_id), "
                + "create_time=IF(VALUES(message_id)=message_id,VALUES(create_time),create_time), "
                + "operate_time=IF(VALUES(message_id)=message_id,VALUES(operate_time),operate_time)";
    }

    private PointValueDO map(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        PointValueDO value = new PointValueDO();
        value.setTenantId(number(row, "tenant_id", Long.class)); value.setDeviceId(number(row, "device_id", Long.class));
        value.setPointId(number(row, "point_id", Long.class)); value.setMessageId(row.get("message_id", String.class));
        value.setSchemaVersion(number(row, "schema_version", Integer.class)); value.setDriverNode(row.get("driver_node", String.class));
        value.setSequence(number(row, "sequence", Long.class)); value.setFencingToken(number(row, "fencing_token", Long.class));
        value.setRawValue(row.get("raw_value", String.class)); value.setCalValue(row.get("cal_value", String.class));
        value.setNumValue(number(row, "num_value", Double.class)); value.setDriverId(number(row, "driver_id", Long.class));
        value.setCreateTime(time(row.get("create_time"))); value.setOperateTime(time(row.get("operate_time")));
        return value;
    }

    private LocalDateTime time(Object value) {
        if (value instanceof LocalDateTime local) return local;
        if (value instanceof OffsetDateTime offset) return offset.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (value instanceof java.time.Instant instant) return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T number(io.r2dbc.spi.Row row, String name, Class<T> type) {
        Object raw = row.get(name);
        if (raw == null) return null;
        if (type.isInstance(raw)) return (T) raw;
        if (raw instanceof Number value) {
            if (type == Long.class) return (T) Long.valueOf(value.longValue());
            if (type == Integer.class) return (T) Integer.valueOf(value.intValue());
            if (type == Double.class) return (T) Double.valueOf(value.doubleValue());
        }
        throw new IllegalStateException("Column " + name + " is not numeric: " + raw.getClass());
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private DatabaseClient.GenericExecuteSpec bindTime(DatabaseClient.GenericExecuteSpec spec, String name, LocalDateTime value) {
        return "postgres".equalsIgnoreCase(dialect.name())
                ? spec.bind(name, value.atOffset(ZoneOffset.UTC))
                : spec.bind(name, value);
    }

    private boolean validKey(Long tenantId, Long deviceId) { return tenantId != null && tenantId > 0 && deviceId != null && deviceId > 0; }
    private boolean validKey(Long tenantId, Long deviceId, Long pointId) { return validKey(tenantId, deviceId) && pointId != null && pointId > 0; }
    private void requireKey(PointValueDO value) { if (!validKey(value.getTenantId(), value.getDeviceId(), value.getPointId())) throw new IllegalArgumentException("tenantId, deviceId and pointId are required"); }
}

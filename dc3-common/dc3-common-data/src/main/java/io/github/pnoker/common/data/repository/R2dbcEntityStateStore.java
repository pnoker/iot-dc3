package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Explicit SQL adapter for current driver/device leases. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcEntityStateStore implements ReactiveEntityStateStore {
    private static final String TABLE = "dc3_data.dc3_entity_state";
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcDialect dialect;

    @Override
    public Mono<Map<Long, Byte>> listStateFlags(Long tenantId, EntityTypeEnum type, Collection<Long> entityIds) {
        List<Long> ids = entityIds == null ? List.of() : entityIds.stream()
                .filter(id -> id != null && id > 0).distinct().toList();
        if (tenantId == null || tenantId <= 0 || type == null || ids.isEmpty()) return Mono.just(Map.of());
        String placeholders = java.util.stream.IntStream.range(0, ids.size())
                .mapToObj(index -> ":id" + index).reduce((left, right) -> left + "," + right).orElseThrow();
        String sql = "SELECT entity_id, entity_state_flag, expire_time FROM " + TABLE
                + " WHERE tenant_id=:tenant_id AND entity_type_flag=:entity_type AND entity_id IN ("
                + placeholders + ")";
        DatabaseClient.GenericExecuteSpec statement = databaseClient.sql(sql)
                .bind("tenant_id", tenantId).bind("entity_type", type.getIndex());
        for (int index = 0; index < ids.size(); index++) statement = statement.bind("id" + index, ids.get(index));
        return transactionalOperator.transactional(statement.map((row, metadata) -> {
            Number entityId = row.get("entity_id", Number.class);
            Number flag = row.get("entity_state_flag", Number.class);
            Object expiry = row.get("expire_time");
            byte state = flag == null ? 1 : flag.byteValue();
            if (expiry instanceof java.time.Instant instant && instant.isBefore(java.time.Instant.now())) state = 1;
            if (expiry instanceof java.time.OffsetDateTime offset && offset.toInstant().isBefore(java.time.Instant.now())) state = 1;
            if (expiry instanceof java.time.LocalDateTime local
                    && local.toInstant(java.time.ZoneOffset.UTC).isBefore(java.time.Instant.now())) state = 1;
            return Map.entry(entityId == null ? 0L : entityId.longValue(), state);
        }).all().collectList().map(entries -> {
            Map<Long, Byte> result = new LinkedHashMap<>();
            entries.forEach(entry -> result.put(entry.getKey(), entry.getValue()));
            return Map.copyOf(result);
        }));
    }

    @Override
    public Mono<Long> countOnline(Long tenantId, EntityTypeEnum type) {
        if (tenantId == null || tenantId <= 0 || type == null) return Mono.just(0L);
        String entityTable = type == EntityTypeEnum.DRIVER ? "dc3_manager.dc3_driver" : "dc3_manager.dc3_device";
        return databaseClient.sql("SELECT COUNT(*) AS total FROM " + TABLE + " s"
                        + " WHERE s.tenant_id=:tenant_id AND s.entity_type_flag=:entity_type"
                        + " AND s.entity_state_flag=:online AND (s.expire_time IS NULL OR s.expire_time >= CURRENT_TIMESTAMP)"
                        + " AND EXISTS (SELECT 1 FROM " + entityTable + " e WHERE e.id=s.entity_id"
                        + " AND e.tenant_id=s.tenant_id AND e.deleted=0)")
                .bind("tenant_id", tenantId)
                .bind("entity_type", type.getIndex())
                .bind("online", io.github.pnoker.common.enums.EntityStatusEnum.ONLINE.getIndex())
                .map((row, metadata) -> {
                    Number value = row.get("total", Number.class);
                    return value == null ? 0L : value.longValue();
                }).one().defaultIfEmpty(0L);
    }

    @Override
    public Mono<EntityStateLease> upsert(Long id, Long tenantId, EntityTypeEnum type, Long entityId,
                                         Long parentEntityId, byte stateFlag, byte initialLastStateFlag,
                                         Instant heartbeatAt, int timeoutSeconds, byte timeoutSourceFlag,
                                         String stateExt) {
        if (tenantId == null || tenantId <= 0 || type == null || entityId == null || timeoutSeconds <= 0) {
            return Mono.error(new IllegalArgumentException("tenant, type, entity and timeout are required"));
        }
        long stateId = id == null ? (UuidV7.nextLong()) : id;
        Instant now = heartbeatAt == null ? Instant.now() : heartbeatAt;
        LocalDateTime nowLocal = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LocalDateTime expires = nowLocal.plusSeconds(timeoutSeconds);
        String insert = "INSERT INTO " + TABLE + " (id,entity_type_flag,entity_id,parent_entity_id,entity_state_flag,"
                + "last_state_flag,lease_version,expire_time,timeout_seconds,last_heartbeat_time,last_alarm_id,"
                + "timeout_source_flag,entity_state_ext,tenant_id,create_time,operate_time) VALUES "
                + "(:id,:entity_type,:entity_id,:parent_entity,:state,:last_state,1,:expire,:timeout,:heartbeat,0,:timeout_source,"
                + dialect.jsonWriteExpression(":state_ext") + ",:tenant_id,:create_time,:operate_time) ";
        String stateExtensionUpdate = dialect.name().equals("postgres")
                ? "CASE WHEN CAST(:state_ext AS JSONB)->>'content' <> '' THEN jsonb_build_object('type',"
                + TABLE + ".entity_state_ext->>'type','content',CAST(:state_ext AS JSONB)->>'content','version',"
                + "COALESCE((" + TABLE + ".entity_state_ext->>'version')::int,0)+1) ELSE " + TABLE + ".entity_state_ext END"
                : "CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(:state_ext,'$.content')) <> '' THEN JSON_OBJECT('type',"
                + "JSON_UNQUOTE(JSON_EXTRACT(entity_state_ext,'$.type')),'content',JSON_UNQUOTE(JSON_EXTRACT(:state_ext,'$.content')),'version',"
                + "COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(entity_state_ext,'$.version')) AS SIGNED),0)+1) ELSE entity_state_ext END";
        String upsert = dialect.name().equals("postgres")
                ? insert + "ON CONFLICT (tenant_id,entity_type_flag,entity_id) DO UPDATE SET parent_entity_id=EXCLUDED.parent_entity_id,"
                + "entity_state_flag=EXCLUDED.entity_state_flag,last_state_flag=" + TABLE + ".entity_state_flag,"
                + "lease_version=" + TABLE + ".lease_version+1,expire_time=EXCLUDED.expire_time,timeout_seconds=EXCLUDED.timeout_seconds,"
                + "last_heartbeat_time=EXCLUDED.last_heartbeat_time,timeout_source_flag=EXCLUDED.timeout_source_flag,"
                + "entity_state_ext=" + stateExtensionUpdate + ",operate_time=EXCLUDED.operate_time"
                : insert + "ON DUPLICATE KEY UPDATE parent_entity_id=VALUES(parent_entity_id),last_state_flag=entity_state_flag,"
                + "entity_state_flag=VALUES(entity_state_flag),lease_version=lease_version+1,expire_time=VALUES(expire_time),"
                + "timeout_seconds=VALUES(timeout_seconds),last_heartbeat_time=VALUES(last_heartbeat_time),"
                + "timeout_source_flag=VALUES(timeout_source_flag),entity_state_ext=" + stateExtensionUpdate + ",operate_time=VALUES(operate_time)";
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(upsert)
                .bind("id", stateId).bind("entity_type", type.getIndex()).bind("entity_id", entityId)
                .bind("parent_entity", parentEntityId == null ? 0L : parentEntityId).bind("state", stateFlag)
                .bind("last_state", initialLastStateFlag).bind("timeout", timeoutSeconds)
                .bind("timeout_source", timeoutSourceFlag).bind("tenant_id", tenantId);
        spec = bindTime(spec, "expire", expires);
        spec = bindTime(spec, "heartbeat", nowLocal);
        spec = bindTime(spec, "create_time", nowLocal);
        spec = bindTime(spec, "operate_time", nowLocal);
        spec = bindNullable(spec, "state_ext", stateExt, String.class);
        return transactionalOperator.transactional(spec.fetch().rowsUpdated()
                .flatMap(rows -> rows > 0 ? findLease(tenantId, type, entityId)
                        : Mono.error(new IllegalStateException("entity state upsert affected " + rows + " rows"))));
    }

    @Override
    public Mono<Boolean> markAlarm(Long tenantId, EntityTypeEnum type, Long entityId, long leaseVersion, Long alarmId) {
        if (tenantId == null || type == null || entityId == null || alarmId == null) return Mono.just(false);
        return transactionalOperator.transactional(databaseClient.sql("UPDATE " + TABLE
                        + " SET last_alarm_id=:alarm_id, operate_time=CURRENT_TIMESTAMP WHERE tenant_id=:tenant_id"
                        + " AND entity_type_flag=:entity_type AND entity_id=:entity_id AND lease_version=:lease_version")
                .bind("alarm_id", alarmId).bind("tenant_id", tenantId).bind("entity_type", type.getIndex())
                .bind("entity_id", entityId).bind("lease_version", leaseVersion).fetch().rowsUpdated()
                .map(rows -> rows == 1));
    }

    @Override
    public Mono<EntityStateLease> claimExpired(Long tenantId, EntityTypeEnum type, Long entityId,
                                               long expectedLeaseVersion, int renewSeconds) {
        if (tenantId == null || tenantId <= 0 || type == null || entityId == null || entityId <= 0
                || expectedLeaseVersion < 1 || renewSeconds < 1) {
            return Mono.empty();
        }
        String renewExpression = "postgres".equals(dialect.name())
                ? "CURRENT_TIMESTAMP + (:renew * INTERVAL '1 second')"
                : "DATE_ADD(CURRENT_TIMESTAMP, INTERVAL :renew SECOND)";
        return transactionalOperator.transactional(databaseClient.sql("UPDATE " + TABLE
                        + " SET last_state_flag=entity_state_flag,entity_state_flag=:offline,"
                        + "lease_version=lease_version+1,expire_time=" + renewExpression
                        + ",last_alarm_id=0,operate_time=CURRENT_TIMESTAMP WHERE tenant_id=:tenant_id"
                        + " AND entity_type_flag=:entity_type AND entity_id=:entity_id"
                        + " AND lease_version=:lease_version AND entity_state_flag IN (:online,:maintain,:fault)"
                        + " AND expire_time <= CURRENT_TIMESTAMP")
                .bind("offline", EntityStatusEnum.OFFLINE.getIndex())
                .bind("renew", renewSeconds)
                .bind("tenant_id", tenantId)
                .bind("entity_type", type.getIndex())
                .bind("entity_id", entityId)
                .bind("lease_version", expectedLeaseVersion)
                .bind("online", EntityStatusEnum.ONLINE.getIndex())
                .bind("maintain", EntityStatusEnum.MAINTAIN.getIndex())
                .bind("fault", EntityStatusEnum.FAULT.getIndex())
                .fetch().rowsUpdated()
                .filter(rows -> rows == 1)
                .flatMap(ignored -> findLease(tenantId, type, entityId))
                .switchIfEmpty(findLease(tenantId, type, entityId)
                        .filter(state -> state.stateFlag() == EntityStatusEnum.OFFLINE.getIndex()
                                && state.leaseVersion() == expectedLeaseVersion + 1
                                && state.lastAlarmId() != null && state.lastAlarmId() > 0)));
    }

    @Override
    public Flux<EntityStateLease> claimExpired(EntityTypeEnum type, int limit, int renewSeconds) {
        if (type == null || limit < 1 || renewSeconds < 1) return Flux.empty();
        byte online = EntityStatusEnum.ONLINE.getIndex();
        byte maintain = EntityStatusEnum.MAINTAIN.getIndex();
        byte fault = EntityStatusEnum.FAULT.getIndex();
        byte offline = EntityStatusEnum.OFFLINE.getIndex();
        if ("postgres".equals(dialect.name())) {
            String sql = "WITH candidates AS (SELECT id FROM " + TABLE
                    + " WHERE entity_type_flag=:entity_type AND entity_state_flag IN (:online,:maintain,:fault)"
                    + " AND expire_time <= CURRENT_TIMESTAMP ORDER BY expire_time LIMIT :limit FOR UPDATE SKIP LOCKED)"
                    + " UPDATE " + TABLE + " s SET last_state_flag=s.entity_state_flag,entity_state_flag=:offline,"
                    + " lease_version=s.lease_version+1,expire_time=CURRENT_TIMESTAMP + (:renew * INTERVAL '1 second'),"
                    + " operate_time=CURRENT_TIMESTAMP FROM candidates c WHERE s.id=c.id RETURNING "
                    + "s.id,s.tenant_id,s.entity_type_flag,s.entity_id,s.parent_entity_id,s.entity_state_flag,s.last_state_flag,"
                    + "s.lease_version,s.expire_time,s.timeout_seconds,s.last_heartbeat_time,s.last_alarm_id,s.timeout_source_flag,s.entity_state_ext";
            return transactionalOperator.transactional(databaseClient.sql(sql).bind("entity_type", type.getIndex())
                            .bind("online", online).bind("maintain", maintain).bind("fault", fault).bind("offline", offline)
                            .bind("limit", limit).bind("renew", renewSeconds)
                            .map((row, metadata) -> lease(row, type)).all().collectList())
                    .flatMapMany(Flux::fromIterable);
        }
        String select = "SELECT id,tenant_id,entity_type_flag,entity_id,parent_entity_id,entity_state_flag,last_state_flag,"
                + "lease_version,expire_time,timeout_seconds,last_heartbeat_time,last_alarm_id,timeout_source_flag,entity_state_ext"
                + " FROM " + TABLE + " WHERE entity_type_flag=:entity_type AND entity_state_flag IN (:online,:maintain,:fault)"
                + " AND expire_time <= CURRENT_TIMESTAMP ORDER BY expire_time LIMIT :limit FOR UPDATE SKIP LOCKED";
        return transactionalOperator.transactional(
                databaseClient.sql(select).bind("entity_type", type.getIndex())
                        .bind("online", online).bind("maintain", maintain).bind("fault", fault).bind("limit", limit)
                        .map((row, metadata) -> lease(row, type)).all().collectList()
                        .flatMap(rows -> Flux.fromIterable(rows).concatMap(row ->
                                databaseClient.sql("UPDATE " + TABLE
                                                + " SET last_state_flag=entity_state_flag,entity_state_flag=:offline,lease_version=lease_version+1,"
                                                + "expire_time=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL :renew SECOND),operate_time=CURRENT_TIMESTAMP"
                                                + " WHERE id=:id AND lease_version=:lease_version")
                                        .bind("offline", offline).bind("renew", renewSeconds).bind("id", row.id())
                                        .bind("lease_version", row.leaseVersion()).fetch().rowsUpdated()
                                        .filter(updated -> updated == 1)
                                        .flatMapMany(ignored -> findLease(row.tenantId(), type, row.entityId()))
                                        .map(lease -> new EntityStateLease(lease.id(), lease.tenantId(), lease.type(), lease.entityId(),
                                                lease.parentEntityId(), lease.stateFlag(), row.stateFlag(), lease.leaseVersion(), lease.expireTime(),
                                                lease.timeoutSeconds(), lease.lastHeartbeatTime(), lease.lastAlarmId(), lease.timeoutSourceFlag(), lease.stateExt())))
                                .collectList()))
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<EntityStateLease> findLease(Long tenantId, EntityTypeEnum type, Long entityId) {
        return databaseClient.sql("SELECT id,tenant_id,entity_type_flag,entity_id,parent_entity_id,entity_state_flag,"
                        + "last_state_flag,lease_version,expire_time,timeout_seconds,last_heartbeat_time,last_alarm_id,"
                        + "timeout_source_flag,entity_state_ext FROM " + TABLE + " WHERE tenant_id=:tenant_id"
                        + " AND entity_type_flag=:entity_type AND entity_id=:entity_id LIMIT 1")
                .bind("tenant_id", tenantId).bind("entity_type", type.getIndex()).bind("entity_id", entityId)
                .map((row, metadata) -> lease(row, type))
                .one();
    }

    private EntityStateLease lease(io.r2dbc.spi.Row row, EntityTypeEnum type) {
        return new EntityStateLease(row.get("id", Long.class), row.get("tenant_id", Long.class), type,
                row.get("entity_id", Long.class), row.get("parent_entity_id", Long.class), number(row.get("entity_state_flag")),
                number(row.get("last_state_flag")), numberLong(row.get("lease_version")), instant(row.get("expire_time")),
                numberInt(row.get("timeout_seconds")), instant(row.get("last_heartbeat_time")), row.get("last_alarm_id", Long.class),
                number(row.get("timeout_source_flag")), text(row.get("entity_state_ext", String.class)));
    }

    private byte number(Object value) { return value instanceof Number number ? number.byteValue() : 0; }
    private long numberLong(Object value) { return value instanceof Number number ? number.longValue() : 0L; }
    private int numberInt(Object value) { return value instanceof Number number ? number.intValue() : 0; }
    private Instant instant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof java.time.OffsetDateTime offset) return offset.toInstant();
        if (value instanceof LocalDateTime local) return local.toInstant(ZoneOffset.UTC);
        return null;
    }
    private String text(String value) { return value; }
    private <T> DatabaseClient.GenericExecuteSpec bindNullable(DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private DatabaseClient.GenericExecuteSpec bindTime(DatabaseClient.GenericExecuteSpec spec, String name, LocalDateTime value) {
        return "postgres".equalsIgnoreCase(dialect.name())
                ? spec.bind(name, value.atOffset(ZoneOffset.UTC))
                : spec.bind(name, value);
    }
}

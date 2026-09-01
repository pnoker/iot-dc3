/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.github.pnoker.db.tck;

import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.data.repository.R2dbcEntityAlarmStore;
import io.github.pnoker.common.data.repository.R2dbcNotifyHistoryStore;
import io.github.pnoker.common.data.repository.NotifyHistoryInsertResult;
import io.github.pnoker.common.data.repository.ReactiveEntityAlarmStore;
import io.github.pnoker.common.data.repository.R2dbcEntityStateStore;
import io.github.pnoker.common.data.repository.R2dbcPointValueIngestOutbox;
import io.github.pnoker.common.data.repository.R2dbcPointValueLatestStore;
import io.github.pnoker.common.data.repository.ReactivePointValueIngestOutbox;
import io.github.pnoker.common.entity.ext.DriverExt;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.repository.R2dbcDriverStore;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect;
import io.github.pnoker.db.r2dbc.runtime.transaction.SpringR2dbcPageTransaction;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL R2DBC contract suite. Every assertion executes through the
 * same reactive repository ports used by production. No JDBC, MyBatis mapper,
 * blocking bridge or compatibility adapter is allowed in this test module.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDbDialectContractTest {

    private static long idSequence = System.currentTimeMillis();
    private final Map<String, ConnectionFactory> factories = new ConcurrentHashMap<>();
    private final Map<String, DatabaseClient> clients = new ConcurrentHashMap<>();

    protected abstract String r2dbcUrl();

    protected abstract String dialectName();

    protected abstract String fingerprintTable();

    protected abstract String operationTable();

    protected abstract String alarmTable();

    protected abstract String notifyHistoryTable();

    @AfterAll
    void closeFactories() {
        factories.values().forEach(factory -> {
            if (factory instanceof io.r2dbc.pool.ConnectionPool pool) pool.dispose();
        });
    }

    private static synchronized long id() {
        return ++idSequence;
    }

    private DatabaseClient client() {
        return clients.computeIfAbsent("default", ignored -> DatabaseClient.create(factory()));
    }

    private ConnectionFactory factory() {
        return factories.computeIfAbsent("default", ignored -> ConnectionFactories.get(r2dbcUrl()));
    }

    private R2dbcDialect dialect() {
        return new StandardR2dbcDialect(dialectName(), fingerprintTable(),
                "postgres".equals(dialectName()) ? '"' : '`', "postgres".equals(dialectName()));
    }

    private TransactionalOperator tx() {
        return TransactionalOperator.create(new R2dbcTransactionManager(factory()));
    }

    private R2dbcPointValueLatestStore latestStore() {
        return new R2dbcPointValueLatestStore(client(), dialect(), tx());
    }

    private R2dbcEntityStateStore stateStore() {
        return new R2dbcEntityStateStore(client(), tx(), dialect());
    }

    private ReactivePointValueIngestOutbox outbox() {
        return new R2dbcPointValueIngestOutbox(client(), dialect(), tx());
    }

    private ReactiveEntityAlarmStore alarmStore() {
        return new R2dbcEntityAlarmStore(client(), tx(), dialect());
    }

    private R2dbcNotifyHistoryStore notifyHistoryStore() {
        return new R2dbcNotifyHistoryStore(client(), tx(),
                new SpringR2dbcPageTransaction(new R2dbcTransactionManager(factory())), dialect());
    }

    private R2dbcDriverStore driverStore() {
        return new R2dbcDriverStore(client(), tx(),
                new SpringR2dbcPageTransaction(new R2dbcTransactionManager(factory())),
                JsonUtil.getJsonMapper(), dialect());
    }

    @Test
    void schemaFingerprintIsPresentAndFlagDayVersioned() {
        Map<String, Object> row = client().sql("SELECT fingerprint_version,schema_contract,id_format,time_format,json_format FROM "
                        + fingerprintTable() + " ORDER BY fingerprint_version DESC LIMIT 1")
                .map((result, metadata) -> Map.of(
                        "version", result.get("fingerprint_version"),
                        "contract", result.get("schema_contract"),
                        "id", result.get("id_format"),
                        "time", result.get("time_format"),
                        "json", result.get("json_format")))
                .one().block();
        assertThat(row).isNotNull();
        assertThat(((Number) row.get("version")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(row).containsEntry("contract", "r2dbc-flag-day-v1")
                .containsEntry("id", "uuidv7")
                .containsEntry("time", "utc-micros")
                .containsEntry("json", "canonical-v1");
    }

    @Test
    void latestUpsertIsFencedAndDeterministicallyOrdered() {
        long tenant = id(), device = id(), point = id();
        R2dbcPointValueLatestStore store = latestStore();
        store.upsertBatch(List.of(value(tenant, device, point, "m-old", 3, 10))).block();
        store.upsertBatch(List.of(value(tenant, device, point, "m-new", 9, 1))).block();
        store.upsertBatch(List.of(value(tenant, device, point, "m-stale", 2, 99))).block();

        PointValueDO latest = store.latest(tenant, device, point).block();
        assertThat(latest).isNotNull();
        assertThat(latest.getMessageId()).isEqualTo("m-new");
        assertThat(latest.getFencingToken()).isEqualTo(9L);

        LocalDateTime tieTime = LocalDateTime.of(2026, 1, 2, 0, 0);
        store.upsertBatch(List.of(value(tenant, device, point, "z", 9, 1, tieTime))).block();
        store.upsertBatch(List.of(value(tenant, device, point, "a", 9, 1, tieTime))).block();
        assertThat(store.latest(tenant, device, point).block().getMessageId()).isEqualTo("z");
    }

    @Test
    void entityLeaseUpsertAndFencedExpiryClaimAreReactive() {
        long tenant = id(), entity = id();
        R2dbcEntityStateStore store = stateStore();
        var first = store.upsert(id(), tenant, EntityTypeEnum.DEVICE, entity, id(), (byte) 2, (byte) 1,
                Instant.now().minusSeconds(10), 1, (byte) 0, "{\"content\":\"online\"}").block();
        assertThat(first).isNotNull();
        assertThat(first.leaseVersion()).isEqualTo(1L);
        var second = store.upsert(id(), tenant, EntityTypeEnum.DEVICE, entity, id(), (byte) 2, (byte) 1,
                Instant.now(), 60, (byte) 0, "{\"content\":\"renewed\"}").block();
        assertThat(second.leaseVersion()).isEqualTo(2L);
        assertThat(store.claimExpired(tenant, EntityTypeEnum.DEVICE, entity, 2L, 60).block()).isNull();
    }

    @Test
    void expiredLeaseClaimCommitsBeforeDownstreamCancellation() {
        long tenant = id(), entity = id();
        R2dbcEntityStateStore store = stateStore();
        store.upsert(id(), tenant, EntityTypeEnum.DEVICE, entity, id(), (byte) 2, (byte) 1,
                Instant.now().minusSeconds(10), 1, (byte) 0, "{}").block();

        store.claimExpired(EntityTypeEnum.DEVICE, 1, 60).take(1).blockLast();

        Map<String, Object> row = client().sql("SELECT entity_state_flag,lease_version FROM dc3_data.dc3_entity_state "
                        + "WHERE tenant_id=:tenant AND entity_type_flag=:type AND entity_id=:entity")
                .bind("tenant", tenant).bind("type", EntityTypeEnum.DEVICE.getIndex()).bind("entity", entity)
                .map((result, metadata) -> Map.of("state", result.get("entity_state_flag"), "version", result.get("lease_version")))
                .one().block();
        assertThat(row).isNotNull();
        assertThat(((Number) row.get("state")).byteValue()).isEqualTo(io.github.pnoker.common.enums.EntityStatusEnum.OFFLINE.getIndex());
        assertThat(((Number) row.get("version")).longValue()).isEqualTo(2L);
    }

    @Test
    void outboxLifecycleRejectsDuplicatesAndEnforcesOwnerFence() {
        long tenant = id(), device = id(), point = id();
        PointValueDO row = value(tenant, device, point, "receipt-1", 1, 1);
        ReactivePointValueIngestOutbox store = outbox();
        String owner = UUID.randomUUID().toString();
        assertThat(store.enqueue(List.of(row), owner).block()).hasSize(1);
        assertThat(store.enqueue(List.of(row), UUID.randomUUID().toString()).block()).isEmpty();
        assertThat(store.markPersisted(row, "wrong-owner").block()).isZero();
        assertThat(store.markPersisted(row, owner).block()).isEqualTo(1);
        assertThat(store.markProcessed(row).block()).isEqualTo(1);
        assertThat(store.markProcessed(row).block()).isZero();
        assertThat(store.findPersisted(List.of(row)).collectList().block()).isEmpty();
    }

    @Test
    void outboxReplayClaimIsExclusiveAndFailedRowsBackoff() {
        long tenant = id(), device = id(), point = id();
        PointValueDO row = value(tenant, device, point, "receipt-replay", 1, 1);
        ReactivePointValueIngestOutbox store = outbox();
        assertThat(store.enqueue(List.of(row), UUID.randomUUID().toString()).block()).hasSize(1);
        client().sql("UPDATE " + operationTable() + " SET status='PENDING',claimed_by=NULL,claimed_at=NULL "
                        + "WHERE tenant_id=:tenant AND message_id=:message")
                .bind("tenant", tenant).bind("message", "receipt-replay").fetch().rowsUpdated().block();
        String owner = UUID.randomUUID().toString();
        PointValueDO claimed = store.claim(owner, 1).blockFirst();
        assertThat(claimed).isNotNull();
        assertThat(store.claim(UUID.randomUUID().toString(), 1).blockFirst()).isNull();
        assertThat(store.markFailed(claimed, "wrong-owner", "boom").block()).isZero();
        assertThat(store.markFailed(claimed, owner, "boom").block()).isEqualTo(1);
    }

    @Test
    void alarmInsertIsIdempotentByTenantAndDedupeKey() {
        long tenant = id();
        EntityAlarmDO alarm = new EntityAlarmDO();
        alarm.setTenantId(tenant);
        alarm.setEntityId(id());
        alarm.setDriverId(id());
        alarm.setDeviceId(id());
        alarm.setPointId(id());
        alarm.setRuleId(id());
        alarm.setAlarmTargetTypeFlag((byte) 0);
        alarm.setAlarmTypeFlag((byte) 0);
        alarm.setAlarmSourceFlag((byte) 0);
        alarm.setAlarmLevelFlag((byte) 2);
        alarm.setConfirmFlag((byte) 0);
        alarm.setExpiredTime(0L);
        alarm.setDedupeKey("tck:" + tenant);

        EntityAlarmDO first = alarmStore().insert(alarm).block();
        EntityAlarmDO duplicate = new EntityAlarmDO();
        duplicate.setTenantId(tenant);
        duplicate.setEntityId(alarm.getEntityId());
        duplicate.setDriverId(alarm.getDriverId());
        duplicate.setDeviceId(alarm.getDeviceId());
        duplicate.setPointId(alarm.getPointId());
        duplicate.setRuleId(alarm.getRuleId());
        duplicate.setAlarmTargetTypeFlag((byte) 0);
        duplicate.setAlarmTypeFlag((byte) 0);
        duplicate.setAlarmSourceFlag((byte) 0);
        duplicate.setAlarmLevelFlag((byte) 2);
        duplicate.setConfirmFlag((byte) 0);
        duplicate.setExpiredTime(0L);
        duplicate.setDedupeKey(alarm.getDedupeKey());
        EntityAlarmDO second = alarmStore().insert(duplicate).block();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(client().sql("SELECT COUNT(*) AS total FROM " + alarmTable()
                        + " WHERE tenant_id=:tenant AND dedupe_key=:dedupe")
                .bind("tenant", tenant).bind("dedupe", alarm.getDedupeKey())
                .map((row, metadata) -> row.get("total", Number.class).longValue()).one().block()).isEqualTo(1L);
    }

    @Test
    void notifyHistoryInsertIsConcurrentAndIdempotentByTenantAndDedupeKey() {
        long tenant = id();
        String dedupeKey = "tck-notify:" + tenant;
        NotifyHistoryDO first = notifyHistory(tenant, dedupeKey);
        NotifyHistoryDO second = notifyHistory(tenant, dedupeKey);

        List<NotifyHistoryInsertResult> results = Flux.merge(
                        notifyHistoryStore().insertIdempotent(first),
                        notifyHistoryStore().insertIdempotent(second))
                .collectList().block();

        assertThat(results).hasSize(2);
        assertThat(results.stream().filter(NotifyHistoryInsertResult::inserted).count()).isEqualTo(1L);
        assertThat(results.get(0).history().getId()).isEqualTo(results.get(1).history().getId());
        assertThat(client().sql("SELECT COUNT(*) AS total FROM " + notifyHistoryTable()
                        + " WHERE tenant_id=:tenant AND dedupe_key=:dedupe")
                .bind("tenant", tenant).bind("dedupe", dedupeKey)
                .map((row, metadata) -> row.get("total", Number.class).longValue()).one().block()).isEqualTo(1L);
    }

    @Test
    void managerDriverWritesAreTenantScopedAndUseCompareAndSetVersions() {
        long tenant = id();
        DriverBO driver = new DriverBO();
        driver.setTenantId(tenant);
        driver.setDriverName("tck-driver-" + tenant);
        driver.setDriverCode("tck-driver-code-" + tenant);
        driver.setServiceName("tck-driver-service-" + tenant);
        driver.setServiceHost("127.0.0.1");
        driver.setVersion(0);
        DriverExt driverExt = new DriverExt();
        driverExt.setType("tck-driver");
        driverExt.setVersion(7);
        driverExt.setRemark("typed JSONB read");
        driverExt.setContent(new DriverExt.Content("nested-value"));
        driver.setDriverExt(driverExt);

        DriverBO inserted = driverStore().insert(driver).block();
        assertThat(inserted).isNotNull();
        assertThat(inserted.getVersion()).isZero();
        assertThat(inserted.getDriverExt()).isNotNull();
        assertThat(inserted.getDriverExt().getType()).isEqualTo("tck-driver");
        assertThat(inserted.getDriverExt().getContent().getKeep()).isEqualTo("nested-value");
        assertThat(driverStore().get(id(), inserted.getId()).block()).isNull();

        inserted.setDriverName(inserted.getDriverName() + "-updated");
        DriverBO updated = driverStore().update(inserted, 0).block();
        assertThat(updated).isNotNull();
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(updated.getDriverName()).endsWith("-updated");
        assertThat(updated.getDriverExt().getContent().getKeep()).isEqualTo("nested-value");
        assertThat(driverStore().update(updated, 0).block()).isNull();
        assertThat(driverStore().delete(tenant, updated.getId(), 0, id(), "tck").block()).isFalse();
        assertThat(driverStore().delete(tenant, updated.getId(), 1, id(), "tck").block()).isTrue();
        assertThat(driverStore().get(tenant, updated.getId()).block()).isNull();
    }

    private NotifyHistoryDO notifyHistory(long tenant, String dedupeKey) {
        NotifyHistoryDO value = new NotifyHistoryDO();
        value.setTenantId(tenant);
        value.setRuleId(id());
        value.setNotifyId(id());
        value.setMessageId(id());
        value.setChannelId(id());
        value.setAlarmId(id());
        value.setChannelTypeFlag((byte) 0);
        value.setStatusFlag((byte) 0);
        value.setDedupeKey(dedupeKey);
        return value;
    }

    private PointValueDO value(long tenant, long device, long point, String message, long fence, long sequence) {
        return value(tenant, device, point, message, fence, sequence,
                LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(sequence));
    }

    private PointValueDO value(long tenant, long device, long point, String message, long fence, long sequence,
                               LocalDateTime createTime) {
        PointValueDO value = new PointValueDO();
        value.setTenantId(tenant);
        value.setDeviceId(device);
        value.setPointId(point);
        value.setMessageId(message);
        value.setSchemaVersion(1);
        value.setDriverNode("tck-node");
        value.setSequence(sequence);
        value.setFencingToken(fence);
        value.setRawValue("raw");
        value.setCalValue("1");
        value.setNumValue(1D);
        value.setDriverId(id());
        value.setCreateTime(createTime);
        value.setOperateTime(createTime);
        return value;
    }
}

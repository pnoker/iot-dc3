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
package io.github.pnoker.db.postgres.manager;

import io.github.pnoker.common.manager.repository.ReactiveDriverLeaseStore;

import io.github.pnoker.common.manager.entity.model.DeviceLeaseDO;
import io.github.pnoker.common.manager.entity.model.DriverLeaseStateDO;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC implementation for driver leases and assignments. */
@Repository
@RequiredArgsConstructor
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class, TransactionalOperator.class})
public class R2dbcDriverLeaseStore implements ReactiveDriverLeaseStore {
    private static final String DRIVER_TABLE = "dc3_manager.dc3_driver";
    private static final String INSTANCE_TABLE = "dc3_manager.dc3_driver_instance";
    private static final String DEVICE_TABLE = "dc3_manager.dc3_device";
    private static final String LEASE_TABLE = "dc3_manager.dc3_device_lease";
    private static final String STATE_TABLE = "dc3_manager.dc3_driver_lease_state";
    private static final String REVISION_TABLE = "dc3_manager.dc3_driver_device_revision";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcDialect dialect;

    @Override
    public Mono<Void> acquireDriverLock(Long tenantId, Long driverId) {
        if (!valid(tenantId) || !valid(driverId)) {
            return Mono.error(new IllegalArgumentException("tenantId and driverId are required"));
        }
        return databaseClient
                .sql("SELECT id FROM " + DRIVER_TABLE
                        + " WHERE tenant_id=:tenant_id AND id=:driver_id AND deleted=0 FOR UPDATE")
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .fetch()
                .first()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("driver does not exist")))
                .then();
    }

    @Override
    public Mono<Void> renewInstance(
            Long tenantId, Long driverId, String node, String client, String host, Instant leaseUntil) {
        if (!valid(tenantId) || !valid(driverId) || blank(node) || blank(client) || blank(host) || leaseUntil == null) {
            return Mono.error(new IllegalArgumentException("invalid driver instance lease"));
        }
        String sql = postgres()
                ? "INSERT INTO " + INSTANCE_TABLE
                        + " (tenant_id, driver_id, node_id, client_id, service_host, started_at, last_heartbeat, lease_until) "
                        + "VALUES (:tenant_id, :driver_id, :node_id, :client_id, :service_host, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :lease_until) "
                        + "ON CONFLICT (tenant_id, driver_id, node_id) DO UPDATE SET "
                        + "client_id = EXCLUDED.client_id, service_host = EXCLUDED.service_host, "
                        + "last_heartbeat = CURRENT_TIMESTAMP, lease_until = EXCLUDED.lease_until"
                : "INSERT INTO " + INSTANCE_TABLE
                        + " (tenant_id, driver_id, node_id, client_id, service_host, started_at, last_heartbeat, lease_until) "
                        + "VALUES (:tenant_id, :driver_id, :node_id, :client_id, :service_host, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :lease_until) "
                        + "ON DUPLICATE KEY UPDATE client_id = VALUES(client_id), service_host = VALUES(service_host), "
                        + "last_heartbeat = CURRENT_TIMESTAMP, lease_until = VALUES(lease_until)";
        return databaseClient
                .sql(sql)
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .bind("node_id", node)
                .bind("client_id", client)
                .bind("service_host", host)
                .bind("lease_until", leaseUntil)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Flux<String> listActiveNodes(Long tenantId, Long driverId) {
        if (!valid(tenantId) || !valid(driverId)) return Flux.empty();
        return databaseClient
                .sql("SELECT node_id FROM " + INSTANCE_TABLE
                        + " WHERE tenant_id=:tenant_id AND driver_id=:driver_id AND lease_until > CURRENT_TIMESTAMP "
                        + "ORDER BY node_id ASC")
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .map((row, metadata) -> row.get("node_id", String.class))
                .all();
    }

    @Override
    public Flux<Long> listDriverDeviceIds(Long tenantId, Long driverId, long afterDeviceId, int limit) {
        if (!valid(tenantId) || !valid(driverId) || afterDeviceId < 0 || limit < 1) return Flux.empty();
        return databaseClient
                .sql("SELECT id FROM " + DEVICE_TABLE
                        + " WHERE tenant_id=:tenant_id AND driver_id=:driver_id AND deleted=0 AND enable_flag=0 "
                        + "AND id > :after_device_id ORDER BY id ASC LIMIT :limit")
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .bind("after_device_id", afterDeviceId)
                .bind("limit", limit)
                .map((row, metadata) -> row.get("id", Long.class))
                .all();
    }

    @Override
    public Mono<DriverLeaseStateDO> getLeaseState(Long tenantId, Long driverId) {
        if (!valid(tenantId) || !valid(driverId)) return Mono.empty();
        return databaseClient
                .sql("SELECT tenant_id, driver_id, membership_hash, device_revision, assignment_version " + "FROM "
                        + STATE_TABLE + " WHERE tenant_id=:tenant_id AND driver_id=:driver_id LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .map((row, metadata) -> {
                    DriverLeaseStateDO state = new DriverLeaseStateDO();
                    state.setTenantId(row.get("tenant_id", Long.class));
                    state.setDriverId(row.get("driver_id", Long.class));
                    state.setMembershipHash(row.get("membership_hash", String.class));
                    state.setDeviceRevision(row.get("device_revision", Long.class));
                    state.setAssignmentVersion(row.get("assignment_version", Long.class));
                    return state;
                })
                .one();
    }

    @Override
    public Mono<Long> getDeviceRevision(Long tenantId, Long driverId) {
        if (!valid(tenantId) || !valid(driverId)) return Mono.just(0L);
        return databaseClient
                .sql("SELECT revision FROM " + REVISION_TABLE
                        + " WHERE tenant_id=:tenant_id AND driver_id=:driver_id LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .map((row, metadata) -> row.get("revision", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Long> advanceAssignmentVersion(
            Long tenantId, Long driverId, String membershipHash, long deviceRevision) {
        if (!valid(tenantId) || !valid(driverId) || blank(membershipHash)) {
            return Mono.error(new IllegalArgumentException("invalid assignment state"));
        }
        String sql = "INSERT INTO " + STATE_TABLE
                + " (tenant_id, driver_id, membership_hash, device_revision, assignment_version, operate_time) "
                + "VALUES (:tenant_id, :driver_id, :membership_hash, :device_revision, 1, CURRENT_TIMESTAMP) "
                + (postgres()
                        ? "ON CONFLICT (tenant_id, driver_id) DO UPDATE SET membership_hash=EXCLUDED.membership_hash,"
                                + " device_revision=EXCLUDED.device_revision, assignment_version=" + STATE_TABLE
                                + ".assignment_version + 1,"
                                + " operate_time=CURRENT_TIMESTAMP RETURNING assignment_version"
                        : "ON DUPLICATE KEY UPDATE membership_hash=VALUES(membership_hash), device_revision=VALUES(device_revision),"
                                + " assignment_version=assignment_version + 1, operate_time=CURRENT_TIMESTAMP");
        DatabaseClient.GenericExecuteSpec statement = databaseClient
                .sql(sql)
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .bind("membership_hash", membershipHash)
                .bind("device_revision", deviceRevision);
        if (postgres()) {
            return statement
                    .map((row, metadata) -> row.get("assignment_version", Long.class))
                    .one();
        }
        return transactionalOperator.transactional(statement
                .fetch()
                .rowsUpdated()
                .then(databaseClient
                        .sql("SELECT assignment_version FROM " + STATE_TABLE
                                + " WHERE tenant_id=:tenant_id AND driver_id=:driver_id LIMIT 1")
                        .bind("tenant_id", tenantId)
                        .bind("driver_id", driverId)
                        .map((row, metadata) -> row.get("assignment_version", Long.class))
                        .one()));
    }

    @Override
    public Mono<Void> deleteExpiredInstances(Long tenantId, Long driverId, Instant expiredBefore) {
        if (!valid(tenantId) || !valid(driverId) || expiredBefore == null) return Mono.empty();
        return databaseClient
                .sql("DELETE FROM " + INSTANCE_TABLE
                        + " WHERE tenant_id=:tenant_id AND driver_id=:driver_id AND lease_until < :expired_before")
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .bind("expired_before", expiredBefore)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Void> reconcileDeviceLeases(List<DeviceLeaseDO> leases) {
        if (leases == null || leases.isEmpty()) return Mono.empty();
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(LEASE_TABLE)
                .append(" (tenant_id, driver_id, device_id, owner_node) VALUES ");
        List<Object> values = new ArrayList<>(leases.size() * 4);
        for (int index = 0; index < leases.size(); index++) {
            if (index > 0) sql.append(',');
            sql.append(" (:tenant_")
                    .append(index)
                    .append(", :driver_")
                    .append(index)
                    .append(", :device_")
                    .append(index)
                    .append(", :owner_")
                    .append(index)
                    .append(')');
            DeviceLeaseDO lease = leases.get(index);
            values.add(lease.getTenantId());
            values.add(lease.getDriverId());
            values.add(lease.getDeviceId());
            values.add(lease.getOwnerNode());
        }
        if (postgres()) {
            sql.append(" ON CONFLICT (tenant_id, device_id) DO UPDATE SET ")
                    .append("fencing_token = CASE WHEN ")
                    .append(LEASE_TABLE)
                    .append(".driver_id <> EXCLUDED.driver_id " + "OR ")
                    .append(LEASE_TABLE)
                    .append(".owner_node <> EXCLUDED.owner_node THEN ")
                    .append(LEASE_TABLE)
                    .append(".fencing_token + 1 ELSE ")
                    .append(LEASE_TABLE)
                    .append(".fencing_token END, ")
                    .append("operate_time = CASE WHEN ")
                    .append(LEASE_TABLE)
                    .append(".driver_id <> EXCLUDED.driver_id " + "OR ")
                    .append(LEASE_TABLE)
                    .append(".owner_node <> EXCLUDED.owner_node THEN CURRENT_TIMESTAMP ELSE ")
                    .append(LEASE_TABLE)
                    .append(".operate_time END, driver_id=EXCLUDED.driver_id, owner_node=EXCLUDED.owner_node");
        } else {
            sql.append(
                    " ON DUPLICATE KEY UPDATE fencing_token = IF(driver_id <> VALUES(driver_id) OR owner_node <> VALUES(owner_node),"
                            + " fencing_token + 1, fencing_token), operate_time = IF(driver_id <> VALUES(driver_id) OR owner_node <> VALUES(owner_node),"
                            + " CURRENT_TIMESTAMP, operate_time), driver_id=VALUES(driver_id), owner_node=VALUES(owner_node)");
        }

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (int index = 0; index < leases.size(); index++) {
            DeviceLeaseDO lease = leases.get(index);
            spec = spec.bind("tenant_" + index, lease.getTenantId())
                    .bind("driver_" + index, lease.getDriverId())
                    .bind("device_" + index, lease.getDeviceId())
                    .bind("owner_" + index, lease.getOwnerNode());
        }
        return spec.fetch().rowsUpdated().then();
    }

    @Override
    public Mono<Void> deleteOrphanedLeases(Long tenantId, Long driverId) {
        if (!valid(tenantId) || !valid(driverId)) return Mono.empty();
        return databaseClient
                .sql("DELETE FROM " + LEASE_TABLE + " lease WHERE lease.tenant_id=:tenant_id "
                        + "AND lease.driver_id=:driver_id AND (NOT EXISTS (SELECT 1 FROM " + DEVICE_TABLE + " device "
                        + "WHERE device.tenant_id=lease.tenant_id AND device.id=lease.device_id AND device.driver_id=lease.driver_id "
                        + "AND device.deleted=0 AND device.enable_flag=0) OR NOT EXISTS (SELECT 1 FROM "
                        + INSTANCE_TABLE + " instance "
                        + "WHERE instance.tenant_id=lease.tenant_id AND instance.driver_id=lease.driver_id "
                        + "AND instance.node_id=lease.owner_node AND instance.lease_until > CURRENT_TIMESTAMP))")
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Flux<DeviceLeaseDO> listOwnedLeases(
            Long tenantId, Long driverId, String node, long afterDeviceId, int limit) {
        if (!valid(tenantId) || !valid(driverId) || blank(node) || afterDeviceId < 0 || limit < 1) return Flux.empty();
        return databaseClient
                .sql("SELECT lease.tenant_id, lease.driver_id, lease.device_id, lease.owner_node, lease.fencing_token "
                        + "FROM " + LEASE_TABLE + " lease JOIN " + INSTANCE_TABLE
                        + " instance ON instance.tenant_id=lease.tenant_id "
                        + "AND instance.driver_id=lease.driver_id AND instance.node_id=lease.owner_node "
                        + "WHERE lease.tenant_id=:tenant_id AND lease.driver_id=:driver_id AND lease.owner_node=:owner_node "
                        + "AND instance.lease_until > CURRENT_TIMESTAMP AND lease.device_id > :after_device_id "
                        + "ORDER BY lease.device_id ASC LIMIT :limit")
                .bind("tenant_id", tenantId)
                .bind("driver_id", driverId)
                .bind("owner_node", node)
                .bind("after_device_id", afterDeviceId)
                .bind("limit", limit)
                .map(this::mapLease)
                .all();
    }

    @Override
    public Mono<DeviceLeaseDO> getActiveLease(Long tenantId, Long deviceId) {
        if (!valid(tenantId) || !valid(deviceId)) return Mono.empty();
        return databaseClient
                .sql("SELECT lease.tenant_id, lease.driver_id, lease.device_id, lease.owner_node, lease.fencing_token "
                        + "FROM " + LEASE_TABLE + " lease JOIN " + INSTANCE_TABLE + " instance "
                        + "ON instance.tenant_id=lease.tenant_id AND instance.driver_id=lease.driver_id "
                        + "AND instance.node_id=lease.owner_node WHERE lease.tenant_id=:tenant_id AND lease.device_id=:device_id "
                        + "AND instance.lease_until > CURRENT_TIMESTAMP LIMIT 1")
                .bind("tenant_id", tenantId)
                .bind("device_id", deviceId)
                .map(this::mapLease)
                .one();
    }

    private DeviceLeaseDO mapLease(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        return new DeviceLeaseDO(
                row.get("tenant_id", Long.class),
                row.get("driver_id", Long.class),
                row.get("device_id", Long.class),
                row.get("owner_node", String.class),
                row.get("fencing_token", Long.class));
    }

    private boolean valid(Long value) {
        return value != null && value > 0;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean postgres() {
        return "postgres".equalsIgnoreCase(dialect.name());
    }
}

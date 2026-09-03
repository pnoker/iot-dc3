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

import io.github.pnoker.common.manager.repository.ReactiveDeviceImportJobStore;

import io.github.pnoker.common.manager.entity.operation.DeviceImportJob;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for device import job records. */
@Repository
@ConditionalOnClass({DatabaseClient.class, TransactionalOperator.class})
@RequiredArgsConstructor
public class R2dbcDeviceImportJobStore implements ReactiveDeviceImportJobStore {

    private static final String TABLE = "dc3_manager.dc3_device_import_job";
    private static final String COLUMNS = "operation_id, tenant_id, driver_id, profile_id, operator_id, operator_name,"
            + " file_name, file_data, claimed_by, claimed_until, attempts";

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcDialect dialect;

    @Override
    public Mono<Void> insert(DeviceImportJob job) {
        Objects.requireNonNull(job, "job must not be null");
        return transactionalOperator.transactional(databaseClient
                .sql("INSERT INTO " + TABLE + " (" + COLUMNS
                        + ", created_at) VALUES (:operation_id,:tenant_id,:driver_id,:profile_id,:operator_id,"
                        + ":operator_name,:file_name,:file_data,:claimed_by,:claimed_until,:attempts,:created_at)")
                .bind("operation_id", uuid(job.operationId()))
                .bind("tenant_id", job.tenantId())
                .bind("driver_id", job.driverId())
                .bind("profile_id", job.profileId())
                .bind("operator_id", job.operatorId() == null ? 0L : job.operatorId())
                .bind("operator_name", value(job.operatorName()))
                .bind("file_name", value(job.fileName()))
                .bind("file_data", job.content())
                .bind("claimed_by", value(job.claimedBy()))
                .bindNull("claimed_until", Object.class)
                .bind("attempts", job.attempts())
                .bind("created_at", dialect.bindInstant(Instant.now()))
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1
                        ? Mono.empty()
                        : Mono.error(
                                new IllegalStateException("device import job insert affected " + rows + " rows"))));
    }

    @Override
    public Mono<DeviceImportJob> claim(UUID operationId, String workerId, Instant now, Instant claimedUntil) {
        if (operationId == null || workerId == null || workerId.isBlank()) return Mono.empty();
        return transactionalOperator.transactional(databaseClient
                .sql("UPDATE " + TABLE
                        + " SET claimed_by=:worker_id, claimed_until=:claimed_until, attempts=attempts+1"
                        + " WHERE operation_id=:operation_id AND (claimed_until IS NULL OR claimed_until<:now)")
                .bind("worker_id", workerId)
                .bind("claimed_until", dialect.bindInstant(claimedUntil))
                .bind("operation_id", uuid(operationId))
                .bind("now", dialect.bindInstant(now))
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows == 1 ? get(operationId) : Mono.empty()));
    }

    @Override
    public Flux<UUID> listRecoverable(Instant now) {
        return databaseClient
                .sql("SELECT operation_id FROM " + TABLE
                        + " WHERE claimed_until IS NULL OR claimed_until<:now ORDER BY created_at, operation_id")
                .bind("now", dialect.bindInstant(now))
                .map((row, metadata) -> readUuid(row.get("operation_id")))
                .all();
    }

    @Override
    public Mono<Boolean> renew(UUID operationId, String workerId, Instant claimedUntil) {
        if (operationId == null || workerId == null || workerId.isBlank()) return Mono.just(false);
        return databaseClient
                .sql("UPDATE " + TABLE + " SET claimed_until=:claimed_until"
                        + " WHERE operation_id=:operation_id AND claimed_by=:worker_id")
                .bind("claimed_until", dialect.bindInstant(claimedUntil))
                .bind("operation_id", uuid(operationId))
                .bind("worker_id", workerId)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows == 1);
    }

    @Override
    public Mono<Void> delete(UUID operationId, Long tenantId) {
        if (operationId == null || tenantId == null) return Mono.empty();
        return databaseClient
                .sql("DELETE FROM " + TABLE + " WHERE operation_id=:operation_id AND tenant_id=:tenant_id")
                .bind("operation_id", uuid(operationId))
                .bind("tenant_id", tenantId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private Mono<DeviceImportJob> get(UUID operationId) {
        return databaseClient
                .sql("SELECT " + COLUMNS + " FROM " + TABLE + " WHERE operation_id=:operation_id")
                .bind("operation_id", uuid(operationId))
                .map((row, metadata) -> new DeviceImportJob(
                        readUuid(row.get("operation_id")),
                        row.get("tenant_id", Long.class),
                        row.get("driver_id", Long.class),
                        row.get("profile_id", Long.class),
                        row.get("operator_id", Long.class),
                        row.get("operator_name", String.class),
                        row.get("file_name", String.class),
                        row.get("file_data", byte[].class),
                        row.get("claimed_by", String.class),
                        instant(row.get("claimed_until")),
                        number(row.get("attempts"))))
                .one();
    }

    private Object uuid(UUID value) {
        if (dialect.name().equals("postgres")) return value;
        byte[] bytes = new byte[16];
        long most = value.getMostSignificantBits();
        long least = value.getLeastSignificantBits();
        for (int index = 7; index >= 0; index--) {
            bytes[index] = (byte) most;
            most >>>= 8;
            bytes[index + 8] = (byte) least;
            least >>>= 8;
        }
        return bytes;
    }

    private UUID readUuid(Object value) {
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof byte[] bytes && bytes.length == 16) {
            long most = 0;
            long least = 0;
            for (int index = 0; index < 8; index++) {
                most = (most << 8) | (bytes[index] & 0xffL);
                least = (least << 8) | (bytes[index + 8] & 0xffL);
            }
            return new UUID(most, least);
        }
        return UUID.fromString(Objects.toString(value));
    }

    private Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime offset) return offset.toInstant();
        if (value instanceof LocalDateTime local) return local.toInstant(ZoneOffset.UTC);
        throw new IllegalStateException("unsupported job timestamp type");
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}

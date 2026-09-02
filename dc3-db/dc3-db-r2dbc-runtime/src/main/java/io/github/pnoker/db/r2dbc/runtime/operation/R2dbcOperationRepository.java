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
package io.github.pnoker.db.r2dbc.runtime.operation;

import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.operation.OperationRepository;
import io.github.pnoker.db.r2dbc.core.operation.OperationState;
import io.github.pnoker.db.r2dbc.core.tenant.TenantScope;
import io.github.pnoker.db.r2dbc.core.time.DatabaseInstant;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/** R2DBC implementation for the runtime operation/idempotency tables. */
public final class R2dbcOperationRepository implements OperationRepository {

    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcDialect dialect;
    private final String table;

    public R2dbcOperationRepository(
            DatabaseClient databaseClient, TransactionalOperator transactionalOperator, R2dbcDialect dialect) {
        this.databaseClient = Objects.requireNonNull(databaseClient, "databaseClient must not be null");
        this.transactionalOperator =
                Objects.requireNonNull(transactionalOperator, "transactionalOperator must not be null");
        this.dialect = Objects.requireNonNull(dialect, "dialect must not be null");
        this.table = dialect.quoteIdentifier(dialect.operationTable());
    }

    @Override
    public Mono<OperationState> create(TenantScope tenant, OperationState state) {
        return Mono.defer(() -> {
            requireTenant(tenant, state);
            String sql = "INSERT INTO " + table
                    + " (operation_id, tenant_id, idempotency_key, request_hash, status, progress, created_at, updated_at, expires_at)"
                    + " VALUES (:operation_id, :tenant_id, :idempotency_key, :request_hash, :status, :progress,"
                    + " :created_at, :updated_at, :expires_at)";
            DatabaseClient.GenericExecuteSpec statement = databaseClient
                    .sql(sql)
                    .bind("operation_id", state.operationId())
                    .bind("tenant_id", state.tenantId())
                    .bind("idempotency_key", state.idempotencyKey())
                    .bind("request_hash", state.requestHash())
                    .bind("status", state.status().name())
                    .bind("progress", state.progress())
                    .bind("created_at", dialect.bindInstant(state.createdAt()))
                    .bind("updated_at", dialect.bindInstant(state.updatedAt()));
            statement = state.expiresAt() == null
                    ? statement.bindNull("expires_at", Object.class)
                    : statement.bind("expires_at", dialect.bindInstant(state.expiresAt()));
            return transactionalOperator
                    .transactional(statement
                            .fetch()
                            .rowsUpdated()
                            .flatMap(rows -> rows == 1
                                    ? findById(tenant, state.operationId())
                                    : Mono.error(
                                            new IllegalStateException("operation insert affected " + rows + " rows"))))
                    .onErrorResume(
                            DataIntegrityViolationException.class,
                            error -> findByIdempotencyKey(tenant, state.idempotencyKey())
                                    .flatMap(existing -> sameRequest(existing, state)
                                            ? Mono.just(existing)
                                            : Mono.error(new IllegalArgumentException(
                                                    "idempotency key has already been used for a different operation")))
                                    .switchIfEmpty(Mono.error(error)));
        });
    }

    @Override
    public Mono<OperationState> findById(TenantScope tenant, UUID operationId) {
        Objects.requireNonNull(tenant, "tenant must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        return one(
                "SELECT operation_id, tenant_id, idempotency_key, request_hash, status, progress, result, error,"
                        + " created_at, updated_at, expires_at FROM " + table
                        + " WHERE tenant_id = :tenant_id AND operation_id = :operation_id",
                statement -> statement.bind("tenant_id", tenant.tenantId()).bind("operation_id", operationId));
    }

    @Override
    public Mono<OperationState> findByIdempotencyKey(TenantScope tenant, String idempotencyKey) {
        return Mono.defer(() -> {
            Objects.requireNonNull(tenant, "tenant must not be null");
            validateKey(idempotencyKey);
            return one(
                    "SELECT operation_id, tenant_id, idempotency_key, request_hash, status, progress, result, error,"
                            + " created_at, updated_at, expires_at FROM " + table
                            + " WHERE tenant_id = :tenant_id AND idempotency_key = :idempotency_key",
                    statement ->
                            statement.bind("tenant_id", tenant.tenantId()).bind("idempotency_key", idempotencyKey));
        });
    }

    @Override
    public Mono<OperationState> transition(
            TenantScope tenant, UUID operationId, OperationState.Status expectedStatus, OperationState nextState) {
        return Mono.defer(() -> {
            Objects.requireNonNull(tenant, "tenant must not be null");
            Objects.requireNonNull(operationId, "operationId must not be null");
            Objects.requireNonNull(expectedStatus, "expectedStatus must not be null");
            requireTenant(tenant, nextState);
            if (!operationId.equals(nextState.operationId())) {
                throw new IllegalArgumentException("next state operationId does not match operationId");
            }
            if (!OperationState.isTransitionAllowed(expectedStatus, nextState.status())) {
                throw new IllegalStateException(
                        "invalid operation transition: " + expectedStatus + " -> " + nextState.status());
            }
            String sql = "UPDATE " + table + " SET status = :status, progress = :progress, updated_at = :updated_at,"
                    + " result = " + dialect.jsonWriteExpression(":result") + ", error = "
                    + dialect.jsonWriteExpression(":error")
                    + " WHERE tenant_id = :tenant_id AND operation_id = :operation_id AND status = :expected_status";
            DatabaseClient.GenericExecuteSpec update = databaseClient
                    .sql(sql)
                    .bind("status", nextState.status().name())
                    .bind("progress", nextState.progress())
                    .bind("updated_at", dialect.bindInstant(nextState.updatedAt()))
                    .bind("tenant_id", tenant.tenantId())
                    .bind("operation_id", operationId)
                    .bind("expected_status", expectedStatus.name());
            update = nextState.result() == null
                    ? update.bindNull("result", String.class)
                    : update.bind("result", nextState.result());
            update = nextState.error() == null
                    ? update.bindNull("error", String.class)
                    : update.bind("error", nextState.error());
            return transactionalOperator.transactional(update.fetch()
                    .rowsUpdated()
                    .flatMap(rows -> rows == 1
                            ? findById(tenant, operationId)
                            : Mono.error(new OptimisticLockingFailureException(
                                    "operation status changed or operation does not exist"))));
        });
    }

    private Mono<OperationState> one(
            String sql,
            java.util.function.Function<DatabaseClient.GenericExecuteSpec, DatabaseClient.GenericExecuteSpec> binder) {
        return binder.apply(databaseClient.sql(sql))
                .map((row, metadata) -> new OperationState(
                        uuid(row.get("operation_id")),
                        longValue(row.get("tenant_id")),
                        row.get("idempotency_key", String.class),
                        parseStatus(row.get("status", String.class)),
                        number(row.get("progress")),
                        instant(row.get("created_at")),
                        instant(row.get("updated_at")),
                        optionalInstant(row.get("expires_at")),
                        row.get("request_hash", String.class),
                        json(row.get("result", String.class)),
                        json(row.get("error", String.class))))
                .one();
    }

    private static void requireTenant(TenantScope tenant, OperationState state) {
        Objects.requireNonNull(tenant, "tenant must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (!tenant.tenantId().equals(state.tenantId())) {
            throw new IllegalArgumentException("operation tenant does not match scope");
        }
    }

    private static void validateKey(String key) {
        if (key == null
                || key.isBlank()
                || key.codePoints().count() > OperationState.MAX_IDEMPOTENCY_KEY_LENGTH
                || !key.equals(key.trim())
                || key.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("idempotencyKey is invalid");
        }
    }

    private static boolean sameRequest(OperationState existing, OperationState requested) {
        return existing.requestHash().equals(requested.requestHash());
    }

    private static OperationState.Status parseStatus(String value) {
        try {
            return OperationState.Status.valueOf(Objects.requireNonNull(value, "status must not be null"));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("unknown operation status", exception);
        }
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException("operation progress is not numeric");
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(Objects.toString(value));
    }

    private static String json(String value) {
        return value;
    }

    private static UUID uuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(Objects.toString(value));
    }

    private static Instant optionalInstant(Object value) {
        return value == null ? null : instant(value);
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant instant) {
            return DatabaseInstant.normalize(instant);
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return DatabaseInstant.normalize(offsetDateTime.toInstant());
        }
        throw new IllegalStateException("operation timestamp has unsupported type: "
                + (value == null ? "null" : value.getClass().getName()));
    }
}

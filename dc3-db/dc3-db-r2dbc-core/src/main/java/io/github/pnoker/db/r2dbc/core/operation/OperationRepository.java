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
package io.github.pnoker.db.r2dbc.core.operation;

import io.github.pnoker.db.r2dbc.core.tenant.TenantScope;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Durable operation port used by asynchronous HTTP/gRPC commands.
 * Implementations must scope every lookup and mutation by tenant and must not
 * infer ownership from a request-local context.
 */
public interface OperationRepository {

    Mono<OperationState> create(TenantScope tenant, OperationState state);

    Mono<OperationState> findById(TenantScope tenant, UUID operationId);

    Mono<OperationState> findByIdempotencyKey(TenantScope tenant, String idempotencyKey);

    /**
     * Apply one validated state transition using optimistic status matching.
     * A missing row or stale expected status is reported as an error.
     */
    Mono<OperationState> transition(
            TenantScope tenant, UUID operationId, OperationState.Status expectedStatus, OperationState nextState);
}

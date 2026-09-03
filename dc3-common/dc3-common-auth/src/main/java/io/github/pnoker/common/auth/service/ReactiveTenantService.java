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
package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.repository.TenantFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive application service for the global tenant catalog. */
public interface ReactiveTenantService {

    /** Resolve the tenant by its id. */
    Mono<TenantBO> getById(Long id);

    /** Resolve the tenant by its code. */
    Mono<TenantBO> getByCode(String code);

    /** Page tenants matching the tenant-scoped filters. */
    Mono<OffsetPage<TenantBO>> list(TenantFilter filter);

    /** Add one tenant. */
    Mono<TenantBO> add(TenantBO tenant);

    /** Update one tenant and emit the updated row. */
    Mono<TenantBO> update(TenantBO tenant);

    /** Delete the tenant, reporting whether a row was removed. */
    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);
}

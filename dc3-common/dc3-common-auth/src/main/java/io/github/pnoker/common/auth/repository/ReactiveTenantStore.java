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
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Persistence port for the global tenant catalog. */
public interface ReactiveTenantStore {

    /** Resolve the tenant by its id. */
    Mono<TenantDO> getById(Long id);

    /** Resolve the enabled tenant by its code. */
    Mono<TenantDO> getEnabledByCode(String code);

    /** Resolve the tenant by its name and code. */
    Mono<TenantDO> getByNameAndCode(String tenantName, String tenantCode);

    /** Page tenants matching the tenant-scoped filters. */
    Mono<OffsetPage<TenantDO>> list(TenantFilter filter);

    /** Insert one tenant and emit the stored row. */
    Mono<TenantDO> insert(TenantDO tenant);

    /** Update one tenant and emit the updated row. */
    Mono<TenantDO> update(TenantDO tenant);

    /** Delete the tenant, reporting whether a row was removed. */
    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);
}

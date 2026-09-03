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

import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.model.RolePrincipalBindDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Non-blocking persistence port for role-principal bindings. */
public interface ReactiveRolePrincipalBindStore {
    /** Resolve the role principal binding by its id. */
    Mono<RolePrincipalBindDO> getById(Long tenantId, Long id);

    /** Page role principal bindings matching the tenant-scoped filters. */
    Mono<OffsetPage<RolePrincipalBindDO>> list(RolePrincipalBindFilter filter);

    /** Insert one role principal binding and emit the stored row. */
    Mono<RolePrincipalBindDO> insert(RolePrincipalBindBO binding);

    /** Delete the role principal binding, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);

    /** Check whether the role principal binding exists under the tenant. */
    Mono<Boolean> exists(Long tenantId, Long roleId, Long principalId, Long excludedId);

    /** Stream role ids matching the request. */
    Flux<Long> listRoleIds(Long tenantId, Long principalId);

    /** Stream principal ids matching the request. */
    Flux<Long> listPrincipalIds(Long tenantId, Long roleId, String principalType);
}

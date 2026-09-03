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

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Reactive persistence port for role records. */

public interface ReactiveRoleStore {
    /** Resolve the role by its id. */
    Mono<RoleDO> getById(Long tenantId, Long id);

    /** Page roles matching the tenant-scoped filters. */
    Mono<OffsetPage<RoleDO>> list(RoleFilter filter);

    /** Emit the role tree for the tenant. */
    Flux<RoleDO> listTree(RoleFilter filter);

    /** Insert one role and emit the stored row. */
    Mono<RoleDO> insert(RoleBO role);

    /** Update one role and emit the updated row. */
    Mono<RoleDO> update(Long tenantId, RoleBO role);

    /** Delete the role, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}

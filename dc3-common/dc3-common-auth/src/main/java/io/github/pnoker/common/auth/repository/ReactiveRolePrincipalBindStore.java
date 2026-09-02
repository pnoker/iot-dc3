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
    Mono<RolePrincipalBindDO> getById(Long tenantId, Long id);

    Mono<OffsetPage<RolePrincipalBindDO>> list(RolePrincipalBindFilter filter);

    Mono<RolePrincipalBindDO> insert(RolePrincipalBindBO binding);

    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);

    Mono<Boolean> exists(Long tenantId, Long roleId, Long principalId, Long excludedId);

    Flux<Long> listRoleIds(Long tenantId, Long principalId);

    Flux<Long> listPrincipalIds(Long tenantId, Long roleId, String principalType);
}

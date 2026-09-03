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

import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Persistence port for users; every tenant-scoped operation carries tenantId explicitly. */
public interface ReactiveUserStore {

    /** Resolve the user by its id. */
    Mono<UserDO> getById(Long tenantId, Long id);

    /** Resolve the user by its user name. */
    Mono<UserDO> getByUserName(Long tenantId, String userName);

    /** Resolve the user by its principal id. */
    Mono<UserDO> getByPrincipalId(Long tenantId, Long principalId);

    /** Page users matching the tenant-scoped filters. */
    Mono<OffsetPage<UserDO>> list(UserFilter filter);
}

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

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.repository.UserFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped user read service. */
public interface ReactiveUserService {
    Mono<UserBO> getById(Long tenantId, Long id);

    Mono<UserBO> getByUserName(Long tenantId, String userName);

    Mono<UserBO> getByPrincipalId(Long tenantId, Long principalId);

    Mono<OffsetPage<UserBO>> list(UserFilter filter);
}

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
package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.repository.ReactiveUserStore;
import io.github.pnoker.common.auth.repository.UserFilter;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking tenant-scoped user read service. */
@Service
@RequiredArgsConstructor
public class ReactiveUserServiceImpl implements ReactiveUserService {
    private final ReactiveUserStore userStore;
    private final UserBuilder userBuilder;

    @Override
    public Mono<UserBO> getById(Long tenantId, Long id) {
        return userStore
                .getById(tenantId, id)
                .map(userBuilder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("User")));
    }

    @Override
    public Mono<UserBO> getByUserName(Long tenantId, String userName) {
        if (userName == null || userName.isBlank()) return Mono.error(new RequestException("User name is required"));
        return userStore
                .getByUserName(tenantId, userName)
                .map(userBuilder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("User")));
    }

    @Override
    public Mono<UserBO> getByPrincipalId(Long tenantId, Long principalId) {
        return userStore
                .getByPrincipalId(tenantId, principalId)
                .map(userBuilder::buildBOByDO)
                .switchIfEmpty(Mono.error(new NotFoundException("User")));
    }

    @Override
    public Mono<OffsetPage<UserBO>> list(UserFilter filter) {
        return userStore
                .list(filter)
                .map(page -> OffsetPage.of(
                        page.items().stream().map(userBuilder::buildBOByDO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }
}

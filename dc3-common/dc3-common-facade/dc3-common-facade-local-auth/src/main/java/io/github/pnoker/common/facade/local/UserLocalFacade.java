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
package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.local.builder.FacadeUserBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** In-process facade for user operations. */
@Component
@RequiredArgsConstructor
public class UserLocalFacade implements UserFacade {
    private final ReactiveUserService userService;
    private final FacadeUserBuilder builder;

    @Override
    public Mono<FacadeUserBO> getById(Long tenantId, Long id) {
        return userService
                .getById(tenantId, id)
                .map(builder::toFacadeBO)
                .onErrorResume(NotFoundException.class, error -> Mono.empty());
    }

    @Override
    public Mono<FacadeUserBO> getByPrincipalId(Long tenantId, Long principalId) {
        return userService
                .getByPrincipalId(tenantId, principalId)
                .map(builder::toFacadeBO)
                .onErrorResume(NotFoundException.class, error -> Mono.empty());
    }
}

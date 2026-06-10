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

package io.github.pnoker.common.security;

import io.github.pnoker.common.facade.api.PermissionFacade;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Set;

/**
 * {@link PermissionProvider} backed by the active facade implementation.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@RequiredArgsConstructor
public class FacadePermissionProvider implements PermissionProvider {

    private final PermissionFacade permissionFacade;

    @Override
    public Mono<Boolean> hasPermission(Long tenantId, Long userId, String resourceCode) {
        if (tenantId == null || userId == null || resourceCode == null) {
            return Mono.just(false);
        }
        return listPermissionCodes(tenantId, userId)
                .map(codes -> codes.contains(resourceCode));
    }

    @Override
    public Mono<Set<String>> listPermissionCodes(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return Mono.just(Set.of());
        }
        return Mono.fromCallable(() -> permissionFacade.listPermissionCodes(tenantId, userId))
                .subscribeOn(Schedulers.boundedElastic());
    }

}

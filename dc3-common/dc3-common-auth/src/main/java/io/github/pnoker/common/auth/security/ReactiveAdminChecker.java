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
package io.github.pnoker.common.auth.security;

import io.github.pnoker.common.auth.service.ReactiveTenantService;
import io.github.pnoker.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Non-blocking authorization guard for system-global metadata mutations. */
@Component
@RequiredArgsConstructor
public class ReactiveAdminChecker {

    private final ReactiveTenantService tenantService;

    public Mono<Void> assertSystemAdmin(Long tenantId) {
        return tenantService
                .getById(tenantId)
                .flatMap(
                        tenant -> "default".equals(tenant.getTenantCode())
                                ? Mono.<Void>empty()
                                : Mono.error(
                                        new ServiceException(
                                                "Only system administrators can manage system-global entities (resources, menus, APIs)")));
    }
}

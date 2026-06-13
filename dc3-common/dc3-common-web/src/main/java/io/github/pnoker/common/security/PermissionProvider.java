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

import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * SPI for checking whether an authenticated user holds a given resource permission.
 * The auth module provides an in-process implementation backed by role-resource bindings.
 * Other services use {@link FacadePermissionProvider} to query the auth center.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface PermissionProvider {

    /**
     * Check whether the given user holds the named resource permission.
     *
     * @param tenantId     tenant id of the authenticated user
     * @param principalId  principal id of the authenticated caller
     * @param resourceCode permission code to check (maps to dc3_resource.resource_code)
     * @return Mono of true if the permission is granted
     */
    Mono<Boolean> hasPermission(Long tenantId, Long principalId, String resourceCode);

    /**
     * Return all resource codes held by the given user within the tenant.
     * Used to populate Spring Security {@code GrantedAuthority} set at authentication
     * time.
     *
     * @param tenantId    tenant scope
     * @param principalId target principal
     * @return Mono of resource code set (never null; empty set when user has no roles)
     */
    Mono<Set<String>> listPermissionCodes(Long tenantId, Long principalId);

    /**
     * Fail-closed default used only when no real auth or facade-backed provider is
     * present. This keeps the security chain active without silently granting access.
     */
    class DefaultPermissionProvider implements PermissionProvider {
        @Override
        public Mono<Boolean> hasPermission(Long tenantId, Long principalId, String resourceCode) {
            return Mono.just(false);
        }

        @Override
        public Mono<Set<String>> listPermissionCodes(Long tenantId, Long principalId) {
            return Mono.just(Set.of());
        }
    }
}

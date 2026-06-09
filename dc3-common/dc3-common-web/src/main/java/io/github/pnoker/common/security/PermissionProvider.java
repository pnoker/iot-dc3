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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * SPI for checking whether an authenticated user holds a given resource permission.
 * The auth module provides an implementation backed by the role-resource binding system.
 * The default implementation (used when auth module is absent) grants all permissions.
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
     * @param userId       user id of the authenticated user
     * @param resourceCode permission code to check (maps to dc3_resource.resource_code)
     * @return Mono of true if the permission is granted
     */
    Mono<Boolean> hasPermission(Long tenantId, Long userId, String resourceCode);

    /**
     * Return all resource codes held by the given user within the tenant.
     * Used to populate Spring Security {@code GrantedAuthority} set at authentication
     * time.
     *
     * @param tenantId tenant scope
     * @param userId   target user
     * @return Mono of resource code set (never null; empty set when user has no roles)
     */
    Mono<Set<String>> listPermissionCodes(Long tenantId, Long userId);

    /**
     * Permissive default: grants every permission when no auth-specific implementation
     * is present on the classpath. Replaced by AuthPermissionProvider in full deployments.
     */
    @Component
    @ConditionalOnMissingBean(value = PermissionProvider.class, name = "authPermissionProvider")
    class DefaultPermissionProvider implements PermissionProvider {
        @Override
        public Mono<Boolean> hasPermission(Long tenantId, Long userId, String resourceCode) {
            return Mono.just(true);
        }

        @Override
        public Mono<Set<String>> listPermissionCodes(Long tenantId, Long userId) {
            // Grant the wildcard authority so @perm.can(...) passes on services that
            // have no role/resource data (manager, data, agentic). These services
            // trust the gateway-authenticated identity; fine-grained authorization
            // is enforced at the auth center. See PermissionMethods.WILDCARD.
            return Mono.just(Set.of("*"));
        }
    }
}

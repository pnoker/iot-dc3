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

import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.security.PermissionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Production PermissionProvider backed by the role-resource binding system.
 * Caches permission results per user for a short TTL to avoid repeated database queries.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component("authPermissionProvider")
@RequiredArgsConstructor
public class AuthPermissionProvider implements PermissionProvider {

    private static final long CACHE_TTL_MS = 300_000; // 5 minutes
    private final RoleResourceBindService roleResourceBindService;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> hasPermission(Long tenantId, Long userId, String resourceCode) {
        if (tenantId == null || userId == null || resourceCode == null) {
            return Mono.just(false);
        }
        String cacheKey = tenantId + ":" + userId;
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && entry.isValid()) {
            return Mono.just(entry.resourceCodes.contains(resourceCode));
        }
        return Mono.fromCallable(() -> {
            var resources = roleResourceBindService.listResourceByUserId(userId, tenantId);
            Set<String> codes = resources.stream()
                    .map(r -> r.getResourceCode())
                    .filter(code -> code != null && !code.isBlank())
                    .collect(Collectors.toSet());
            cache.put(cacheKey, new CacheEntry(codes, CACHE_TTL_MS));
            return codes.contains(resourceCode);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static class CacheEntry {
        final Set<String> resourceCodes;
        final long expiresAt;

        CacheEntry(Set<String> codes, long ttlMs) {
            this.resourceCodes = Set.copyOf(codes);
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isValid() {
            return System.currentTimeMillis() < expiresAt;
        }
    }
}

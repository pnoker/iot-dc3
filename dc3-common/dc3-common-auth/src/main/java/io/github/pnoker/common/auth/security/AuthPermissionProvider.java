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

import io.github.pnoker.common.auth.repository.ReactivePermissionStore;
import io.github.pnoker.common.security.PermissionMethods;
import io.github.pnoker.common.security.PermissionProvider;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Production PermissionProvider backed by the role-resource binding system.
 * Caches permission results per principal for a short TTL to avoid repeated database queries.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component("authPermissionProvider")
@RequiredArgsConstructor
public class AuthPermissionProvider implements PermissionProvider, PermissionCacheInvalidator {

    private static final long CACHE_TTL_MS = 300_000; // 5 minutes
    private ReactivePermissionStore reactivePermissionStore;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setReactivePermissionStore(ReactivePermissionStore reactivePermissionStore) {
        this.reactivePermissionStore = reactivePermissionStore;
    }

    @Override
    public Mono<Set<String>> listPermissionCodes(Long tenantId, Long principalId) {
        if (tenantId == null || principalId == null) {
            return Mono.just(Set.of());
        }
        String cacheKey = tenantId + ":" + principalId;
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && entry.isValid()) {
            return Mono.just(entry.resourceCodes);
        }
        if (reactivePermissionStore == null)
            return Mono.error(new IllegalStateException("ReactivePermissionStore is not configured"));
        return reactivePermissionStore
                .listResourceCodes(tenantId, principalId)
                .collect(Collectors.toSet())
                .doOnNext(codes -> cache.put(cacheKey, new CacheEntry(codes, CACHE_TTL_MS)));
    }

    @Override
    public Mono<Boolean> hasPermission(Long tenantId, Long principalId, String resourceCode) {
        if (tenantId == null || principalId == null || resourceCode == null) {
            return Mono.just(false);
        }
        String cacheKey = tenantId + ":" + principalId;
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && entry.isValid()) {
            return Mono.just(entry.hasPermission(resourceCode));
        }
        if (reactivePermissionStore == null)
            return Mono.error(new IllegalStateException("ReactivePermissionStore is not configured"));
        return reactivePermissionStore
                .listResourceCodes(tenantId, principalId)
                .collect(Collectors.toSet())
                .doOnNext(codes -> cache.put(cacheKey, new CacheEntry(codes, CACHE_TTL_MS)))
                .map(codes -> codes.contains(PermissionMethods.WILDCARD) || codes.contains(resourceCode));
    }

    @Override
    public void invalidate(Long tenantId, Long principalId) {
        if (tenantId != null && principalId != null) cache.remove(tenantId + ":" + principalId);
    }

    @Override
    public void invalidateTenant(Long tenantId) {
        if (tenantId == null) return;
        String prefix = tenantId + ":";
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public void invalidateAll() {
        cache.clear();
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

        boolean hasPermission(String resourceCode) {
            return resourceCodes.contains(PermissionMethods.WILDCARD) || resourceCodes.contains(resourceCode);
        }
    }
}

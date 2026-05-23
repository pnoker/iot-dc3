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

package io.github.pnoker.common.auth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.constant.cache.TimeoutConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * In-memory denylist of cancelled tokens, keyed by (loginName, tenantCode).
 *
 * <p>
 * The platform replaced Redis with Caffeine for shared caches; this component keeps the
 * same model for token revocation. We do not store the full JWT — instead we record the
 * UTC milliseconds at which a user logged out. Any token whose {@code issuedAt} predates
 * that timestamp is treated as cancelled. One logout therefore invalidates every active
 * token for that login (which is the safer behaviour when the same account is used from
 * multiple devices).
 * </p>
 *
 * <p>
 * Entries expire after {@link TimeoutConstant#TOKEN_CACHE_TIMEOUT} hours so the cache
 * cannot grow without bound: by then every JWT issued before the logout has already
 * exceeded its own {@code exp} claim and would be rejected anyway.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.16
 */
@Component
public class TokenDenylistCache {

    private Cache<String, Long> cache;

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(100_000L)
                .expireAfterWrite(TimeoutConstant.TOKEN_CACHE_TIMEOUT, TimeUnit.HOURS)
                .build();
    }

    /**
     * Mark the given (loginName, tenantCode) pair as logged-out at {@code logoutEpochMs}.
     */
    public void markLogout(String loginName, String tenantCode, long logoutEpochMs) {
        if (Objects.isNull(loginName) || Objects.isNull(tenantCode)) {
            return;
        }
        cache.put(buildKey(loginName, tenantCode), logoutEpochMs);
    }

    /**
     * Return true when the JWT identified by ({@code loginName}, {@code tenantCode},
     * {@code issuedAtEpochMs}) was issued before a recorded logout — i.e. the client
     * cancelled this session and the token must be rejected.
     */
    public boolean isRevoked(String loginName, String tenantCode, long issuedAtEpochMs) {
        if (Objects.isNull(loginName) || Objects.isNull(tenantCode)) {
            return false;
        }
        Long logoutAt = cache.getIfPresent(buildKey(loginName, tenantCode));
        return Objects.nonNull(logoutAt) && issuedAtEpochMs <= logoutAt;
    }

    private String buildKey(String loginName, String tenantCode) {
        return tenantCode + SymbolConstant.COLON + loginName;
    }

}

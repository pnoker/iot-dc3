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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.cache.TimeoutConstant;

/**
 * Login-token lifetime, resolved once per JVM from {@code DC3_TOKEN_TTL_HOURS} / 
 * {@code dc3.token.ttl-hours}, defaulting to {@link TimeoutConstant#TOKEN_CACHE_TIMEOUT}.
 *
 * <p>Both the token signer and every cache that must outlive tokens (e.g. denylist)
 * read from here so a lifetime override cannot invalidate their invariant.
 *
 * @author pnoker
 */
public final class TokenTtl {

    private static final int MIN_HOURS = 1;
    private static final int MAX_HOURS = 24 * 30;

    private static final int HOURS = resolve();

    private TokenTtl() {
        throw new IllegalStateException("utility class");
    }

    /**
     * Configured login-token lifetime in whole hours.
     *
     * @return lifetime hours, clamped to [1, 720]
     */
    public static int hours() {
        return HOURS;
    }

    private static int resolve() {
        String raw = System.getenv("DC3_TOKEN_TTL_HOURS");
        if (raw == null || raw.isBlank()) {
            raw = System.getProperty("dc3.token.ttl-hours");
        }
        if (raw == null || raw.isBlank()) {
            return TimeoutConstant.TOKEN_CACHE_TIMEOUT;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < MIN_HOURS || value > MAX_HOURS) {
                throw new IllegalStateException(
                        "dc3.token.ttl-hours must be between " + MIN_HOURS + " and " + MAX_HOURS + ": " + raw);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("dc3.token.ttl-hours is not a number: " + raw, e);
        }
    }
}

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

package io.github.pnoker.common.mq.subscription;

/**
 * Bounded redelivery with exponential backoff; exhaustion routes to the dead-letter
 * instead of dropping. Defaults mirror the point-value batch consumer configuration.
 *
 * @param maxAttempts           maximum delivery attempts before dead-lettering
 * @param initialBackoffMillis  first retry delay
 * @param multiplier            backoff multiplier
 * @param maxBackoffMillis      backoff ceiling
 * @author pnoker
 * @since 2026.8.19
 */
public record RetryPolicy(int maxAttempts, long initialBackoffMillis, double multiplier, long maxBackoffMillis) {

    /**
     * The platform default: 3 attempts, 1s initial delay, 2x multiplier, 10s ceiling —
     * mirroring the point-value batch consumer settings.
     *
     * @return the default retry policy
     */
    public static RetryPolicy defaults() {
        return new RetryPolicy(3, 1000, 2, 10000);
    }
}

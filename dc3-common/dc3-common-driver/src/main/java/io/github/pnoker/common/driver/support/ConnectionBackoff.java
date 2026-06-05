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

package io.github.pnoker.common.driver.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-device exponential-backoff guard to prevent reconnection storms.
 * <p>
 * After a connection failure, subsequent connection attempts for the same device
 * are suppressed until the backoff window expires. The delay doubles on each
 * consecutive failure up to a maximum of 5 minutes. A single success resets the
 * backoff entirely.
 * </p>
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
public final class ConnectionBackoff {

    private static final long INITIAL_DELAY_MS = 1000;   // 1 second
    private static final long MAX_DELAY_MS = 300_000;      // 5 minutes
    private static final double MULTIPLIER = 2.0;

    private static final Map<Long, State> states = new ConcurrentHashMap<>();

    private ConnectionBackoff() {
    }

    /**
     * Check whether a connection attempt should be allowed for the given device.
     *
     * @param deviceId unique device identifier
     * @return {@code true} if the backoff window has expired or no backoff is active
     */
    public static boolean shouldAttempt(Long deviceId) {
        State state = states.get(deviceId);
        if (state == null) {
            return true;
        }
        return System.currentTimeMillis() >= state.nextAttemptAt;
    }

    /**
     * Reset backoff for the device after a successful operation.
     *
     * @param deviceId unique device identifier
     */
    public static void recordSuccess(Long deviceId) {
        states.remove(deviceId);
    }

    /**
     * Record a connection failure and advance the backoff window.
     *
     * @param deviceId unique device identifier
     */
    public static void recordFailure(Long deviceId) {
        State current = states.get(deviceId);
        long nextDelay = (current == null) ? INITIAL_DELAY_MS
                : Math.min((long) (current.delay * MULTIPLIER), MAX_DELAY_MS);
        states.put(deviceId, new State(System.currentTimeMillis() + nextDelay, nextDelay));
    }

    /**
     * Get the remaining backoff delay for the device in milliseconds.
     *
     * @param deviceId unique device identifier
     * @return remaining milliseconds, or 0 if no backoff is active
     */
    public static long remainingDelay(Long deviceId) {
        State state = states.get(deviceId);
        if (state == null) {
            return 0;
        }
        return Math.max(0, state.nextAttemptAt - System.currentTimeMillis());
    }

    /**
     * Immutable snapshot of the backoff state for a single device.
     */
    private static class State {
        final long nextAttemptAt;
        final long delay;

        State(long nextAttemptAt, long delay) {
            this.nextAttemptAt = nextAttemptAt;
            this.delay = delay;
        }
    }
}

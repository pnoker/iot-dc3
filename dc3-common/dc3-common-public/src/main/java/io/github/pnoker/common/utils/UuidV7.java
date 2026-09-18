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

import java.security.SecureRandom;
import java.util.UUID;

/** RFC 9562 UUID version 7 generator with process-local monotonic ordering. */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static long lastTimestamp;
    private static int sequence;

    private UuidV7() {
        throw new IllegalStateException("utility class");
    }

    /**
     * Generate a UUIDv7 using the current Unix epoch millisecond timestamp.
     * Clock rollback is handled by retaining the last timestamp and advancing
     * the twelve-bit monotonic sequence. Sequence exhaustion waits for the
     * next millisecond rather than producing a duplicate identifier.
     *
     * @return a process-locally monotonic UUID version 7 value
     */
    public static synchronized UUID next() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            if (sequence == 0xFFF) {
                do {
                    timestamp = System.currentTimeMillis();
                } while (timestamp <= lastTimestamp);
                sequence = RANDOM.nextInt(0x1000);
            } else {
                sequence++;
            }
        } else {
            sequence = RANDOM.nextInt(0x1000);
        }
        lastTimestamp = timestamp;

        long mostSignificantBits = (timestamp << 16) | (0x7L << 12) | sequence;
        long random = RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL;
        long leastSignificantBits = random | Long.MIN_VALUE;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    /**
     * Generate a positive 63-bit identifier for legacy BIGINT columns while
     * retaining entropy from both UUID halves. New schemas should persist the
     * UUID itself; this method is only for aggregates that still use BIGINT.
     *
     * @return a positive non-zero 63-bit identifier
     */
    public static long nextLong() {
        UUID value = next();
        long mixed = value.getMostSignificantBits() ^ Long.rotateLeft(value.getLeastSignificantBits(), 29);
        mixed &= Long.MAX_VALUE;
        return mixed == 0 ? 1 : mixed;
    }
}

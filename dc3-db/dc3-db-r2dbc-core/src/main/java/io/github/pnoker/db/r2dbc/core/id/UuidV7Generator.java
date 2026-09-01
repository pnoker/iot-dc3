package io.github.pnoker.db.r2dbc.core.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Monotonic UUIDv7 generator for identifiers that retain millisecond time order.
 */
public final class UuidV7Generator {

    private static final long RANDOM_B_MASK = 0x3fff_ffff_ffff_ffffL;
    private static final int RANDOM_A_MASK = 0x0fff;
    private static final long TIMESTAMP_MASK = 0x0000_ffff_ffff_ffffL;

    private final Clock clock;
    private final SecureRandom random;
    private long lastTimestamp = -1L;
    private int sequence;

    public UuidV7Generator() {
        this(Clock.systemUTC(), new SecureRandom());
    }

    public UuidV7Generator(Clock clock, SecureRandom random) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public synchronized UUID next() {
        long timestamp = clock.millis();
        if (timestamp < 0 || timestamp > TIMESTAMP_MASK) {
            throw new IllegalStateException("UUIDv7 timestamp is outside the 48-bit range");
        }
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp;
        }

        if (timestamp == lastTimestamp) {
            if (sequence == RANDOM_A_MASK) {
                // Do not spin on a frozen/test clock. Advancing the logical
                // timestamp preserves ordering and keeps generation bounded.
                if (timestamp == TIMESTAMP_MASK) {
                    throw new IllegalStateException("UUIDv7 timestamp exhausted");
                }
                timestamp++;
                sequence = random.nextInt(RANDOM_A_MASK + 1);
            } else {
                sequence++;
            }
        } else {
            sequence = random.nextInt(RANDOM_A_MASK + 1);
        }
        lastTimestamp = timestamp;

        long mostSignificantBits = (timestamp << 16)
                | (0x7L << 12)
                | sequence;
        long leastSignificantBits = 0x8000_0000_0000_0000L | (random.nextLong() & RANDOM_B_MASK);
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

}

package io.github.pnoker.db.r2dbc.core.id;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UuidV7GeneratorTest {

    @Test
    void generatesVersionSevenVariantTwoIdentifiersInOrder() {
        UuidV7Generator generator = new UuidV7Generator(
                Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC),
                new SecureRandom());

        UUID previous = generator.next();
        for (int index = 0; index < 100; index++) {
            UUID current = generator.next();
            assertEquals(7, current.version());
            assertEquals(2, current.variant());
            assertTrue(current.compareTo(previous) > 0);
            previous = current;
        }
    }

    @Test
    void doesNotSpinWhenClockIsFrozenAtSequenceOverflow() {
        UuidV7Generator generator = new UuidV7Generator(
                Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC), new SecureRandom());

        UUID previous = generator.next();
        for (int index = 0; index < 4_096; index++) {
            previous = generator.next();
        }

        UUID next = generator.next();
        assertTrue(next.compareTo(previous) > 0);
    }

    @Test
    void rejectsTimestampsThatCannotBeEncodedInUuidV7() {
        UuidV7Generator generator = new UuidV7Generator(
                Clock.fixed(Instant.ofEpochMilli(-1), ZoneOffset.UTC), new SecureRandom());

        assertThrows(IllegalStateException.class, generator::next);
    }

    @Test
    void matchesTheRfc9562UuidV7TestVector() {
        SecureRandom deterministicRandom = new SecureRandom() {
            @Override
            public int nextInt(int bound) {
                return 0x0cc3;
            }

            @Override
            public long nextLong() {
                return 0x18c4_dc0c_0c07_398fL;
            }
        };
        UuidV7Generator generator = new UuidV7Generator(
                Clock.fixed(Instant.ofEpochMilli(0x017f_22e2_79b0L), ZoneOffset.UTC),
                deterministicRandom);

        assertEquals(UUID.fromString("017f22e2-79b0-7cc3-98c4-dc0c0c07398f"), generator.next());
    }
}

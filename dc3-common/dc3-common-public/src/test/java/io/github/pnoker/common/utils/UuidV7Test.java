package io.github.pnoker.common.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7Test {

    @Test
    void generatesRfc9562VersionAndVariant() {
        UUID value = UuidV7.next();

        assertThat(value.version()).isEqualTo(7);
        assertThat(value.variant()).isEqualTo(2);
    }

    @Test
    void generatedValuesAreMonotonicWithinProcess() {
        UUID previous = UuidV7.next();
        for (int index = 0; index < 10_000; index++) {
            UUID current = UuidV7.next();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }

    @Test
    void bigintFallbackUsesBothUuidHalvesAndNeverReturnsZero() {
        Set<Long> values = new HashSet<>();
        for (int index = 0; index < 10_000; index++) {
            long value = UuidV7.nextLong();
            assertThat(value).isPositive();
            values.add(value);
        }
        assertThat(values).hasSize(10_000);
    }
}

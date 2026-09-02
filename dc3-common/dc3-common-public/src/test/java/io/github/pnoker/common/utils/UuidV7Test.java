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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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

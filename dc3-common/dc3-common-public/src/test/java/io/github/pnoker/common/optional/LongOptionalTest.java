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

package io.github.pnoker.common.optional;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LongOptionalTest {

    @Test
    void ifPresentAcceptsPositive() {
        AtomicLong captured = new AtomicLong(-1);
        LongOptional.ofNullable(42L).ifPresent(captured::set);
        assertThat(captured.get()).isEqualTo(42L);
    }

    @Test
    void ifPresentRejectsNullZeroNegative() {
        AtomicBoolean called = new AtomicBoolean(false);
        LongOptional.ofNullable(null).ifPresent(value -> called.set(true));
        LongOptional.ofNullable(0L).ifPresent(value -> called.set(true));
        LongOptional.ofNullable(-1L).ifPresent(value -> called.set(true));
        assertThat(called).isFalse();
    }

    @Test
    void ifPresentOrElseFiresFallbackForNonPositive() {
        AtomicBoolean fallback = new AtomicBoolean(false);
        AtomicLong captured = new AtomicLong(-1);
        LongOptional.ofNullable(null).ifPresentOrElse(captured::set, () -> fallback.set(true));
        assertThat(fallback).isTrue();

        AtomicBoolean fallback2 = new AtomicBoolean(false);
        LongOptional.ofNullable(7L).ifPresentOrElse(captured::set, () -> fallback2.set(true));
        assertThat(captured.get()).isEqualTo(7L);
        assertThat(fallback2).isFalse();
    }
}

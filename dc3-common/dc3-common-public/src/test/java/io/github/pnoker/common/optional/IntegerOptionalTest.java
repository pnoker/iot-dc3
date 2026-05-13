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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IntegerOptionalTest {

    @Test
    void ifPresentAcceptsPositive() {
        AtomicInteger captured = new AtomicInteger(-1);
        IntegerOptional.ofNullable(7).ifPresent(captured::set);
        assertThat(captured.get()).isEqualTo(7);
    }

    @Test
    void ifPresentRejectsNullAndZeroAndNegative() {
        AtomicBoolean called = new AtomicBoolean(false);
        IntegerOptional.ofNullable(null).ifPresent(value -> called.set(true));
        IntegerOptional.ofNullable(0).ifPresent(value -> called.set(true));
        IntegerOptional.ofNullable(-1).ifPresent(value -> called.set(true));
        assertThat(called).isFalse();
    }

    @Test
    void ifPresentOrElseSplitsBetweenPositiveAndNonPositive() {
        AtomicBoolean fallbackForNull = new AtomicBoolean(false);
        AtomicInteger captured = new AtomicInteger(-1);
        IntegerOptional.ofNullable(null).ifPresentOrElse(captured::set, () -> fallbackForNull.set(true));
        assertThat(fallbackForNull).isTrue();

        AtomicBoolean fallbackForPositive = new AtomicBoolean(false);
        IntegerOptional.ofNullable(3).ifPresentOrElse(captured::set, () -> fallbackForPositive.set(true));
        assertThat(captured.get()).isEqualTo(3);
        assertThat(fallbackForPositive).isFalse();
    }
}

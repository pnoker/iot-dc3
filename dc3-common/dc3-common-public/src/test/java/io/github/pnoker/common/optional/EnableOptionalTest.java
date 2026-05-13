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

import io.github.pnoker.common.enums.EnableFlagEnum;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class EnableOptionalTest {

    @Test
    void ifPresentInvokesActionForKnownByteIndex() {
        AtomicReference<EnableFlagEnum> captured = new AtomicReference<>();
        EnableOptional.ofNullable((byte) 0).ifPresent(captured::set);
        assertThat(captured.get()).isEqualTo(EnableFlagEnum.ENABLE);
    }

    @Test
    void ifPresentInvokesActionForKnownIntIndex() {
        AtomicReference<EnableFlagEnum> captured = new AtomicReference<>();
        EnableOptional.ofNullable(0).ifPresent(captured::set);
        assertThat(captured.get()).isEqualTo(EnableFlagEnum.ENABLE);
    }

    @Test
    void ifPresentRejectsUnknownIndex() {
        AtomicBoolean called = new AtomicBoolean(false);
        EnableOptional.ofNullable((byte) 99).ifPresent(value -> called.set(true));
        assertThat(called).isFalse();
    }

    @Test
    void ifPresentOrElseFiresFallbackForUnknownIndex() {
        AtomicBoolean fallback = new AtomicBoolean(false);
        EnableOptional.ofNullable((byte) 99).ifPresentOrElse(value -> {}, () -> fallback.set(true));
        assertThat(fallback).isTrue();
    }

    @Test
    void ifPresentOrElseInvokesActionForKnownIndex() {
        AtomicReference<EnableFlagEnum> captured = new AtomicReference<>();
        AtomicBoolean fallback = new AtomicBoolean(false);
        EnableOptional.ofNullable((byte) 0)
                .ifPresentOrElse(captured::set, () -> fallback.set(true));
        assertThat(captured.get()).isEqualTo(EnableFlagEnum.ENABLE);
        assertThat(fallback).isFalse();
    }
}

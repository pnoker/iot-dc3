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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JsonOptionalTest {

    @Test
    void ifPresentInvokesActionForValidJson() {
        AtomicReference<String> captured = new AtomicReference<>();
        JsonOptional.ofNullable("{\"a\":1}").ifPresent(captured::set);
        assertThat(captured.get()).isEqualTo("{\"a\":1}");
    }

    @Test
    void ifPresentRejectsInvalidJson() {
        AtomicBoolean called = new AtomicBoolean(false);
        JsonOptional.ofNullable("not-json").ifPresent(value -> called.set(true));
        assertThat(called).isFalse();
    }

    @Test
    void ifPresentRejectsNullAndEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);
        JsonOptional.ofNullable(null).ifPresent(value -> called.set(true));
        JsonOptional.ofNullable("").ifPresent(value -> called.set(true));
        assertThat(called).isFalse();
    }

    @Test
    void ifPresentOrElseFiresFallbackForInvalidJson() {
        AtomicBoolean fallback = new AtomicBoolean(false);
        JsonOptional.ofNullable("garbage").ifPresentOrElse(value -> {}, () -> fallback.set(true));
        assertThat(fallback).isTrue();
    }

    @Test
    void ifPresentOrElseInvokesActionForValidJson() {
        AtomicReference<String> captured = new AtomicReference<>();
        AtomicBoolean fallback = new AtomicBoolean(false);
        JsonOptional.ofNullable("[1,2]").ifPresentOrElse(captured::set, () -> fallback.set(true));
        assertThat(captured.get()).isEqualTo("[1,2]");
        assertThat(fallback).isFalse();
    }
}

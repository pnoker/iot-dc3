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

class StringOptionalTest {

    @Test
    void ifPresentInvokesActionForNonEmpty() {
        AtomicReference<String> captured = new AtomicReference<>();
        StringOptional.ofNullable("dc3").ifPresent(captured::set);
        assertThat(captured.get()).isEqualTo("dc3");
    }

    @Test
    void ifPresentSkipsActionForNull() {
        AtomicBoolean called = new AtomicBoolean(false);
        StringOptional.ofNullable(null).ifPresent(value -> called.set(true));
        assertThat(called).isFalse();
    }

    @Test
    void ifPresentSkipsActionForEmptyString() {
        AtomicBoolean called = new AtomicBoolean(false);
        StringOptional.ofNullable("").ifPresent(value -> called.set(true));
        assertThat(called).isFalse();
    }

    @Test
    void ifPresentOrElseRunsActionForPresent() {
        AtomicReference<String> captured = new AtomicReference<>();
        AtomicBoolean fallback = new AtomicBoolean(false);
        StringOptional.ofNullable("dc3").ifPresentOrElse(captured::set, () -> fallback.set(true));
        assertThat(captured.get()).isEqualTo("dc3");
        assertThat(fallback).isFalse();
    }

    @Test
    void ifPresentOrElseRunsFallbackForAbsent() {
        AtomicBoolean called = new AtomicBoolean(false);
        AtomicBoolean fallback = new AtomicBoolean(false);
        StringOptional.ofNullable(null).ifPresentOrElse(value -> called.set(true), () -> fallback.set(true));
        assertThat(called).isFalse();
        assertThat(fallback).isTrue();
    }
}

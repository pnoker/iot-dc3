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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionOptionalTest {

    @Test
    void ifPresentInvokesActionForNonEmptyCollection() {
        AtomicReference<Collection<Integer>> captured = new AtomicReference<>();
        CollectionOptional.ofNullable(List.of(1, 2)).ifPresent(captured::set);
        assertThat(captured.get()).containsExactly(1, 2);
    }

    @Test
    void ifPresentRejectsNullAndEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);
        CollectionOptional.<Integer>ofNullable(null).ifPresent(value -> called.set(true));
        CollectionOptional.<Integer>ofNullable(List.of()).ifPresent(value -> called.set(true));
        assertThat(called).isFalse();
    }

    @Test
    void ifPresentOrElseFiresFallbackForEmpty() {
        AtomicBoolean fallback = new AtomicBoolean(false);
        CollectionOptional.<Integer>ofNullable(List.of()).ifPresentOrElse(values -> {
        }, () -> fallback.set(true));
        assertThat(fallback).isTrue();
    }

    @Test
    void ifPresentOrElseInvokesActionForNonEmpty() {
        AtomicReference<Collection<?>> captured = new AtomicReference<>();
        AtomicBoolean fallback = new AtomicBoolean(false);
        CollectionOptional.ofNullable(List.of(7))
                .ifPresentOrElse(captured::set, () -> fallback.set(true));
        assertThat(captured.get()).hasSize(1).first().isEqualTo(7);
        assertThat(fallback).isFalse();
    }
}

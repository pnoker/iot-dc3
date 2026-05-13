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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapStructUtilTest {

    @Test
    void isNotEmptyReturnsTrueForNonBlank() {
        assertThat(MapStructUtil.isNotEmpty("hello")).isTrue();
    }

    @Test
    void isNotEmptyReturnsFalseForNullOrEmpty() {
        assertThat(MapStructUtil.isNotEmpty(null)).isFalse();
        assertThat(MapStructUtil.isNotEmpty("")).isFalse();
    }

    @Test
    void isValidNumberAcceptsFiniteValues() {
        assertThat(MapStructUtil.isValidNumber(1)).isTrue();
        assertThat(MapStructUtil.isValidNumber(1.5d)).isTrue();
        assertThat(MapStructUtil.isValidNumber(1.5f)).isTrue();
    }

    @Test
    void isValidNumberRejectsNaNAndInfinity() {
        assertThat(MapStructUtil.isValidNumber(Double.NaN)).isFalse();
        assertThat(MapStructUtil.isValidNumber(Double.POSITIVE_INFINITY)).isFalse();
        assertThat(MapStructUtil.isValidNumber(Float.NaN)).isFalse();
        assertThat(MapStructUtil.isValidNumber(Float.NEGATIVE_INFINITY)).isFalse();
    }

    @Test
    void isValidNumberRejectsNull() {
        assertThat(MapStructUtil.isValidNumber(null)).isFalse();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<MapStructUtil> constructor = MapStructUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}

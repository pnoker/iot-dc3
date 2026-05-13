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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ArithmeticUtilTest {

    @ParameterizedTest
    @CsvSource({
            "1.10, 2.20, 3.30",
            "0,    0,    0",
            "-1.5, 1.5,  0.0",
            "0.1,  0.2,  0.3",
            "1E10, 1E10, 2E10"
    })
    void shouldAddBigDecimalsWithoutFloatingPointDrift(String a, String b, String expected) {
        assertThat(ArithmeticUtil.add(a, b)).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void shouldAddFloatsViaBigDecimalRouting() {
        assertThat(ArithmeticUtil.add(0.1f, 0.2f)).isEqualTo(0.3f, offset(0.0001f));
    }

    @Test
    void shouldAddDoublesWithoutBinaryArtifacts() {
        assertThat(ArithmeticUtil.add(0.1d, 0.2d)).isEqualTo(0.3d);
    }

    @ParameterizedTest
    @CsvSource({
            "5.5, 2.2, 3.3",
            "0,   0,   0",
            "1,   2,   -1"
    })
    void shouldSubtractBigDecimals(String a, String b, String expected) {
        assertThat(ArithmeticUtil.subtract(a, b)).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void shouldSubtractFloats() {
        assertThat(ArithmeticUtil.subtract(1.0f, 0.4f)).isEqualTo(0.6f, offset(0.0001f));
    }

    @Test
    void shouldSubtractDoubles() {
        assertThat(ArithmeticUtil.subtract(1.0d, 0.9d)).isEqualTo(0.1d);
    }

    @Test
    void shouldMultiplyBigDecimals() {
        assertThat(ArithmeticUtil.multiply("0.1", "0.2")).isEqualByComparingTo(new BigDecimal("0.02"));
    }

    @Test
    void shouldMultiplyFloats() {
        assertThat(ArithmeticUtil.multiply(0.1f, 0.2f)).isEqualTo(0.02f, offset(0.0001f));
    }

    @Test
    void shouldMultiplyDoubles() {
        assertThat(ArithmeticUtil.multiply(0.1d, 0.2d)).isEqualTo(0.02d);
    }

    @Test
    void shouldDivideWithHalfUpRounding() {
        assertThat(ArithmeticUtil.divide("10", "3", 4)).isEqualByComparingTo(new BigDecimal("3.3333"));
    }

    @Test
    void shouldRejectFloatDivisionWithExcessiveScale() {
        assertThatThrownBy(() -> ArithmeticUtil.divide(1.0f, 3.0f, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ~ 7");
    }

    @Test
    void shouldRejectDoubleDivisionWithExcessiveScale() {
        assertThatThrownBy(() -> ArithmeticUtil.divide(1.0d, 3.0d, 17))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ~ 16");
    }

    @Test
    void shouldRejectDivisionByZero() {
        assertThatThrownBy(() -> ArithmeticUtil.divide("1", "0", 2))
                .isInstanceOf(ArithmeticException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "1.235, 2, 1.24",
            "1.234, 2, 1.23",
            "1.5,   0, 2",
            "-1.5,  0, -2"
    })
    void shouldRoundBigDecimalsHalfUp(String value, int scale, String expected) {
        assertThat(ArithmeticUtil.round(value, scale)).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void shouldRejectFloatRoundingWithExcessiveScale() {
        assertThatThrownBy(() -> ArithmeticUtil.round(1.0f, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ~ 7");
    }

    @Test
    void shouldRejectDoubleRoundingWithExcessiveScale() {
        assertThatThrownBy(() -> ArithmeticUtil.round(1.0d, 17))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ~ 16");
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<ArithmeticUtil> constructor = ArithmeticUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}

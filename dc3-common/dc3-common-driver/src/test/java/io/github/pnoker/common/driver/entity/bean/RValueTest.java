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
package io.github.pnoker.common.driver.entity.bean;

import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.OutRangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RValue} covering type conversion, numeric projection,
 * scaling, and edge cases.
 */
class RValueTest {

    private DeviceBO device;
    private PointBO point;

    @BeforeEach
    void setUp() {
        device = new DeviceBO();
        device.setId(1L);
        point = new PointBO();
        point.setId(10L);
    }

    private PointBO pointWithType(PointTypeFlagEnum type) {
        point.setPointTypeFlag(type);
        point.setBaseValue(BigDecimal.ZERO);
        point.setMultiple(BigDecimal.ONE);
        point.setValueDecimal((byte) 6);
        return point;
    }

    private PointBO pointWithTypeAndScale(PointTypeFlagEnum type, BigDecimal base, BigDecimal multiple, byte decimal) {
        point.setPointTypeFlag(type);
        point.setBaseValue(base);
        point.setMultiple(multiple);
        point.setValueDecimal(decimal);
        return point;
    }

    @Nested
    class StringType {

        @Test
        void stringValueReturnsRawInput() {
            pointWithType(PointTypeFlagEnum.STRING);
            RValue rValue = new RValue(device, point, "hello world");

            assertThat(rValue.getFinalValue()).isEqualTo("hello world");
            assertThat(rValue.getNumericValue()).isNull();
        }

        @Test
        void stringNumericTextStillNull() {
            pointWithType(PointTypeFlagEnum.STRING);
            RValue rValue = new RValue(device, point, "42");

            assertThat(rValue.getFinalValue()).isEqualTo("42");
            assertThat(rValue.getNumericValue()).isNull();
        }

        @Test
        void defaultTypeIsString() {
            point.setPointTypeFlag(null);
            point.setBaseValue(BigDecimal.ZERO);
            point.setMultiple(BigDecimal.ONE);
            RValue rValue = new RValue(device, point, "anything");

            assertThat(rValue.getFinalValue()).isEqualTo("anything");
            assertThat(rValue.getNumericValue()).isNull();
        }
    }

    @Nested
    class NumericTypes {

        @Test
        void byteType() {
            pointWithType(PointTypeFlagEnum.BYTE);
            RValue rValue = new RValue(device, point, "100");

            assertThat(rValue.getFinalValue()).isEqualTo("100");
            assertThat(rValue.getNumericValue()).isEqualTo(100.0);
        }

        @Test
        void shortType() {
            pointWithType(PointTypeFlagEnum.SHORT);
            RValue rValue = new RValue(device, point, "1000");

            assertThat(rValue.getFinalValue()).isEqualTo("1000");
            assertThat(rValue.getNumericValue()).isEqualTo(1000.0);
        }

        @Test
        void intType() {
            pointWithType(PointTypeFlagEnum.INT);
            RValue rValue = new RValue(device, point, "42");

            assertThat(rValue.getFinalValue()).isEqualTo("42");
            assertThat(rValue.getNumericValue()).isEqualTo(42.0);
        }

        @Test
        void longType() {
            pointWithType(PointTypeFlagEnum.LONG);
            RValue rValue = new RValue(device, point, "9999999999");

            assertThat(rValue.getFinalValue()).isEqualTo("9999999999");
            assertThat(rValue.getNumericValue()).isEqualTo(9999999999.0);
        }

        @Test
        void floatType() {
            pointWithType(PointTypeFlagEnum.FLOAT);
            RValue rValue = new RValue(device, point, "3.14");

            assertThat(rValue.getFinalValue()).isEqualTo("3.14");
            assertThat(rValue.getNumericValue()).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        void doubleType() {
            pointWithType(PointTypeFlagEnum.DOUBLE);
            RValue rValue = new RValue(device, point, "2.718281828");

            // ArithmeticUtil.round with decimal=6 rounds to 6 decimal places
            assertThat(rValue.getFinalValue()).isEqualTo("2.718282");
            assertThat(rValue.getNumericValue()).isCloseTo(2.718282, org.assertj.core.data.Offset.offset(0.000001));
        }

        @Test
        void negativeDouble() {
            pointWithType(PointTypeFlagEnum.DOUBLE);
            RValue rValue = new RValue(device, point, "-42.5");

            assertThat(rValue.getFinalValue()).isEqualTo("-42.5");
            assertThat(rValue.getNumericValue()).isEqualTo(-42.5);
        }

        @Test
        void zero() {
            pointWithType(PointTypeFlagEnum.INT);
            RValue rValue = new RValue(device, point, "0");

            assertThat(rValue.getFinalValue()).isEqualTo("0");
            assertThat(rValue.getNumericValue()).isEqualTo(0.0);
        }
    }

    @Nested
    class BooleanType {

        @Test
        void trueMapsToOne() {
            pointWithType(PointTypeFlagEnum.BOOLEAN);
            RValue rValue = new RValue(device, point, "true");

            assertThat(rValue.getFinalValue()).isEqualTo("true");
            assertThat(rValue.getNumericValue()).isEqualTo(1.0);
        }

        @Test
        void falseMapsToZero() {
            pointWithType(PointTypeFlagEnum.BOOLEAN);
            RValue rValue = new RValue(device, point, "false");

            assertThat(rValue.getFinalValue()).isEqualTo("false");
            assertThat(rValue.getNumericValue()).isEqualTo(0.0);
        }
    }

    @Nested
    class Scaling {

        @Test
        void offsetApplied() {
            pointWithTypeAndScale(PointTypeFlagEnum.INT, new BigDecimal("10"), BigDecimal.ONE, (byte) 0);
            RValue rValue = new RValue(device, point, "5");

            assertThat(rValue.getFinalValue()).isEqualTo("15");
            assertThat(rValue.getNumericValue()).isEqualTo(15.0);
        }

        @Test
        void multiplierApplied() {
            pointWithTypeAndScale(PointTypeFlagEnum.DOUBLE, BigDecimal.ZERO, new BigDecimal("2.5"), (byte) 2);
            RValue rValue = new RValue(device, point, "10");

            assertThat(rValue.getFinalValue()).isEqualTo("25.0");
            assertThat(rValue.getNumericValue()).isEqualTo(25.0);
        }

        @Test
        void linearTransformApplied() {
            pointWithTypeAndScale(PointTypeFlagEnum.DOUBLE, new BigDecimal("32"), new BigDecimal("1.8"), (byte) 2);
            RValue rValue = new RValue(device, point, "100");

            assertThat(rValue.getFinalValue()).isEqualTo("212.0");
            assertThat(rValue.getNumericValue()).isEqualTo(212.0);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void nullPointThrowsEmptyException() {
            RValue rValue = RValue.builder().device(device).point(null).value("42").build();

            assertThatThrownBy(rValue::getFinalValue).isInstanceOf(EmptyException.class);
        }

        @Test
        void outOfRangeByteThrows() {
            pointWithType(PointTypeFlagEnum.BYTE);
            RValue rValue = new RValue(device, point, "200");

            assertThatThrownBy(rValue::getFinalValue).isInstanceOf(OutRangeException.class);
        }

        @Test
        void outOfRangeShortThrows() {
            pointWithType(PointTypeFlagEnum.SHORT);
            RValue rValue = new RValue(device, point, "100000");

            assertThatThrownBy(rValue::getFinalValue).isInstanceOf(OutRangeException.class);
        }

        @Test
        void invalidNumberForIntThrows() {
            pointWithType(PointTypeFlagEnum.INT);
            RValue rValue = new RValue(device, point, "not-a-number");

            // BigDecimal constructor throws NumberFormatException, wrapped by OutRangeException
            assertThatThrownBy(rValue::getFinalValue).isInstanceOf(OutRangeException.class);
        }
    }

    @Nested
    class NumericValueCaching {

        @Test
        void numericValueIsCachedAfterGetFinalValue() {
            pointWithType(PointTypeFlagEnum.DOUBLE);
            RValue rValue = new RValue(device, point, "3.14");

            Double first = rValue.getNumericValue();
            // Before calling getFinalValue, numericValue may be null (convenience constructor doesn't compute)
            rValue.getFinalValue();
            Double after = rValue.getNumericValue();

            assertThat(after).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        void numericValueIsNullBeforeGetFinalValueWithBuilder() {
            pointWithType(PointTypeFlagEnum.DOUBLE);
            RValue rValue = RValue.builder().device(device).point(point).value("3.14").build();

            // Builder doesn't trigger getFinalValue, so numericValue should be null
            assertThat(rValue.getNumericValue()).isNull();
            rValue.getFinalValue();
            assertThat(rValue.getNumericValue()).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001));
        }
    }
}

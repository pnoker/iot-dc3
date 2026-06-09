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
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.OutRangeException;
import io.github.pnoker.common.exception.TypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ReadPointValue} covering type conversion, numeric projection,
 * scaling, and edge cases.
 */
class ReadPointValueTest {

    private DeviceBO device;
    private PointBO point;

    @BeforeEach
    void setUp() {
        device = new DeviceBO();
        device.setId(1L);
        point = new PointBO();
        point.setId(10L);
    }

    private PointBO pointWithType(PointTypeEnum type) {
        point.setPointTypeFlag(type);
        point.setBaseValue(BigDecimal.ZERO);
        point.setMultiple(BigDecimal.ONE);
        point.setValueDecimal((byte) 6);
        return point;
    }

    private PointBO pointWithTypeAndScale(PointTypeEnum type, BigDecimal base, BigDecimal multiple, byte decimal) {
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
            pointWithType(PointTypeEnum.STRING);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "hello world");

            assertThat(readPointValue.getFinalValue()).isEqualTo("hello world");
            assertThat(readPointValue.getNumericValue()).isNull();
        }

        @Test
        void stringNumericTextStillNull() {
            pointWithType(PointTypeEnum.STRING);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "42");

            assertThat(readPointValue.getFinalValue()).isEqualTo("42");
            assertThat(readPointValue.getNumericValue()).isNull();
        }

        @Test
        void defaultTypeIsString() {
            point.setPointTypeFlag(null);
            point.setBaseValue(BigDecimal.ZERO);
            point.setMultiple(BigDecimal.ONE);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "anything");

            assertThat(readPointValue.getFinalValue()).isEqualTo("anything");
            assertThat(readPointValue.getNumericValue()).isNull();
        }
    }

    @Nested
    class NumericTypes {

        @Test
        void byteType() {
            pointWithType(PointTypeEnum.BYTE);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "100");

            assertThat(readPointValue.getFinalValue()).isEqualTo("100");
            assertThat(readPointValue.getNumericValue()).isEqualTo(100.0);
        }

        @Test
        void shortType() {
            pointWithType(PointTypeEnum.SHORT);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "1000");

            assertThat(readPointValue.getFinalValue()).isEqualTo("1000");
            assertThat(readPointValue.getNumericValue()).isEqualTo(1000.0);
        }

        @Test
        void intType() {
            pointWithType(PointTypeEnum.INT);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "42");

            assertThat(readPointValue.getFinalValue()).isEqualTo("42");
            assertThat(readPointValue.getNumericValue()).isEqualTo(42.0);
        }

        @Test
        void longType() {
            pointWithType(PointTypeEnum.LONG);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "9999999999");

            assertThat(readPointValue.getFinalValue()).isEqualTo("9999999999");
            assertThat(readPointValue.getNumericValue()).isEqualTo(9999999999.0);
        }

        @Test
        void floatType() {
            pointWithType(PointTypeEnum.FLOAT);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "3.14");

            assertThat(readPointValue.getFinalValue()).isEqualTo("3.14");
            assertThat(readPointValue.getNumericValue()).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        void doubleType() {
            pointWithType(PointTypeEnum.DOUBLE);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "2.718281828");

            // ArithmeticUtil.round with decimal=6 rounds to 6 decimal places
            assertThat(readPointValue.getFinalValue()).isEqualTo("2.718282");
            assertThat(readPointValue.getNumericValue()).isCloseTo(2.718282, org.assertj.core.data.Offset.offset(0.000001));
        }

        @Test
        void negativeDouble() {
            pointWithType(PointTypeEnum.DOUBLE);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "-42.5");

            assertThat(readPointValue.getFinalValue()).isEqualTo("-42.5");
            assertThat(readPointValue.getNumericValue()).isEqualTo(-42.5);
        }

        @Test
        void zero() {
            pointWithType(PointTypeEnum.INT);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "0");

            assertThat(readPointValue.getFinalValue()).isEqualTo("0");
            assertThat(readPointValue.getNumericValue()).isEqualTo(0.0);
        }
    }

    @Nested
    class BooleanType {

        @Test
        void trueMapsToOne() {
            pointWithType(PointTypeEnum.BOOLEAN);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "true");

            assertThat(readPointValue.getFinalValue()).isEqualTo("true");
            assertThat(readPointValue.getNumericValue()).isEqualTo(1.0);
        }

        @Test
        void falseMapsToZero() {
            pointWithType(PointTypeEnum.BOOLEAN);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "false");

            assertThat(readPointValue.getFinalValue()).isEqualTo("false");
            assertThat(readPointValue.getNumericValue()).isEqualTo(0.0);
        }

        @Test
        void oneMapsToTrue() {
            pointWithType(PointTypeEnum.BOOLEAN);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "1");

            assertThat(readPointValue.getFinalValue()).isEqualTo("true");
            assertThat(readPointValue.getNumericValue()).isEqualTo(1.0);
        }

        @Test
        void invalidBooleanThrowsTypeException() {
            pointWithType(PointTypeEnum.BOOLEAN);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "yes");

            assertThatThrownBy(readPointValue::getFinalValue).isInstanceOf(TypeException.class);
        }
    }

    @Nested
    class Scaling {

        @Test
        void offsetApplied() {
            pointWithTypeAndScale(PointTypeEnum.INT, new BigDecimal("10"), BigDecimal.ONE, (byte) 0);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "5");

            assertThat(readPointValue.getFinalValue()).isEqualTo("15");
            assertThat(readPointValue.getNumericValue()).isEqualTo(15.0);
        }

        @Test
        void multiplierApplied() {
            pointWithTypeAndScale(PointTypeEnum.DOUBLE, BigDecimal.ZERO, new BigDecimal("2.5"), (byte) 2);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "10");

            assertThat(readPointValue.getFinalValue()).isEqualTo("25.0");
            assertThat(readPointValue.getNumericValue()).isEqualTo(25.0);
        }

        @Test
        void linearTransformApplied() {
            pointWithTypeAndScale(PointTypeEnum.DOUBLE, new BigDecimal("32"), new BigDecimal("1.8"), (byte) 2);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "100");

            assertThat(readPointValue.getFinalValue()).isEqualTo("212.0");
            assertThat(readPointValue.getNumericValue()).isEqualTo(212.0);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void nullPointThrowsEmptyException() {
            ReadPointValue readPointValue = ReadPointValue.builder().device(device).point(null).value("42").build();

            assertThatThrownBy(readPointValue::getFinalValue).isInstanceOf(EmptyException.class);
        }

        @Test
        void outOfRangeByteThrows() {
            pointWithType(PointTypeEnum.BYTE);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "200");

            assertThatThrownBy(readPointValue::getFinalValue).isInstanceOf(OutRangeException.class);
        }

        @Test
        void outOfRangeShortThrows() {
            pointWithType(PointTypeEnum.SHORT);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "100000");

            assertThatThrownBy(readPointValue::getFinalValue).isInstanceOf(OutRangeException.class);
        }

        @Test
        void invalidNumberForIntThrows() {
            pointWithType(PointTypeEnum.INT);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "not-a-number");

            assertThatThrownBy(readPointValue::getFinalValue).isInstanceOf(TypeException.class);
        }
    }

    @Nested
    class NumericValueCalculation {

        @Test
        void numericValueIsAvailableBeforeGetFinalValue() {
            pointWithType(PointTypeEnum.DOUBLE);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "3.14");

            assertThat(readPointValue.getNumericValue()).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001));
            readPointValue.getFinalValue();
            assertThat(readPointValue.getNumericValue()).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        void builderValueCalculatesNumericValueWithoutCallOrderDependency() {
            pointWithType(PointTypeEnum.DOUBLE);
            ReadPointValue readPointValue = ReadPointValue.builder().device(device).point(point).value("3.14").build();

            assertThat(readPointValue.getNumericValue()).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001));
            assertThat(readPointValue.getFinalValue()).isEqualTo("3.14");
        }

        @Test
        void calculateReturnsBothValuesAtOnce() {
            pointWithType(PointTypeEnum.INT);
            ReadPointValue readPointValue = new ReadPointValue(device, point, "7");

            CalculatedPointValue calculated = readPointValue.calculate();

            assertThat(calculated.getFinalValue()).isEqualTo("7");
            assertThat(calculated.getNumericValue()).isEqualTo(7.0);
        }
    }
}

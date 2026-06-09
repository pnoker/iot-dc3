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

import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.OutRangeException;
import io.github.pnoker.common.exception.TypeException;
import io.github.pnoker.common.exception.UnSupportException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for point write value conversion.
 */
class WritePointValueTest {

    @Test
    void convertsExactIntegerTypes() {
        assertThat(writePointValue("12", PointTypeEnum.BYTE).getValue(Byte.class)).isEqualTo((byte) 12);
        assertThat(writePointValue("1024", PointTypeEnum.SHORT).getValue(Short.class)).isEqualTo((short) 1024);
        assertThat(writePointValue("42.0", PointTypeEnum.INT).getValue(Integer.class)).isEqualTo(42);
        assertThat(writePointValue("9999999999", PointTypeEnum.LONG).getValue(Long.class)).isEqualTo(9999999999L);
    }

    @Test
    void acceptsPrimitiveTargetClasses() {
        assertThat(writePointValue("42", PointTypeEnum.INT).getValue(Integer.TYPE)).isEqualTo(42);
        assertThat(writePointValue("true", PointTypeEnum.BOOLEAN).getValue(Boolean.TYPE)).isEqualTo(true);
    }

    @Test
    void convertsFloatingPointTypes() {
        assertThat(writePointValue("3.14", PointTypeEnum.FLOAT).getValue(Float.class)).isEqualTo(3.14f);
        assertThat(writePointValue("2.718281828", PointTypeEnum.DOUBLE).getValue(Double.class)).isEqualTo(2.718281828d);
    }

    @Test
    void acceptsEmptyStringForStringPoint() {
        assertThat(writePointValue("", PointTypeEnum.STRING).getValue(String.class)).isEmpty();
    }

    @Test
    void strictBooleanSupportsTrueFalseAndOneZero() {
        assertThat(writePointValue("true", PointTypeEnum.BOOLEAN).getValue(Boolean.class)).isTrue();
        assertThat(writePointValue("0", PointTypeEnum.BOOLEAN).getValue(Boolean.class)).isFalse();
    }

    @Test
    void invalidBooleanThrowsTypeException() {
        assertThatThrownBy(() -> writePointValue("yes", PointTypeEnum.BOOLEAN).getValue(Boolean.class))
                .isInstanceOf(TypeException.class);
    }

    @Test
    void wrongTargetTypeThrowsTypeException() {
        assertThatThrownBy(() -> writePointValue("1", PointTypeEnum.INT).getValue(Long.class))
                .isInstanceOf(TypeException.class);
    }

    @Test
    void emptyNumericValueThrowsEmptyException() {
        assertThatThrownBy(() -> writePointValue("", PointTypeEnum.INT).getValue(Integer.class))
                .isInstanceOf(EmptyException.class);
    }

    @Test
    void invalidNumberThrowsTypeException() {
        assertThatThrownBy(() -> writePointValue("abc", PointTypeEnum.INT).getValue(Integer.class))
                .isInstanceOf(TypeException.class);
    }

    @Test
    void outOfRangeThrowsOutRangeException() {
        assertThatThrownBy(() -> writePointValue("200", PointTypeEnum.BYTE).getValue(Byte.class))
                .isInstanceOf(OutRangeException.class);
    }

    @Test
    void nullTypeThrowsUnsupportedException() {
        assertThatThrownBy(() -> writePointValue("1", null).getValue(Integer.class))
                .isInstanceOf(UnSupportException.class);
    }

    private WritePointValue writePointValue(String value, PointTypeEnum type) {
        return WritePointValue.builder().value(value).type(type).build();
    }

}

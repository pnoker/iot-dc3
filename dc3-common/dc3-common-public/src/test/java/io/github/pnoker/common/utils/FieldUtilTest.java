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

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldUtilTest {

    static class Sample implements Serializable {
        private String name;
        private Long id;

        public String getName() {
            return name;
        }

        public Long getId() {
            return id;
        }
    }

    @Test
    void getFieldExtractsLambdaPropertyName() {
        SFunction<Sample, ?> nameAccessor = Sample::getName;
        assertThat(FieldUtil.getField(nameAccessor)).isEqualTo("name");
    }

    @Test
    void getFieldHandlesIdProperty() {
        SFunction<Sample, ?> idAccessor = Sample::getId;
        assertThat(FieldUtil.getField(idAccessor)).isEqualTo("id");
    }

    @Test
    void isValidIdFieldAcceptsPositive() {
        assertThat(FieldUtil.isValidIdField(1L)).isTrue();
    }

    @Test
    void isValidIdFieldRejectsNullOrZeroOrNegative() {
        assertThat(FieldUtil.isValidIdField(null)).isFalse();
        assertThat(FieldUtil.isValidIdField(0L)).isFalse();
        assertThat(FieldUtil.isValidIdField(-1L)).isFalse();
    }

    @Test
    void isValidEnumIndexFieldAcceptsPositive() {
        assertThat(FieldUtil.isValidEnumIndexField((byte) 1)).isTrue();
    }

    @Test
    void isValidEnumIndexFieldRejectsNullOrZeroOrNegative() {
        assertThat(FieldUtil.isValidEnumIndexField(null)).isFalse();
        assertThat(FieldUtil.isValidEnumIndexField((byte) 0)).isFalse();
        assertThat(FieldUtil.isValidEnumIndexField((byte) -1)).isFalse();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<FieldUtil> constructor = FieldUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}

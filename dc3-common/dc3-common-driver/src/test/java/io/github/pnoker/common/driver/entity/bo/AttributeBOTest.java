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
package io.github.pnoker.common.driver.entity.bo;

import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.exception.TypeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for driver and point attribute value conversion.
 */
class AttributeBOTest {

    @Test
    void convertsConfiguredAttributeTypes() {
        assertThat(attribute("driver-1", AttributeTypeFlagEnum.STRING).getValue(String.class)).isEqualTo("driver-1");
        assertThat(attribute("9400", AttributeTypeFlagEnum.INT).getValue(Integer.class)).isEqualTo(9400);
        assertThat(attribute("true", AttributeTypeFlagEnum.BOOLEAN).getValue(Boolean.class)).isTrue();
    }

    @Test
    void acceptsPrimitiveTargetClasses() {
        assertThat(attribute("9400", AttributeTypeFlagEnum.INT).getValue(Integer.TYPE)).isEqualTo(9400);
        assertThat(attribute("1", AttributeTypeFlagEnum.BOOLEAN).getValue(Boolean.TYPE)).isEqualTo(true);
    }

    @Test
    void strictBooleanRejectsAmbiguousValues() {
        assertThatThrownBy(() -> attribute("enabled", AttributeTypeFlagEnum.BOOLEAN).getValue(Boolean.class))
                .isInstanceOf(TypeException.class);
    }

    @Test
    void emptyStringAttributeIsAllowedForStringType() {
        assertThat(attribute("", AttributeTypeFlagEnum.STRING).getValue(String.class)).isEmpty();
    }

    private AttributeBO attribute(String value, AttributeTypeFlagEnum type) {
        return AttributeBO.builder().value(value).type(type).build();
    }

}

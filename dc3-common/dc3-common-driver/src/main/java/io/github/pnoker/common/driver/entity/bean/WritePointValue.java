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

import io.github.pnoker.common.driver.support.TypedValueConverter;
import io.github.pnoker.common.enums.PointTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Typed point value to be written to a device.
 *
 * @author pnoker
 * @version 2026.5.15
 * @since 2026.5.15
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class WritePointValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Raw value content stored as a string before type conversion.
     */
    private String value;

    /**
     * Declared point type used to convert the raw string value.
     */
    private PointTypeEnum type;

    /**
     * Converts the stored string value to the requested Java type.
     *
     * @param clazz target type
     * @param <T>   target type parameter
     * @return converted value
     */
    public <T> T getValue(Class<T> clazz) {
        return TypedValueConverter.convertPointValue(value, type, clazz);
    }

}

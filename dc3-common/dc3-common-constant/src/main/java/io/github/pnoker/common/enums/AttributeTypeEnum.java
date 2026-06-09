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

package io.github.pnoker.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration of attribute type flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum AttributeTypeEnum {

    /**
     * String
     */
    STRING((byte) 0, "string", "String"),

    /**
     * Byte
     */
    BYTE((byte) 1, "byte", "Byte"),

    /**
     * Short
     */
    SHORT((byte) 2, "short", "Short"),

    /**
     * Int
     */
    INT((byte) 3, "int", "Int"),

    /**
     * Long
     */
    LONG((byte) 4, "long", "Long"),

    /**
     * Float
     */
    FLOAT((byte) 5, "float", "Float"),

    /**
     * Double
     */
    DOUBLE((byte) 6, "double", "Double"),

    /**
     * Boolean
     */
    BOOLEAN((byte) 7, "boolean", "Boolean"),
    ;

    /**
     * Index
     */
    @EnumValue
    private final Byte index;

    /**
     * Code
     */
    private final String code;

    /**
     * Remark
     */
    private final String remark;

    /**
     * Get enum by index
     *
     * @param index Index
     * @return {@link AttributeTypeEnum}
     */
    public static AttributeTypeEnum ofIndex(Byte index) {
        Optional<AttributeTypeEnum> any = Arrays.stream(AttributeTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link AttributeTypeEnum}
     */
    public static AttributeTypeEnum ofCode(String code) {
        Optional<AttributeTypeEnum> any = Arrays.stream(AttributeTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Name
     * @return {@link AttributeTypeEnum}
     */
    public static AttributeTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

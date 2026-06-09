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
 * Enumeration of expiration flag values (enabled/disabled).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum ExpireTypeEnum {

    /**
     * Permanent
     */
    PERMANENT((byte) 0, "permanent", "Permanent"),

    /**
     * One day
     */
    ONE_DAY((byte) 1, "one-day", "One day"),

    /**
     * One week
     */
    ONE_WEEK((byte) 2, "one-week", "One week"),

    /**
     * One month
     */
    ONE_MONTH((byte) 3, "one-month", "One month"),

    /**
     * Three months
     */
    THREE_MONTHS((byte) 4, "three-months", "Three months"),

    /**
     * Half year
     */
    HALF_YEAR((byte) 5, "half-year", "Half year"),

    /**
     * One year
     */
    ONE_YEAR((byte) 6, "one-year", "One year"),

    /**
     * Custom
     */
    CUSTOM((byte) 7, "custom", "Custom"),
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
     * @return {@link ExpireTypeEnum}
     */
    public static ExpireTypeEnum ofIndex(Byte index) {
        Optional<ExpireTypeEnum> any = Arrays.stream(ExpireTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link ExpireTypeEnum}
     */
    public static ExpireTypeEnum ofCode(String code) {
        Optional<ExpireTypeEnum> any = Arrays.stream(ExpireTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Enum name
     * @return {@link ExpireTypeEnum}
     */
    public static ExpireTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

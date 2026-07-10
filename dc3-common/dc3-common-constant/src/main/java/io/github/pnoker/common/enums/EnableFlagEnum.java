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
 * Enumeration of enable/disable flag values.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum EnableFlagEnum {

    /**
     * Enabled.
     */
    ENABLE((byte) 0, "enable", "Enabled"),

    /**
     * Disabled.
     */
    DISABLE((byte) 1, "disable", "Disabled"),
    ;

    /**
     * Index value stored in database.
     */
    @EnumValue
    private final Byte index;

    /**
     * Code string.
     */
    private final String code;

    /**
     * Human-readable description.
     */
    private final String remark;

    /**
     * Get enum by index value.
     *
     * @param index index value
     * @return {@link EnableFlagEnum} or {@code null} if not found
     */
    public static EnableFlagEnum ofIndex(Byte index) {
        Optional<EnableFlagEnum> any = Arrays.stream(EnableFlagEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code string.
     *
     * @param code code string
     * @return {@link EnableFlagEnum} or {@code null} if not found
     */
    public static EnableFlagEnum ofCode(String code) {
        Optional<EnableFlagEnum> any = Arrays.stream(EnableFlagEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by enum name.
     *
     * @param name enum name
     * @return {@link EnableFlagEnum} or {@code null} if parsing fails
     */
    public static EnableFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}

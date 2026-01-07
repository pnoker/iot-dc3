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
 * Common alarm level enumeration
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum AlarmMessageLevelFlagEnum {
    /**
     * P0
     */
    P0((byte) 0, "p0", "P0"),

    /**
     * P1
     */
    P1((byte) 1, "p1", "P1"),

    /**
     * P2
     */
    P2((byte) 2, "p2", "P2"),

    /**
     * P3
     */
    P3((byte) 3, "p3", "P3"),
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
     * @return {@link AlarmMessageLevelFlagEnum}
     */
    public static AlarmMessageLevelFlagEnum ofIndex(Byte index) {
        Optional<AlarmMessageLevelFlagEnum> any = Arrays.stream(AlarmMessageLevelFlagEnum.values()).filter(type -> type.getIndex().equals(index)).findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link AlarmMessageLevelFlagEnum}
     */
    public static AlarmMessageLevelFlagEnum ofCode(String code) {
        Optional<AlarmMessageLevelFlagEnum> any = Arrays.stream(AlarmMessageLevelFlagEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Name
     * @return {@link AlarmMessageLevelFlagEnum}
     */
    public static AlarmMessageLevelFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

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
 * Enumeration of read/write flag values.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum RwFlagEnum {

    /**
     * Read only
     */
    READ_ONLY((byte) 0, "r", "Read only"),

    /**
     * Write only
     */
    WRITE_ONLY((byte) 1, "w", "Write only"),

    /**
     * Read and Write
     */
    READ_WRITE((byte) 2, "rw", "Read and Write"),
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
     * @return {@link RwFlagEnum}
     */
    public static RwFlagEnum ofIndex(Byte index) {
        Optional<RwFlagEnum> any = Arrays.stream(RwFlagEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link RwFlagEnum}
     */
    public static RwFlagEnum ofCode(String code) {
        Optional<RwFlagEnum> any = Arrays.stream(RwFlagEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Name
     * @return {@link RwFlagEnum}
     */
    public static RwFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

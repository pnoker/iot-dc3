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
 * Enumeration of permission scope type flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum ResourceScopeTypeEnum {

    /**
     * Add
     */
    ADD((byte) 0, "add", "Add"),

    /**
     * Delete
     */
    DELETE((byte) 1, "delete", "Delete"),

    /**
     * Update
     */
    UPDATE((byte) 2, "update", "Update"),

    /**
     * List
     */
    LIST((byte) 3, "list", "List"),

    /**
     * Get (single entity query)
     */
    GET((byte) 4, "get", "Get"),
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
     * @return {@link ResourceScopeTypeEnum}
     */
    public static ResourceScopeTypeEnum ofIndex(Byte index) {
        Optional<ResourceScopeTypeEnum> any = Arrays.stream(ResourceScopeTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link ResourceScopeTypeEnum}
     */
    public static ResourceScopeTypeEnum ofCode(String code) {
        Optional<ResourceScopeTypeEnum> any = Arrays.stream(ResourceScopeTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Name
     * @return {@link ResourceScopeTypeEnum}
     */
    public static ResourceScopeTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

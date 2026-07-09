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
 * Enumeration of resource type flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum ResourceTypeEnum {

    /**
     * Driver
     */
    DRIVER((byte) 0, "driver", "Driver"),

    /**
     * Profile
     */
    PROFILE((byte) 1, "profile", "Profile"),

    /**
     * Point
     */
    POINT((byte) 2, "point", "Point"),

    /**
     * Device
     */
    DEVICE((byte) 3, "device", "Device"),

    /**
     * Data
     */
    DATA((byte) 4, "data", "Data"),

    /**
     * Menu
     */
    MENU((byte) 5, "menu", "Menu"),

    /**
     * API
     */
    API((byte) 6, "api", "API"),
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
     * @return {@link ResourceTypeEnum}
     */
    public static ResourceTypeEnum ofIndex(Byte index) {
        Optional<ResourceTypeEnum> any = Arrays.stream(ResourceTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link ResourceTypeEnum}
     */
    public static ResourceTypeEnum ofCode(String code) {
        Optional<ResourceTypeEnum> any = Arrays.stream(ResourceTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Name
     * @return {@link ResourceTypeEnum}
     */
    public static ResourceTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}

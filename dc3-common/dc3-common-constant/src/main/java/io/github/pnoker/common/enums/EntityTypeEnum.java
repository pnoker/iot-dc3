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
 * Enumeration of entity type flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum EntityTypeEnum {

    /**
     * System
     */
    SYSTEM((byte) 0, "system", "System"),

    /**
     * User
     */
    USER((byte) 1, "user", "User"),

    /**
     * Group
     */
    GROUP((byte) 2, "group", "Group"),

    /**
     * Driver
     */
    DRIVER((byte) 3, "driver", "Driver"),

    /**
     * Profile
     */
    PROFILE((byte) 4, "profile", "Profile"),

    /**
     * Point
     */
    POINT((byte) 5, "point", "Point"),

    /**
     * Device
     */
    DEVICE((byte) 6, "device", "Device"),

    /**
     * Command
     */
    COMMAND((byte) 7, "command", "Command"),

    /**
     * Event
     */
    EVENT((byte) 8, "event", "Event"),
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
     * @return {@link EntityTypeEnum}
     */
    public static EntityTypeEnum ofIndex(Byte index) {
        Optional<EntityTypeEnum> any = Arrays.stream(EntityTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link EntityTypeEnum}
     */
    public static EntityTypeEnum ofCode(String code) {
        Optional<EntityTypeEnum> any = Arrays.stream(EntityTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Enum name
     * @return {@link EntityTypeEnum}
     */
    public static EntityTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}

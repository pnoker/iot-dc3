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
 * Enumeration of event type flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum EventTypeFlagEnum {

    /**
     * Info event
     */
    INFO((byte) 0, "info", "Info event"),

    /**
     * Alert event
     */
    ALERT((byte) 1, "alert", "Alert event"),

    /**
     * Fault event
     */
    FAULT((byte) 2, "fault", "Fault event"),

    /**
     * Lifecycle event
     */
    LIFECYCLE((byte) 3, "lifecycle", "Lifecycle event"),
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
     * @return {@link EventTypeFlagEnum}
     */
    public static EventTypeFlagEnum ofIndex(Byte index) {
        Optional<EventTypeFlagEnum> any = Arrays.stream(EventTypeFlagEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link EventTypeFlagEnum}
     */
    public static EventTypeFlagEnum ofCode(String code) {
        Optional<EventTypeFlagEnum> any = Arrays.stream(EventTypeFlagEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Name
     * @return {@link EventTypeFlagEnum}
     */
    public static EventTypeFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}

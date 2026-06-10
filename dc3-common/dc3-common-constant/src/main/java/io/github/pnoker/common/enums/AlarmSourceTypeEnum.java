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
 * Enumeration of alarm source flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum AlarmSourceTypeEnum {

    /**
     * Rule engine
     */
    RULE((byte) 0, "rule", "Rule engine"),

    /**
     * State timeout
     */
    STATE_TIMEOUT((byte) 1, "state-timeout", "State timeout"),

    /**
     * Device report
     */
    DEVICE_REPORT((byte) 2, "device-report", "Device report"),

    /**
     * Driver report
     */
    DRIVER_REPORT((byte) 3, "driver-report", "Driver report"),

    /**
     * Event report
     *
     * <p>Index 5 is kept for persisted value compatibility.
     */
    EVENT_REPORT((byte) 5, "event-report", "Event report"),

    /**
     * System
     */
    SYSTEM((byte) 4, "system", "System"),
    ;

    @EnumValue
    private final Byte index;

    private final String code;

    private final String remark;

    public static AlarmSourceTypeEnum ofIndex(Byte index) {
        Optional<AlarmSourceTypeEnum> any = Arrays.stream(AlarmSourceTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    public static AlarmSourceTypeEnum ofCode(String code) {
        Optional<AlarmSourceTypeEnum> any = Arrays.stream(AlarmSourceTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    public static AlarmSourceTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

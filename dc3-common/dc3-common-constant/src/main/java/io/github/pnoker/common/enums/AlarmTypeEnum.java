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
 * Enumeration of alarm type flags.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum AlarmTypeEnum {

    /**
     * Rule alarm
     */
    RULE((byte) 0, "rule", "Rule alarm"),

    /**
     * Offline alarm
     */
    OFFLINE((byte) 1, "offline", "Offline alarm"),

    /**
     * Fault alarm
     */
    FAULT((byte) 2, "fault", "Fault alarm"),

    /**
     * State flip alarm
     */
    STATE_FLIP((byte) 3, "state-flip", "State flip alarm"),

    /**
     * Report alarm
     */
    REPORT((byte) 4, "report", "Report alarm"),
    ;

    @EnumValue
    private final Byte index;

    private final String code;

    private final String remark;

    public static AlarmTypeEnum ofIndex(Byte index) {
        Optional<AlarmTypeEnum> any = Arrays.stream(AlarmTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    public static AlarmTypeEnum ofCode(String code) {
        Optional<AlarmTypeEnum> any = Arrays.stream(AlarmTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    public static AlarmTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}

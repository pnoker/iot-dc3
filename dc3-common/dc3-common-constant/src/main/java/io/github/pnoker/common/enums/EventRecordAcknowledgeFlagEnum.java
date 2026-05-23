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
 * Enumeration of event record acknowledge flags.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Getter
@AllArgsConstructor
public enum EventRecordAcknowledgeFlagEnum {

    /**
     * Not acknowledged
     */
    NO((byte) 0, "no", "Not acknowledged"),

    /**
     * Acknowledged
     */
    YES((byte) 1, "yes", "Acknowledged"),
    ;

    @EnumValue
    private final Byte index;

    private final String code;

    private final String remark;

    public static EventRecordAcknowledgeFlagEnum ofIndex(Byte index) {
        Optional<EventRecordAcknowledgeFlagEnum> any = Arrays.stream(EventRecordAcknowledgeFlagEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    public static EventRecordAcknowledgeFlagEnum ofCode(String code) {
        Optional<EventRecordAcknowledgeFlagEnum> any = Arrays.stream(EventRecordAcknowledgeFlagEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    public static EventRecordAcknowledgeFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

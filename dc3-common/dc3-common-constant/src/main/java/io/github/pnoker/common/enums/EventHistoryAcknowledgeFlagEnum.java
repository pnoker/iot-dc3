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

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration of event record acknowledge flags.
 *
 * @author pnoker
 * @since 2026.5.23
 */
@Getter
@AllArgsConstructor
public enum EventHistoryAcknowledgeFlagEnum {

    /**
     * Not acknowledged
     */
    NO((byte) 0, "no", "Not acknowledged"),

    /**
     * Acknowledged
     */
    YES((byte) 1, "yes", "Acknowledged"),
    ;

    private final Byte index;

    private final String code;

    private final String remark;

    /**
     * Resolve an acknowledgement flag from its persisted numeric index.
     *
     * @param index persisted index
     * @return matching flag, or {@code null} when the index is unknown
     */
    public static EventHistoryAcknowledgeFlagEnum ofIndex(Byte index) {
        Optional<EventHistoryAcknowledgeFlagEnum> any = Arrays.stream(EventHistoryAcknowledgeFlagEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Resolve an acknowledgement flag from its stable wire-format code.
     *
     * @param code wire-format code
     * @return matching flag, or {@code null} when the code is unknown
     */
    public static EventHistoryAcknowledgeFlagEnum ofCode(String code) {
        Optional<EventHistoryAcknowledgeFlagEnum> any = Arrays.stream(EventHistoryAcknowledgeFlagEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Resolve an acknowledgement flag from its Java enum constant name.
     *
     * @param name enum constant name
     * @return matching flag, or {@code null} when the name is unknown
     */
    public static EventHistoryAcknowledgeFlagEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

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
 * Rule runtime state enumeration.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum RuleStatusEnum {

    /** Normal. */
    NORMAL((byte) 0, "normal", "Normal"),

    /** Firing. */
    FIRING((byte) 1, "firing", "Firing"),

    /** Recovered. */
    RECOVERED((byte) 2, "recovered", "Recovered"),
    ;

    @EnumValue
    private final Byte index;

    private final String code;

    private final String remark;

    /**
     * Resolve a rule status from its persisted numeric index.
     *
     * @param index persisted index
     * @return matching status, or {@code null} when the index is unknown
     */
    public static RuleStatusEnum ofIndex(Byte index) {
        Optional<RuleStatusEnum> any = Arrays.stream(RuleStatusEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Resolve a rule status from its stable wire-format code.
     *
     * @param code wire-format code
     * @return matching status, or {@code null} when the code is unknown
     */
    public static RuleStatusEnum ofCode(String code) {
        Optional<RuleStatusEnum> any = Arrays.stream(RuleStatusEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Resolve a rule status from its Java enum constant name.
     *
     * @param name enum constant name
     * @return matching status, or {@code null} when the name is unknown
     */
    public static RuleStatusEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}

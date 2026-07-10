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
 * Agentic message persistence status.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Getter
@AllArgsConstructor
public enum AgenticMessageStatusEnum {

    /**
     * Message saved successfully.
     */
    OK((byte) 0, "ok", "Message saved successfully"),
    ;

    /**
     * Index value stored in database.
     */
    @EnumValue
    private final Byte index;

    /**
     * Code string.
     */
    private final String code;

    /**
     * Human-readable description.
     */
    private final String remark;

    /**
     * Get enum by index value.
     *
     * @param index index value
     * @return {@link AgenticMessageStatusEnum} or {@code null} if not found
     */
    public static AgenticMessageStatusEnum ofIndex(Byte index) {
        Optional<AgenticMessageStatusEnum> any = Arrays.stream(AgenticMessageStatusEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code.
     *
     * @param code code
     * @return {@link AgenticMessageStatusEnum} or {@code null} if not found
     */
    public static AgenticMessageStatusEnum ofCode(String code) {
        Optional<AgenticMessageStatusEnum> any = Arrays.stream(AgenticMessageStatusEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name.
     *
     * @param name name
     * @return {@link AgenticMessageStatusEnum} or {@code null} if not found
     */
    public static AgenticMessageStatusEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}

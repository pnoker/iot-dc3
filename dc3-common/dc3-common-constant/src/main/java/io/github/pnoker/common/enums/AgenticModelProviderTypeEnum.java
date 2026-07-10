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
 * Agentic model provider type.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Getter
@AllArgsConstructor
public enum AgenticModelProviderTypeEnum {

    /**
     * OpenAI-compatible API provider.
     */
    OPENAI_COMPATIBLE((byte) 0, "openai-compatible", "OpenAI-compatible API provider"),

    /**
     * Anthropic API provider.
     */
    ANTHROPIC((byte) 1, "anthropic", "Anthropic API provider"),
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
     * @return {@link AgenticModelProviderTypeEnum} or {@code null} if not found
     */
    public static AgenticModelProviderTypeEnum ofIndex(Byte index) {
        Optional<AgenticModelProviderTypeEnum> any = Arrays.stream(AgenticModelProviderTypeEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code string.
     *
     * @param code code string
     * @return {@link AgenticModelProviderTypeEnum} or {@code null} if not found
     */
    public static AgenticModelProviderTypeEnum ofCode(String code) {
        Optional<AgenticModelProviderTypeEnum> any = Arrays.stream(AgenticModelProviderTypeEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by enum name.
     *
     * @param name enum name
     * @return {@link AgenticModelProviderTypeEnum} or {@code null} if parsing fails
     */
    public static AgenticModelProviderTypeEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

}

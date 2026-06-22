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
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * MCP tool-call audit outcome.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@AllArgsConstructor
public enum McpAuditStatusEnum {

    /**
     * Tool call succeeded.
     */
    SUCCESS("SUCCESS", "success", "Tool call succeeded"),

    /**
     * Caller denied by authorization.
     */
    DENIED("DENIED", "denied", "Denied by authorization"),

    /**
     * Denied by risk/confirmation policy.
     */
    POLICY_DENIED("POLICY_DENIED", "policy_denied", "Denied by policy"),

    /**
     * Tool call errored.
     */
    ERROR("ERROR", "error", "Tool call errored"),

    /**
     * Outcome unknown.
     */
    UNKNOWN("UNKNOWN", "unknown", "Unknown outcome"),
    ;

    /**
     * Database / wire value.
     */
    @EnumValue
    @JsonValue
    private final String value;

    /**
     * Code string.
     */
    private final String code;

    /**
     * Human-readable description.
     */
    private final String remark;

    public static McpAuditStatusEnum ofValue(String value) {
        Optional<McpAuditStatusEnum> any = Arrays.stream(values())
                .filter(type -> type.getValue().equals(value))
                .findFirst();
        return any.orElse(null);
    }

}

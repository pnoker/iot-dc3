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

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * OAuth registered client type.
 *
 * @author pnoker
 * @since 2026.6.12
 */
@Getter
@AllArgsConstructor
public enum OAuthClientTypeEnum {

    /**
     * Public client (no secret, PKCE required).
     */
    PUBLIC("PUBLIC", "public", "Public client"),

    /**
     * Confidential client (holds a secret).
     */
    CONFIDENTIAL("CONFIDENTIAL", "confidential", "Confidential client"),
    ;

    /**
     * Database / wire value.
     */
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

    /**
     * Resolve an OAuth client type from its persisted wire value.
     *
     * @param value persisted client-type value
     * @return matching type, or {@code null} when the value is unknown
     */
    public static OAuthClientTypeEnum ofValue(String value) {
        Optional<OAuthClientTypeEnum> any = Arrays.stream(values())
                .filter(type -> type.getValue().equals(value))
                .findFirst();
        return any.orElse(null);
    }

}

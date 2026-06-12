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
 * Principal classification for authenticated and authorized callers.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@AllArgsConstructor
public enum PrincipalTypeEnum {

    /**
     * Human user.
     */
    USER("USER", "user", "Human user"),

    /**
     * Machine or automation identity.
     */
    SERVICE_ACCOUNT("SERVICE_ACCOUNT", "service_account", "Service account"),

    /**
     * Internal system identity.
     */
    SYSTEM("SYSTEM", "system", "System principal"),
    ;

    /**
     * Database value.
     */
    @EnumValue
    private final String value;

    /**
     * Code string.
     */
    private final String code;

    /**
     * Human-readable description.
     */
    private final String remark;

    public static PrincipalTypeEnum ofValue(String value) {
        Optional<PrincipalTypeEnum> any = Arrays.stream(values())
                .filter(type -> type.getValue().equals(value))
                .findFirst();
        return any.orElse(null);
    }

    public static PrincipalTypeEnum ofCode(String code) {
        Optional<PrincipalTypeEnum> any = Arrays.stream(values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

}

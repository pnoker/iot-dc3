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
 * Connectivity check probe level. L1 lists models (zero token cost), L2 sends
 * a minimal real chat completion (end-to-end verdict), BOTH runs L1 then L2.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum ConnectivityCheckLevelEnum {

    /**
     * Model-list probe only: validates DNS/TLS/routing/key and returns the
     * available model ids.
     */
    L1((byte) 0, "l1", "Model-list probe only"),

    /**
     * Minimal chat probe only: validates the full inference chain for one model.
     */
    L2((byte) 1, "l2", "Minimal chat probe only"),

    /**
     * Run L1 then L2 (default).
     */
    BOTH((byte) 2, "both", "Run both probes"),
    ;

    /**
     * Index value stored in the request/response contract.
     */
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
     * @return {@link ConnectivityCheckLevelEnum} or {@code null} if not found
     */
    public static ConnectivityCheckLevelEnum ofIndex(Byte index) {
        Optional<ConnectivityCheckLevelEnum> any = Arrays.stream(ConnectivityCheckLevelEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code string.
     *
     * @param code code string
     * @return {@link ConnectivityCheckLevelEnum} or {@code null} if not found
     */
    public static ConnectivityCheckLevelEnum ofCode(String code) {
        Optional<ConnectivityCheckLevelEnum> any = Arrays.stream(ConnectivityCheckLevelEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by enum name.
     *
     * @param name enum name
     * @return {@link ConnectivityCheckLevelEnum} or {@code null} if parsing fails
     */
    public static ConnectivityCheckLevelEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

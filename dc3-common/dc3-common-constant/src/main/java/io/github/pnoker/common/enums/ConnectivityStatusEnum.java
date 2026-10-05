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
 * Connectivity check status. Only PASS/FAIL are persisted in
 * {@code last_check_status} (NULL = never checked); SKIPPED is a
 * response-only status for probe levels that were not requested.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@AllArgsConstructor
public enum ConnectivityStatusEnum {

    /**
     * Probe passed.
     */
    PASS((byte) 1, "pass", "Probe passed"),

    /**
     * Probe failed; see the error type for the diagnosis.
     */
    FAIL((byte) 2, "fail", "Probe failed"),

    /**
     * Probe level not requested in this check (response-only, never persisted).
     */
    SKIPPED((byte) 3, "skipped", "Probe level skipped"),
    ;

    /**
     * Index value stored in database ({@code dc3_model_provider.last_check_status}
     * and {@code dc3_model_config.last_check_status}).
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
     * @return {@link ConnectivityStatusEnum} or {@code null} if not found
     */
    public static ConnectivityStatusEnum ofIndex(Byte index) {
        Optional<ConnectivityStatusEnum> any = Arrays.stream(ConnectivityStatusEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code string.
     *
     * @param code code string
     * @return {@link ConnectivityStatusEnum} or {@code null} if not found
     */
    public static ConnectivityStatusEnum ofCode(String code) {
        Optional<ConnectivityStatusEnum> any = Arrays.stream(ConnectivityStatusEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by enum name.
     *
     * @param name enum name
     * @return {@link ConnectivityStatusEnum} or {@code null} if parsing fails
     */
    public static ConnectivityStatusEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

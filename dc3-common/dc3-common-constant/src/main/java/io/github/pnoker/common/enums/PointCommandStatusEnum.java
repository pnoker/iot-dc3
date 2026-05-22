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
 * Enumeration of point command statuses.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Getter
@AllArgsConstructor
public enum PointCommandStatusEnum {

    /**
     * Command submitted, waiting for publish
     */
    PENDING((byte) 0, "pending", "Command submitted, waiting for publish"),

    /**
     * Published to broker, waiting for driver
     */
    SENT((byte) 1, "sent", "Published to broker, waiting for driver"),

    /**
     * Driver confirmed success
     */
    SUCCESS((byte) 2, "success", "Driver confirmed success"),

    /**
     * Driver reported failure
     */
    FAILED((byte) 3, "failed", "Driver reported failure"),

    /**
     * Application-level timeout
     */
    TIMEOUT((byte) 4, "timeout", "Application-level timeout"),

    /**
     * Expire-at passed before execution
     */
    EXPIRED((byte) 5, "expired", "Expire-at passed before execution"),

    /**
     * Rejected into DLX, no further processing
     */
    DEAD((byte) 6, "dead", "Rejected into DLX, no further processing"),

    /**
     * Duplicate command rejected by driver dedup cache
     */
    DUPLICATE((byte) 7, "duplicate", "Duplicate command rejected by driver dedup cache"),
    ;

    /**
     * Index
     */
    @EnumValue
    private final Byte index;

    /**
     * Code
     */
    private final String code;

    /**
     * Remark
     */
    private final String remark;

    /**
     * Get enum by index
     *
     * @param index Index
     * @return {@link PointCommandStatusEnum}
     */
    public static PointCommandStatusEnum ofIndex(Byte index) {
        Optional<PointCommandStatusEnum> any = Arrays.stream(PointCommandStatusEnum.values())
                .filter(type -> type.getIndex().equals(index))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by code
     *
     * @param code Code
     * @return {@link PointCommandStatusEnum}
     */
    public static PointCommandStatusEnum ofCode(String code) {
        Optional<PointCommandStatusEnum> any = Arrays.stream(PointCommandStatusEnum.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
        return any.orElse(null);
    }

    /**
     * Get enum by name
     *
     * @param name Name
     * @return {@link PointCommandStatusEnum}
     */
    public static PointCommandStatusEnum ofName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

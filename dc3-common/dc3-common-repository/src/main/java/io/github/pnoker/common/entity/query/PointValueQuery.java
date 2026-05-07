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

package io.github.pnoker.common.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Point Value Query Object
 * <p>
 * Query object for searching and filtering point values in the repository layer. Supports
 * filtering by tenant, device, point name, and enable status. Includes pagination support
 * for large datasets.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PointValueQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * Tenant ID for multi-tenant data isolation
     */
    private Long tenantId;

    // Query fields

    /**
     * Device ID to filter by specific device
     */
    private Long deviceId;

    /**
     * Device name for text-based filtering
     */
    private String deviceName;

    /**
     * Point ID to filter by specific point
     */
    private Long pointId;

    /**
     * Point name for text-based filtering
     */
    private String pointName;

    /**
     * Enable flag to filter active/inactive points
     */
    private EnableFlagEnum enableFlag;

    /**
     * Optional lower bound for create_time filtering (time range feature).
     */
    private java.time.LocalDateTime createTimeFrom;

    /**
     * Convenience field: when set (e.g. 24, 168, 720), the service layer converts it to
     * createTimeFrom = now - rangeHours. Retained for backward compatibility; new callers
     * should set {@link #rangeKey}.
     */
    private Integer rangeHours;

    /**
     * Preferred time-range selector. Accepts one of the
     * {@link io.github.pnoker.common.enums.TimeRangeKeyEnum} codes ({@code today},
     * {@code 24h}, {@code 7d}, {@code 30d}). Takes precedence over {@link #rangeHours}
     * when both are supplied.
     */
    private String rangeKey;

}

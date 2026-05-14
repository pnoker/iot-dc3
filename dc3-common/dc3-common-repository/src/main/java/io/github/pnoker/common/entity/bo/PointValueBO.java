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

package io.github.pnoker.common.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Point Value Business Object
 * <p>
 * Business object representing point values in IoT DC3 platform. Contains device ID,
 * point ID, raw value, calculated value, driver ID, and timestamps for data storage and
 * retrieval operations.
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
public class PointValueBO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Device ID associated with the point value
     */
    private Long deviceId;

    /**
     * Point ID associated with the value
     */
    private Long pointId;

    /**
     * Raw value as received from the device
     */
    private String rawValue;

    /**
     * Calculated/processed value after transformations
     */
    private String calValue;

    /**
     * Best-effort numeric projection of {@link #calValue} populated when the
     * value parses cleanly as a double. NULL for booleans, JSON, and free-form
     * text. Aggregate queries on the history table use this column with a
     * partial index to skip the cast / parse step at query time.
     */
    private Double numericValue;

    /**
     * Driver ID that collected the data
     */
    private Long driverId;

    /**
     * Tenant ID the data belongs to
     */
    private Long tenantId;

    /**
     * Timestamp when the value was created
     */
    private LocalDateTime createTime;

    /**
     * Timestamp when the value was last operated on
     */
    private LocalDateTime operateTime;

}

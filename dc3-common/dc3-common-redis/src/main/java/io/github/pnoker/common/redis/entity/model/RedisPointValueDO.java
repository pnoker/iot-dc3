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

package io.github.pnoker.common.redis.entity.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Redis Point Value Data Object
 * <p>
 * Data object representing point values stored in Redis.
 * Contains device ID, point ID, raw and calculated values,
 * driver ID, and timestamps for time-series data storage.
 * Used for Redis persistence of IoT device point values.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
public class RedisPointValueDO implements Serializable {

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
     * Driver ID that collected the data
     */
    private Long driverId;

    /**
     * Timestamp when the value was created
     */
    private LocalDateTime createTime;

    /**
     * Timestamp when the value was last operated on
     */
    private LocalDateTime operateTime;
}

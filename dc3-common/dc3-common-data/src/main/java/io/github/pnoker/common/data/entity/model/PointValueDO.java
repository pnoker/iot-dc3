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
package io.github.pnoker.common.data.entity.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Persistence object for the dc3_point_value table.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
public class PointValueDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Immutable event identity used for idempotent inserts.
     */
    private String messageId;

    /**
     * Wire schema version.
     */
    private Integer schemaVersion;

    /**
     * Unique runtime node that produced this reading.
     */
    private String driverNode;

    /**
     * Monotonically increasing sequence within {@link #driverNode}.
     */
    private Long sequence;

    /**
     * Manager-issued device ownership fencing token.
     */
    private Long fencingToken;

    /**
     * Device ID
     */
    private Long deviceId;

    /**
     * Point ID
     */
    private Long pointId;

    /**
     * Raw value
     */
    private String rawValue;

    /**
     *
     */
    private String calValue;

    /**
     * Best-effort numeric projection of {@link #calValue} for aggregation
     * queries (AVG/MIN/MAX/SUM/timeseries). NULL for non-numeric payloads
     * (booleans, JSON, free-form text).
     */
    private Double numValue;

    /**
     * OPC-UA style quality code. Zero represents a good sample.
     */
    private Integer quality;

    /**
     * Driver ID
     */
    private Long driverId;

    /**
     * Tenant ID
     */
    private Long tenantId;

    /**
     * Create Time
     */
    private LocalDateTime createTime;

    /**
     * Operate Time
     */
    private LocalDateTime operateTime;
}

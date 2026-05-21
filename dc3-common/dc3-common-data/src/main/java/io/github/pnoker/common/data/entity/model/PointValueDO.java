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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Persistence object for the dc3_point_value table.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@TableName("dc3_point_value")
public class PointValueDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Device ID
     */
    @TableField("device_id")
    private Long deviceId;

    /**
     * Point ID
     */
    @TableField("point_id")
    private Long pointId;

    /**
     * Raw value
     */
    @TableField("raw_value")
    private String rawValue;

    /**
     *
     */
    @TableField("cal_value")
    private String calValue;

    /**
     * Best-effort numeric projection of {@link #calValue} for aggregation
     * queries (AVG/MIN/MAX/SUM/timeseries). NULL for non-numeric payloads
     * (booleans, JSON, free-form text).
     */
    @TableField("num_value")
    private Double numValue;

    /**
     * Driver ID
     */
    @TableField("driver_id")
    private Long driverId;

    /**
     * Tenant ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * Create Time
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * Operate Time
     */
    @TableField("operate_time")
    private LocalDateTime operateTime;

}

/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.postgres.entity.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 设备位号历史数据表(Float类型)
 * </p>
 *
 * @author pnoker
 * @version 2025.2.5
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
@TableName("dc3_point_value_float")
public class PointValueFloatDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    @TableField("device_id")
    private Long deviceId;

    /**
     * 位号ID
     */
    @TableField("point_id")
    private Long pointId;

    /**
     * 原始值
     */
    @TableField("raw_value")
    private Double rawValue;

    /**
     * 计算值
     */
    @TableField("cal_value")
    private Double calValue;

    /**
     * 数据信息
     */
    @TableField("value_ext")
    private Object valueExt;

    /**
     * 驱动ID
     */
    @TableField("driver_id")
    private Long driverId;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 原始时间
     */
    @TableField("origin_time")
    private LocalDateTime originTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 操作时间
     */
    @TableField("operate_time")
    private LocalDateTime operateTime;
}

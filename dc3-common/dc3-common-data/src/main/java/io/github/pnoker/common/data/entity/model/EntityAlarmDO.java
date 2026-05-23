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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.pnoker.common.entity.ext.JsonExt;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Persistence object for the dc3_entity_alarm table.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_entity_alarm", autoResultMap = true)
public class EntityAlarmDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Alarm target type flag, 0: point, 1: device, 2: driver
     */
    @TableField("alarm_target_type_flag")
    private Byte alarmTargetTypeFlag;

    /**
     * Alarm target entity ID
     */
    @TableField("entity_id")
    private Long entityId;

    /**
     * Driver ID
     */
    @TableField("driver_id")
    private Long driverId;

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
     * Rule ID
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * Rule state ID
     */
    @TableField("rule_state_id")
    private Long ruleStateId;

    /**
     * Alarm type flag, 0: rule, 1: offline, 2: fault, 3: state flip, 4: report
     */
    @TableField("alarm_type_flag")
    private Byte alarmTypeFlag;

    /**
     * Alarm source flag, 0: rule, 1: state timeout, 2: device report, 3: driver report, 4: system
     */
    @TableField("alarm_source_flag")
    private Byte alarmSourceFlag;

    /**
     * Alarm level flag, 0: P0, 1: P1, 2: P2, 3: P3
     */
    @TableField("alarm_level_flag")
    private Byte alarmLevelFlag;

    /**
     * Alarm extension information
     */
    @TableField(value = "alarm_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt alarmExt;

    /**
     * Expiration duration, seconds
     */
    @TableField("expired_time")
    private Long expiredTime;

    /**
     * Confirmation flag, 0: unconfirmed, 1: confirmed
     */
    @TableField("confirm_flag")
    private Byte confirmFlag;

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

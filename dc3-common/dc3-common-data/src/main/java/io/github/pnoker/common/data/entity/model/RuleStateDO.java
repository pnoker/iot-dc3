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
 * Persistence object for the dc3_rule_state table.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_rule_state", autoResultMap = true)
public class RuleStateDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("alarm_target_type_flag")
    private Byte alarmTargetTypeFlag;

    @TableField("entity_id")
    private Long entityId;

    @TableField("fingerprint")
    private String fingerprint;

    @TableField("entity_state_flag")
    private Byte entityStateFlag;

    @TableField("first_trigger_time")
    private LocalDateTime firstTriggerTime;

    @TableField("last_trigger_time")
    private LocalDateTime lastTriggerTime;

    @TableField("last_recover_time")
    private LocalDateTime lastRecoverTime;

    @TableField("last_notify_time")
    private LocalDateTime lastNotifyTime;

    @TableField("trigger_count")
    private Long triggerCount;

    @TableField("alarm_id")
    private Long alarmId;

    @TableField(value = "entity_state_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt entityStateExt;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("remark")
    private String remark;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("creator_name")
    private String creatorName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operate_time")
    private LocalDateTime operateTime;

}

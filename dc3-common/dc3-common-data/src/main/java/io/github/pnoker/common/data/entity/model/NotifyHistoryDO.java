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
 * Persistence object for the dc3_notify_history table.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_notify_history", autoResultMap = true)
public class NotifyHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("notify_id")
    private Long notifyId;

    @TableField("message_id")
    private Long messageId;

    @TableField("channel_id")
    private Long channelId;

    @TableField("alarm_id")
    private Long alarmId;

    @TableField("channel_type_flag")
    private Byte channelTypeFlag;

    @TableField("target")
    private String target;

    @TableField("status_flag")
    private Byte statusFlag;

    @TableField(value = "request_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt requestExt;

    @TableField(value = "response_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt responseExt;

    @TableField("error_message")
    private String errorMessage;

    @TableField("retry_count")
    private Integer retryCount;

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

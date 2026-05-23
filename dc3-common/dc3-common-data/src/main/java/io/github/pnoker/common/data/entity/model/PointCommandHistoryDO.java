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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Persistence object for the dc3_point_command_history table.
 * <p>
 * Stores point command records with lifecycle tracking from submission
 * through driver execution to terminal status.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_point_command_history")
public class PointCommandHistoryDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("command_id")
    private String commandId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("type")
    private String type;

    @TableField("device_id")
    private Long deviceId;

    @TableField("point_id")
    private Long pointId;

    @TableField("request_value")
    private String requestValue;

    @TableField("response_value")
    private String responseValue;

    @TableField("status")
    private String status;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("source")
    private String source;

    @TableField("source_user_id")
    private Long sourceUserId;

    @TableField("occur_time")
    private LocalDateTime occurTime;

    @TableField("send_time")
    private LocalDateTime sendTime;

    @TableField("finish_time")
    private LocalDateTime finishTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("schema_version")
    private Short schemaVersion;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("operate_time")
    private LocalDateTime operateTime;

}

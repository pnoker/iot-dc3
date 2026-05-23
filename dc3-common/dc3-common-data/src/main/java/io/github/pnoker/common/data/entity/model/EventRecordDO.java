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
 * Persistence object for the dc3_event_record table.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_event_record")
public class EventRecordDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("record_id")
    private String recordId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("device_id")
    private Long deviceId;

    @TableField("event_id")
    private Long eventId;

    @TableField("event_code")
    private String eventCode;

    @TableField("event_type_flag")
    private Byte eventTypeFlag;

    @TableField("event_level_flag")
    private Byte eventLevelFlag;

    @TableField("param_values")
    private String paramValues;

    @TableField("message")
    private String message;

    @TableField("occur_time")
    private LocalDateTime occurTime;

    @TableField("receive_time")
    private LocalDateTime receiveTime;

    @TableField("acknowledge_flag")
    private Byte acknowledgeFlag;

    @TableField("schema_version")
    private Short schemaVersion;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

}

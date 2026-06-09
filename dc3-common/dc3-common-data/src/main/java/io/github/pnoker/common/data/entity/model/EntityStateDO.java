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
 * Persistence object for the dc3_entity_state table.
 * <p>
 * Stores the current online-status lease for every driver and device. The expiry
 * scanner checks {@code expire_time} to detect entities whose heartbeat stopped;
 * query services read this table as the authoritative source of truth.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.21
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_entity_state", autoResultMap = true)
public class EntityStateDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Entity type flag (matches EntityTypeEnum: DRIVER=3, DEVICE=6)
     */
    @TableField("entity_type_flag")
    private Byte entityTypeFlag;

    /**
     * Entity ID (driver ID or device ID)
     */
    @TableField("entity_id")
    private Long entityId;

    /**
     * Parent entity ID. For driver entries this is 0; for device entries the owning driver.
     */
    @TableField("parent_entity_id")
    private Long parentEntityId;

    /**
     * Current status index ({@link io.github.pnoker.common.enums.EntityStatusEnum} index)
     */
    @TableField("entity_state_flag")
    private Byte stateFlag;

    /**
     * Last state flag, default OFFLINE (1)
     */
    @TableField("last_state_flag")
    private Byte lastStateFlag;

    /**
     * Monotonic version incremented on each heartbeat renewal
     */
    @TableField("lease_version")
    private Long leaseVersion;

    /**
     * Absolute time when this lease expires
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * Timeout in seconds used for this entry
     */
    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    /**
     * Latest heartbeat time
     */
    @TableField("last_heartbeat_time")
    private LocalDateTime lastHeartbeatTime;

    /**
     * Latest alarm ID, default 0
     */
    @TableField("last_alarm_id")
    private Long lastAlarmId;

    /**
     * Timeout source (TimeoutSourceTypeEnum), default 0
     */
    @TableField("timeout_source_flag")
    private Byte timeoutSourceFlag;

    /**
     * JSON extension payload
     */
    @TableField(value = "entity_state_ext", typeHandler = JacksonTypeHandler.class)
    private JsonExt stateExt;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("operate_time")
    private LocalDateTime operateTime;

}

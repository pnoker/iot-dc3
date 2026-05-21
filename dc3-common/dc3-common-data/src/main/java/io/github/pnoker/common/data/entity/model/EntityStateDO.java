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
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Getter
@Setter
@ToString
@TableName("dc3_entity_state")
public class EntityStateDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Entity type flag, 1: device, 2: driver (matches EntityTypeFlagEnum)
     */
    @TableField("entity_type_flag")
    private Byte entityTypeFlag;

    /**
     * Entity ID (driver ID or device ID)
     */
    @TableField("entity_id")
    private Long entityId;

    /**
     * Driver ID. For driver entries same as entity_id; for device entries the owning driver.
     */
    @TableField("driver_id")
    private Long driverId;

    /**
     * Current status index (DriverStatusEnum / DeviceStatusEnum index)
     */
    @TableField("state_flag")
    private Byte stateFlag;

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
     * TTL in seconds used for this entry
     */
    @TableField("ttl_seconds")
    private Integer ttlSeconds;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("operate_time")
    private LocalDateTime operateTime;

    @TableLogic
    @TableField("deleted")
    private Byte deleted;

}

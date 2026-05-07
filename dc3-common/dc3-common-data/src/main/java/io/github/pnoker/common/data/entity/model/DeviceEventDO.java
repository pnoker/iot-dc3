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

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.pnoker.common.entity.ext.JsonExt;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Device Event
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
@TableName(value = "dc3_device_event", autoResultMap = true)
public class DeviceEventDO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Primary key ID
	 */
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

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
	 * Type
	 */
	@TableField("event_type_flag")
	private Byte eventTypeFlag;

	/**
	 *
	 */
	@TableField(value = "event_ext", typeHandler = JacksonTypeHandler.class)
	private JsonExt eventExt;

	/**
	 * ,
	 */
	@TableField("expired_time")
	private Long expiredTime;

	/**
	 * , 0:, 1:
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

	/**
	 * Logical delete flag, 0:not deleted, 1:deleted
	 */
	@TableLogic
	@TableField("deleted")
	private Byte deleted;

}

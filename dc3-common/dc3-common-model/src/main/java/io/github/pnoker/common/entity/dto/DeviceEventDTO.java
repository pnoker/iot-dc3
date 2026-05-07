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

package io.github.pnoker.common.entity.dto;

import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Device Event
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEventDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Type
	 */
	private DeviceEventTypeEnum type;

	/**
	 *
	 */
	private String content;

	/**
	 * Create Time
	 */
	private LocalDateTime createTime;

	public DeviceEventDTO(DeviceEventTypeEnum type, String content) {
		this.type = type;
		this.content = content;
		this.createTime = LocalDateTimeUtil.now();
	}

	/**
	 * Device status
	 *
	 * @author pnoker
	 * @version 2025.9.0
	 * @since 2022.1.0
	 */
	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DeviceStatus implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		/**
		 * Driver ID that reports this status. Populated by the sender.
		 */
		private Long driverId;

		/**
		 * Tenant ID the device belongs to. Populated by the sender.
		 */
		private Long tenantId;

		/**
		 * Device ID
		 */
		private Long deviceId;

		/**
		 * Device status
		 */
		private DeviceStatusEnum status;

		/**
		 * Device status
		 */
		@Builder.Default
		private int timeOut = 15;

		/**
		 * Device status
		 */
		@Builder.Default
		private TimeUnit timeUnit = TimeUnit.MINUTES;

		/**
		 * Human-readable message. Populated for ALARM events; null for HEARTBEAT.
		 */
		private String message;

		/**
		 * Create Time
		 */
		private LocalDateTime createTime;

		public DeviceStatus(Long deviceId, DeviceStatusEnum status) {
			this.deviceId = deviceId;
			this.status = status;
			this.createTime = LocalDateTimeUtil.now();
		}

		public DeviceStatus(Long deviceId, DeviceStatusEnum status, int timeOut, TimeUnit timeUnit) {
			this.deviceId = deviceId;
			this.status = status;
			this.timeOut = timeOut;
			this.timeUnit = timeUnit;
			this.createTime = LocalDateTimeUtil.now();
		}

	}

}

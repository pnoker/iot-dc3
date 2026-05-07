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

package io.github.pnoker.common.data.biz;

import io.github.pnoker.common.entity.dto.DeviceEventDTO;

/**
 * Interface for device-related events
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface DeviceEventService {

	/**
	 * Handle a device heartbeat event: refresh the in-memory online-status key and
	 * persist one row to {@code dc3_device_event}. Derives an ALARM event if the status
	 * flipped between ONLINE/MAINTAIN and OFFLINE.
	 * @param entityDTO DeviceEventDTO
	 */
	void heartbeatEvent(DeviceEventDTO entityDTO);

	/**
	 * Persist a device ALARM event to {@code dc3_device_event}.
	 * @param entityDTO DeviceEventDTO with type=ALARM
	 */
	void alarmEvent(DeviceEventDTO entityDTO);

}

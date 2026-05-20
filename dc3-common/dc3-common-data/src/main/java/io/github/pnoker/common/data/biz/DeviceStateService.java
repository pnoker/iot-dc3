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

import io.github.pnoker.common.entity.dto.DeviceStateDTO;

/**
 * Handles device heartbeat/state events: refreshes the online-status cache and
 * derives alarm rows on status flips.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DeviceStateService {

    /**
     * Handle a device heartbeat: refresh the in-memory online-status key and
     * delegate to {@link DeviceAlarmService} when the status flips between
     * ONLINE/MAINTAIN and OFFLINE.
     *
     * @param entityDTO DeviceStateDTO
     */
    void heartbeat(DeviceStateDTO entityDTO);

}

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

package io.github.pnoker.common.driver.service;

import io.github.pnoker.common.entity.dto.DeviceCommandDTO;

/**
 *
 */
public interface DriverReadService {

    /**
     * 读取位号值
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     */
    void read(Long deviceId, Long pointId);

    /**
     * 指令读取位号值
     *
     * @param commandDTO {@link DeviceCommandDTO}
     */
    void read(DeviceCommandDTO commandDTO);

}

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

import io.github.pnoker.common.entity.dto.PointCommandDTO;

/**
 * Service contract for executing point read operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverReadService {

    /**
     * Reads the current value of the specified point on the specified device.
     *
     * @param deviceId device identifier
     * @param pointId  point identifier
     */
    void read(Long deviceId, Long pointId);

    /**
     * Executes a read command received from the command queue.
     *
     * @param commandDTO point command payload
     */
    void read(PointCommandDTO commandDTO);

}

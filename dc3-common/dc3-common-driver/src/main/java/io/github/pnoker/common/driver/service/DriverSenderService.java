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

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.entity.dto.DeviceStateDTO;
import io.github.pnoker.common.entity.dto.DriverStateDTO;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service contract for publishing driver, device, and point-value messages to RabbitMQ.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverSenderService {

    /**
     * Publishes a driver state heartbeat.
     *
     * @param entityDTO driver state payload
     */
    void driverStateSender(DriverStateDTO entityDTO);

    /**
     * Publishes a device state heartbeat.
     *
     * @param entityDTO device state payload
     */
    void deviceStateSender(DeviceStateDTO entityDTO);

    /**
     * Publishes a device status event using the default timeout.
     *
     * @param deviceId device identifier
     * @param status   device status
     */
    void deviceStatusSender(Long deviceId, DeviceStatusEnum status);

    /**
     * Publishes a device status event using the supplied timeout.
     *
     * @param deviceId device identifier
     * @param status   device status
     * @param timeout  timeout value
     * @param timeoutUnit timeout unit
     */
    void deviceStatusSender(Long deviceId, DeviceStatusEnum status, int timeout, TimeUnit timeoutUnit);

    /**
     * Publishes a driver-level ALARM event. Used by protocol drivers when a connection /
     * read / write fails or a runtime guard trips.
     *
     * @param message human-readable reason, e.g. "OPC UA session dropped"
     */
    void driverAlarmSender(String message);

    /**
     * Publishes a device-level ALARM event scoped to the given device.
     *
     * @param deviceId device identifier
     * @param message  human-readable reason
     */
    void deviceAlarmSender(Long deviceId, String message);

    /**
     * Publishes a single point value.
     *
     * @param entityDTO point value payload
     */
    void pointValueSender(PointValue entityDTO);

    /**
     * Publishes multiple point values.
     *
     * @param entityDTOList point value payloads
     */
    void pointValueSender(List<PointValue> entityDTOList);

    /**
     * Publishes a point command result receipt back to the data center.
     *
     * @param resultDTO command result payload
     */
    void pointCommandResultSender(PointCommandResultDTO resultDTO);

}

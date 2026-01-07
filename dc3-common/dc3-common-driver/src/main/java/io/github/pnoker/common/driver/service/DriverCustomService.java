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

import io.github.pnoker.common.driver.entity.bean.RValue;
import io.github.pnoker.common.driver.entity.bean.WValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;

import java.util.Map;

/**
 * DriverCustomService Interface Definition
 * <p>
 * Describes the core custom behavior logic of the driver, including initialization, scheduling,
 * custom events, and read/write operations.
 * Classes implementing this interface need to implement these methods to override specific driver logic.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface DriverCustomService {
    /**
     * Driver Initialization Interface
     * <p>
     * This interface is automatically called when the driver starts up, used to perform
     * necessary initialization operations for the driver.
     * Developers can configure essential resources or prepare the environment in this method.
     */
    void initial();

    /**
     * Custom Scheduling Interface
     * <p>
     * This interface is used to execute custom scheduling tasks, which can be configured
     * through the configuration file {@code driver.schedule.custom}.
     * Developers can implement custom scheduling logic here, such as timed tasks, periodic tasks, etc.
     * <p>
     * Note: The execution frequency and trigger conditions of scheduling tasks should be
     * configured reasonably according to actual requirements.
     */
    void schedule();

    /**
     * Handle Metadata Events for Driver, Device, and Point
     * <p>
     * This event is triggered when metadata for driver, device, or point undergoes
     * addition, update, or deletion operations.
     * The specific event type (driver, device, or point) is determined by {@link io.github.pnoker.common.enums.MetadataTypeEnum}.
     *
     * @param metadataEvent metadata event object containing detailed event information {@link MetadataEventDTO}
     */
    void event(MetadataEventDTO metadataEvent);

    /**
     * Execute Read Operation
     * <p>
     * This interface is used to read point data from the specified device. Due to differences
     * in device types and communication protocols, read operations may not be directly executable,
     * please handle flexibly according to actual situations.
     * <p>
     * Note: Read operations may throw exceptions, callers need to handle exceptions properly.
     *
     * @param driverConfig driver property configuration containing driver-related configuration information
     * @param pointConfig  point property configuration containing point-related configuration information
     * @param device       device object containing basic device information and properties
     * @param point        point object containing basic point information and properties
     * @return returns the read data encapsulated in a {@link RValue} object
     */
    RValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point);

    /**
     * Execute Write Operation
     * <p>
     * This interface is used to write data to a point in the specified device. Due to differences
     * in device types and communication protocols, write operations may not be directly executable,
     * please handle flexibly according to actual situations.
     * <p>
     * Note: Write operations may throw exceptions, callers need to handle exceptions properly.
     *
     * @param driverConfig driver property configuration containing driver-related configuration information
     * @param pointConfig  point property configuration containing point-related configuration information
     * @param device       device object containing basic device information and properties
     * @param point        point object containing basic point information and properties
     * @param wValue       data to be written encapsulated in a {@link WValue} object
     * @return returns whether the write operation was successful, returns {@code true} if successful,
     * otherwise returns {@code false} or throws an exception
     */
    Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device, PointBO point, WValue wValue);

}

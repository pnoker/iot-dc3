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

package io.github.pnoker.driver.service.impl;

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.enums.DeviceStatusEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Custom driver service implementation for the Virtual Driver.
 * <p>
 * This service provides virtual device simulation capabilities for testing and
 * development. It generates random data for different point types and simulates device
 * behavior without requiring physical hardware connections.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualDriverCustomServiceImpl implements DriverCustomService {

    private final DriverMetadata driverMetadata;

    private final DriverSenderService driverSenderService;

    /**
     * Initializes the virtual driver.
     * <p>
     * This method is called when the driver starts. Override this method to implement
     * custom initialization logic specific to your virtual device simulation.
     * </p>
     */
    @Override
    public void initial() {
        /*
         * Driver initialization logic
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario. This method is automatically executed when the
         * driver starts, and you can perform specific initialization operations here.
         *
         */
    }

    @Override
    public void schedule() {
        // Device state lease renewal is owned by the SDK device health job.
    }

    /**
     * Handles metadata change events for drivers, devices, and points.
     * <p>
     * This method is called when metadata is created, updated, or deleted. Override this
     * method to implement custom event handling logic.
     * </p>
     *
     * @param metadataEvent the metadata event containing type, operation, and ID
     *                      information
     */
    @Override
    public void event(MetadataEventDTO metadataEvent) {
        /*
         * Receive metadata events for driver, device, and point creation, update, and
         * deletion.
         *
         * Metadata type: {@link MetadataTypeEnum} (DRIVER, DEVICE, POINT) Metadata
         * operation type: {@link MetadataOperateTypeEnum} (ADD, DELETE, UPDATE)
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario.
         */
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        MetadataOperateTypeEnum operateType = metadataEvent.getOperateType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            // to do something for device event
            log.info("Driver metadata event received, protocol=virtual, metadataType={}, operateType={}, deviceId={}",
                    metadataType, operateType, metadataEvent.getId());
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            // to do something for point event
            log.info("Driver metadata event received, protocol=virtual, metadataType={}, operateType={}, pointId={}",
                    metadataType, operateType, metadataEvent.getId());
        }
    }

    /**
     * Reads data from a virtual device point.
     * <p>
     * This method generates simulated data based on the point type:
     * <ul>
     * <li>STRING type: returns "abcd1234"</li>
     * <li>BOOLEAN type: returns a random boolean value</li>
     * <li>Other types: returns a random float between 0 and 100</li>
     * </ul>
     * Override this method to implement custom data generation logic.
     * </p>
     *
     * @param driverConfig driver configuration attributes
     * @param pointConfig  point configuration attributes
     * @param device       the device to read from
     * @param point        the point to read
     * @return the read value wrapped in a ReadPointValue object
     */
    @Override
    public ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                               PointBO point) {
        /*
         * Read device point data logic
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario. Generate random data based on point type: - If the
         * point type is STRING, generate a random string of length 8; - If the point type
         * is BOOLEAN, generate a random boolean value; - Otherwise, generate a random
         * float between 0 and 100.
         */
        if (PointTypeFlagEnum.STRING.equals(point.getPointTypeFlag())) {
            return new ReadPointValue(device, point, "abcd1234");
        }
        if (PointTypeFlagEnum.BOOLEAN.equals(point.getPointTypeFlag())) {
            Random random = new Random();
            boolean b = random.nextBoolean();
            return new ReadPointValue(device, point, String.valueOf(b));
        }

        Random random = new Random();
        double value = random.nextDouble() * 100;
        return new ReadPointValue(device, point, String.valueOf(value));
    }

    /**
     * Writes data to a virtual device point.
     * <p>
     * By default, this method returns false indicating the write operation is not
     * supported. Override this method to implement custom write logic for virtual
     * devices.
     * </p>
     *
     * @param driverConfig    driver configuration attributes
     * @param pointConfig     point configuration attributes
     * @param device          the device to write to
     * @param point           the point to write
     * @param writePointValue the value to write
     * @return true if the write operation succeeded, false otherwise
     */
    @Override
    public Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                         PointBO point, WritePointValue writePointValue) {
        /*
         * Write device point data logic
         *
         * Hint: The logic here is for reference only; please modify it according to the
         * actual application scenario. You can implement the point data write logic based
         * on specific business requirements. By default, this method returns false,
         * indicating that the write operation was not executed or failed.
         */
        return false;
    }

}

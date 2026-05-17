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

import io.github.pnoker.common.driver.entity.bean.ReadPointValue;
import io.github.pnoker.common.driver.entity.bean.WritePointValue;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;

import java.util.Map;

/**
 * Protocol-level read / write operations a driver exposes to the SDK runtime.
 * <p>
 * The SDK's {@code DriverReadService} and {@code DriverWriteService} delegate
 * to these methods after they have resolved metadata, configuration, and
 * tenant scope — the implementation only has to talk to the device.
 *
 * <p>Throwing from either method is the canonical way to signal that the
 * protocol attempt failed; the SDK logs the error and acks/nacks the
 * originating command on RabbitMQ.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverProtocol {

    /**
     * Read one point value from the device.
     *
     * @param driverConfig driver attribute values keyed by attribute code
     * @param pointConfig  point attribute values keyed by attribute code
     * @param device       device descriptor with id, code, profile bindings
     * @param point        point descriptor with type flag, scaling, unit
     * @return the freshly read sample wrapped in a {@link ReadPointValue}
     */
    ReadPointValue read(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                        PointBO point);

    /**
     * Write one point value to the device.
     *
     * @param driverConfig    driver attribute values keyed by attribute code
     * @param pointConfig     point attribute values keyed by attribute code
     * @param device          device descriptor with id, code, profile bindings
     * @param point           point descriptor with type flag, scaling, unit
     * @param writePointValue the value to write, already coerced to the point's
     *                        declared type
     * @return {@code true} when the device acknowledged the write; otherwise
     * either return {@code false} or throw a domain exception
     */
    Boolean write(Map<String, AttributeBO> driverConfig, Map<String, AttributeBO> pointConfig, DeviceBO device,
                  PointBO point, WritePointValue writePointValue);

}

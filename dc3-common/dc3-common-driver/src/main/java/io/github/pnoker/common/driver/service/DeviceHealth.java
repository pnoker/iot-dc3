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

import io.github.pnoker.common.driver.entity.bean.DeviceHealthState;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;

import java.util.Map;

/**
 * Protocol-level device health hook invoked by the SDK's device health schedule.
 *
 * <p>The SDK resolves device metadata and driver configuration, then calls this
 * method for each enabled device according to {@code dc3.driver.health.device.cron}.
 * The returned {@link DeviceHealthState} carries both the ONLINE/OFFLINE decision
 * and, when needed, a per-device lease timeout.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
public interface DeviceHealth {

    /**
     * Determine device health from the protocol driver's point of view.
     *
     * @param driverConfig driver attribute values keyed by attribute code
     * @param device       device descriptor with id, code, profile bindings
     * @return device health state and optional lease timeout
     */
    default DeviceHealthState health(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        return DeviceHealthState.online();
    }

}

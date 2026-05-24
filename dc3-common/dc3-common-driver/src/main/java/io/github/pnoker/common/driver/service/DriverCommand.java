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

import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;

import java.util.Collections;
import java.util.Map;

/**
 * Contract for executing custom commands on a device.
 * <p>
 * The default implementation returns an empty map so existing drivers that
 * don't support custom commands compile and operate unchanged. Drivers that
 * do support custom commands override this method to execute the command
 * against the device and return output parameter name-value pairs.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
public interface DriverCommand {

    /**
     * Execute a custom command on a device.
     *
     * @param driverConfig  driver-level attribute configurations
     * @param commandConfig command-level attribute configurations
     * @param device        device metadata
     * @param command       command metadata (code, type flags, timeout, ext)
     * @param paramValues   input parameter name-value pairs
     * @return output parameter name-value pairs; empty map if command has no outputs
     */
    default Map<String, String> execute(
            Map<String, AttributeBO> driverConfig,
            Map<String, AttributeBO> commandConfig,
            DeviceBO device,
            FacadeCommandBO command,
            Map<String, String> paramValues) {
        return Collections.emptyMap();
    }

}

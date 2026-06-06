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

import io.github.pnoker.common.driver.entity.bean.ValidationReport;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;

import java.util.Map;

/**
 * Configuration validation and simulation contract for protocol drivers.
 *
 * <p>This SPI provides two capabilities that do NOT require a real device:
 *
 * <ul>
 *   <li><b>validate</b> — check that driver/device/point configurations are
 *       structurally complete and semantically valid (e.g. required attributes
 *       present, port in range, codec parameters compatible).</li>
 *   <li><b>simulate</b> — generate a deterministic synthetic value for a point.
 *       Unlike the virtual driver's random generator, this produces stable outputs
 *       useful for frontend integration and configuration dry-runs.</li>
 * </ul>
 *
 * <p>All methods come with sensible defaults: validate always passes, simulate
 * returns a type-appropriate fixture value. Drivers override only the checks
 * that are meaningful for their protocol.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
public interface DriverValidator {

    /**
     * Validate driver-level configuration.
     * <p>
     * Invoked at driver startup. Use this to check that every mandatory
     * driver attribute is present and its value is within the expected range.
     *
     * @param driverConfig driver attribute values keyed by attribute code
     * @return validation report
     */
    default ValidationReport validate(Map<String, AttributeBO> driverConfig) {
        return ValidationReport.passed();
    }

    /**
     * Validate device-level configuration.
     * <p>
     * Invoked when a new device is registered or its metadata is updated.
     * Use this to verify that the device's attributes are complete and
     * consistent with the protocol's expectations.
     *
     * @param driverConfig driver attribute values keyed by attribute code
     * @param device       device metadata
     * @return validation report
     */
    default ValidationReport validateDevice(Map<String, AttributeBO> driverConfig, DeviceBO device) {
        return ValidationReport.passed();
    }

    /**
     * Validate point-level configuration.
     * <p>
     * Invoked when points are added or updated. Use this to check that
     * protocol-specific point attributes (e.g. slaveId, functionCode, offset
     * for Modbus) are present and correctly typed.
     *
     * @param pointConfig point attribute values keyed by attribute code
     * @param point       point metadata
     * @return validation report
     */
    default ValidationReport validatePoint(Map<String, AttributeBO> pointConfig, PointBO point) {
        return ValidationReport.passed();
    }

    /**
     * Generate a deterministic synthetic value for the given point.
     * <p>
     * The returned value is type-aware and stable — calling simulate twice for
     * the same point returns the same value. This is useful for:
     *
     * <ul>
     *   <li>Frontend integration testing without a real device</li>
     *   <li>Configuration dry-runs to verify the data pipeline</li>
     *   <li>Demo and documentation</li>
     * </ul>
     *
     * @param point point metadata (type flag is the primary discriminator)
     * @return simulated value as string
     */
    default String simulate(PointBO point) {
        return switch (point.getPointTypeFlag()) {
            case INT -> "42";
            case LONG -> "424242424242";
            case FLOAT -> "3.14";
            case DOUBLE -> "3.141592653589793";
            case STRING -> "simulated";
            case BOOLEAN -> "true";
            default -> "0";
        };
    }

}

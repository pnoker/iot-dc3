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

package io.github.pnoker.common.driver.metadata;

import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.enums.DriverStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * In-memory holder for driver registration state and shared metadata used across the
 * driver runtime.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
@Component
public final class DriverMetadata {

	/**
	 * Current driver status.
	 */
	private DriverStatusEnum driverStatus = DriverStatusEnum.OFFLINE;

	/**
	 * Registered driver definition.
	 */
	private DriverBO driver;

	/**
	 * Identifiers of devices owned by the driver.
	 */
	private Set<Long> deviceIds;

	/**
	 * Driver attributes keyed by attribute identifier.
	 */
	private Map<Long, DriverAttributeDTO> driverAttributeIdMap;

	/**
	 * Driver attributes keyed by attribute code.
	 */
	private Map<String, DriverAttributeDTO> driverAttributeNameMap;

	/**
	 * Point attributes keyed by attribute identifier.
	 */
	private Map<Long, PointAttributeDTO> pointAttributeIdMap;

	/**
	 * Point attributes keyed by attribute code.
	 */
	private Map<String, PointAttributeDTO> pointAttributeNameMap;

}

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

package io.github.pnoker.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Common driver type flag enumeration
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum DriverTypeFlagEnum {

	/**
	 * Protocol driver, client mode
	 */
	DRIVER_CLIENT((byte) 0, "driver_client", "Protocol driver, client mode"),

	/**
	 * Protocol driver, server mode
	 */
	DRIVER_SERVER((byte) 1, "driver_server", "Protocol driver, server mode"),

	/**
	 * Gateway driver
	 */
	GATEWAY((byte) 2, "gateway", "Gateway driver"),

	/**
	 * Connection driver
	 */
	CONNECT((byte) 3, "connect", "Connection driver"),;

	/**
	 * Index
	 */
	@EnumValue
	private final Byte index;

	/**
	 * Code
	 */
	private final String code;

	/**
	 * Remark
	 */
	private final String remark;

	/**
	 * Get enum by index
	 * @param index Index
	 * @return {@link DriverTypeFlagEnum}
	 */
	public static DriverTypeFlagEnum ofIndex(Byte index) {
		Optional<DriverTypeFlagEnum> any = Arrays.stream(DriverTypeFlagEnum.values())
			.filter(type -> type.getIndex().equals(index))
			.findFirst();
		return any.orElse(null);
	}

	/**
	 * Get enum by code
	 * @param code Code
	 * @return {@link DriverTypeFlagEnum}
	 */
	public static DriverTypeFlagEnum ofCode(String code) {
		Optional<DriverTypeFlagEnum> any = Arrays.stream(DriverTypeFlagEnum.values())
			.filter(type -> type.getCode().equals(code))
			.findFirst();
		return any.orElse(null);
	}

	/**
	 * Get enum by name
	 * @param name Name
	 * @return {@link DriverTypeFlagEnum}
	 */
	public static DriverTypeFlagEnum ofName(String name) {
		try {
			return valueOf(name);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

}

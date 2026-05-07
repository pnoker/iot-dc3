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

package io.github.pnoker.common.manager.entity.bo;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.ext.DriverAttributeExt;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

/**
 * BO
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class DriverAttributeBO extends BaseBO {

	/**
	 * Name
	 */
	private String attributeName;

	/**
	 * Code
	 */
	private String attributeCode;

	/**
	 * Type
	 */
	private AttributeTypeFlagEnum attributeTypeFlag;

	/**
	 *
	 */
	private String defaultValue;

	/**
	 * Driver ID
	 */
	private Long driverId;

	/**
	 *
	 */
	private DriverAttributeExt attributeExt;

	/**
	 * Enable flag
	 */
	private EnableFlagEnum enableFlag;

	/**
	 * Tenant ID
	 */
	private Long tenantId;

	/**
	 *
	 */
	private String signature;

	/**
	 *
	 */
	private Integer version;

}

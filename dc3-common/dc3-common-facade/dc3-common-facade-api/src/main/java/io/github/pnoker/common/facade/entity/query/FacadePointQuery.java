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

package io.github.pnoker.common.facade.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Facade-level point query.
 *
 * @author pnoker
 * @since 2026.5.5
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FacadePointQuery implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Pages page;

	private Long tenantId;

	private String pointName;

	private String pointCode;

	private PointTypeFlagEnum pointTypeFlag;

	private RwFlagEnum rwFlag;

	private Long profileId;

	private EnableFlagEnum enableFlag;

	private Integer version;

	/**
	 * Device ID — filter points bound to a specific device.
	 */
	private Long deviceId;

}

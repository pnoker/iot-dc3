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

package io.github.pnoker.common.entity.bo;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Point Query Business Object
 * <p>
 * Business object for point queries in IoT DC3 platform.
 * Contains device ID and point ID for querying specific point data.
 * Used for retrieving point information from the repository layer.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PointQueryBO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Device ID for querying point data
     */
    private Long deviceId;

    /**
     * Point ID for querying specific point
     */
    private Long pointId;
}

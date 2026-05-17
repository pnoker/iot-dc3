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

package io.github.pnoker.common.facade.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Facade-level point value BO.
 * <p>
 * Field set matches {@code api.center.data.PointValueApi} wire shape. Uses {@code value}
 * (the processed/calculated value) and epoch-second {@code createTime} to keep the facade
 * transport-neutral (no LocalDateTime dependency).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FacadePointValueBO {

    /**
     * Device ID associated with the point value
     */
    private Long deviceId;

    /**
     * Point ID associated with the value
     */
    private Long pointId;

    /**
     * Processed/calculated value after transformations
     */
    private String value;

    /**
     * Raw value as received from the device
     */
    private String rawValue;

    /**
     * Numeric projection of {@link #value}; null when value is non-numeric
     */
    private Double numValue;

    /**
     * Storage timestamp (epoch seconds)
     */
    private long createTime;

}

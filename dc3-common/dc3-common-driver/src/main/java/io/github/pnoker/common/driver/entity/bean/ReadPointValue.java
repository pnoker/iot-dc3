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

package io.github.pnoker.common.driver.entity.bean;

import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.support.TypedValueConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Raw point value read from a device together with the device and point metadata
 * required to calculate its final value.
 *
 * @author pnoker
 * @version 2026.5.15
 * @since 2026.5.15
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReadPointValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Source device that produced the value.
     */
    private DeviceBO device;

    /**
     * Point definition associated with the value.
     */
    private PointBO point;

    /**
     * Raw point value represented as a string.
     */
    private String value;

    /**
     * Calculates the final point value and numeric projection without mutating this
     * carrier object.
     *
     * @return calculated point value
     */
    public CalculatedPointValue calculate() {
        return TypedValueConverter.calculatePointValue(value, point);
    }

    /**
     * Returns the final point value after applying scaling and type conversion rules
     * defined by the point metadata.
     *
     * @return final point value as a string
     */
    public String getFinalValue() {
        return calculate().getFinalValue();
    }

    /**
     * Returns the numeric projection computed from the current point metadata and raw
     * value.
     *
     * @return numeric value or {@code null} for non-numeric point types
     */
    public Double getNumericValue() {
        return calculate().getNumericValue();
    }

}

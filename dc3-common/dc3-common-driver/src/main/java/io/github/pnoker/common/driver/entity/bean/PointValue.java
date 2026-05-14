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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message payload that carries a point reading, including the raw value, the calculated
 * value, and the timestamp when the reading was produced.
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Driver ID that collected the data. Populated by the sender before the message is
     * published.
     */
    private Long driverId;

    /**
     * Tenant ID the data belongs to. Populated by the sender before the message is
     * published.
     */
    private Long tenantId;

    /**
     * Device ID
     */
    private Long deviceId;

    /**
     * Point ID
     */
    private Long pointId;

    /**
     * Raw value
     */
    private String rawValue;

    /**
     * Calculated point value after applying scaling and type conversion.
     */
    private String calValue;

    /**
     * Numeric projection of {@link #calValue} for aggregation queries. Set by the
     * driver when the point type is numeric or boolean (1.0/0.0); {@code null} for
     * string payloads.
     */
    private Double numValue;

    /**
     * Time when the point value message was created.
     */
    private LocalDateTime createTime;

    public PointValue(RValue rValue) {
        this.deviceId = rValue.getDevice().getId();
        this.pointId = rValue.getPoint().getId();
        this.rawValue = rValue.getValue();
        this.calValue = rValue.getFinalValue();
        this.numValue = rValue.getNumericValue();
        this.createTime = LocalDateTimeUtil.now();
    }

}

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
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
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
import java.util.Locale;
import java.util.Objects;

/**
 * Message payload that carries a point reading, including the raw value, the calculated
 * value, and the timestamp when the reading was produced.
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

    public PointValue(ReadPointValue readPointValue) {
        CalculatedPointValue calculatedValue = readPointValue.calculate();
        this.deviceId = readPointValue.getDevice().getId();
        this.pointId = readPointValue.getPoint().getId();
        this.rawValue = readPointValue.getValue();
        this.calValue = calculatedValue.getFinalValue();
        this.numValue = calculatedValue.getNumericValue();
        this.createTime = LocalDateTimeUtil.now();
    }

    /**
     * Creates a {@link PointValue} for a write echo where the value is already in its
     * final device-level form. Scaling ({@code base}, {@code multiple}, {@code decimal})
     * is NOT re-applied; the raw and calibrated values are set to the same string.
     *
     * @param device source device
     * @param point  point definition (used only for numeric projection)
     * @param value  final device-level value returned after a successful write
     * @return a PointValue with {@code rawValue == calValue == value}
     */
    public static PointValue ofRawValue(DeviceBO device, PointBO point, String value) {
        PointValue pointValue = new PointValue();
        pointValue.setDeviceId(device.getId());
        pointValue.setPointId(point.getId());
        pointValue.setRawValue(value);
        pointValue.setCalValue(value);
        pointValue.setNumValue(resolveNumValue(value, point));
        pointValue.setCreateTime(LocalDateTimeUtil.now());
        return pointValue;
    }

    /**
     * Computes a numeric projection of a final (already-scaled) value without
     * re-applying point scaling rules.
     *
     * @param value final value string
     * @param point point definition (used for type determination)
     * @return numeric value or {@code null} for string-typed points
     */
    private static Double resolveNumValue(String value, PointBO point) {
        if (Objects.isNull(point) || Objects.isNull(value) || Objects.isNull(point.getPointTypeFlag())) {
            return null;
        }
        return switch (point.getPointTypeFlag()) {
            case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> {
                try {
                    yield Double.parseDouble(value.trim());
                } catch (NumberFormatException ignored) {
                    yield null;
                }
            }
            case BOOLEAN -> {
                String normalized = value.trim().toLowerCase(Locale.ROOT);
                yield switch (normalized) {
                    case "true", "1" -> 1.0;
                    case "false", "0" -> 0.0;
                    default -> null;
                };
            }
            case STRING -> null;
        };
    }

}

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
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.OutRangeException;
import io.github.pnoker.common.utils.ArithmeticUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a raw point value read from a device together with the device and point
 * metadata required to calculate its final value.
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
public class RValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final BigDecimal defaultBase = new BigDecimal(0);

    private static final BigDecimal defaultMultiple = new BigDecimal(1);

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
     * Numeric projection for aggregation queries. Set by {@link #getFinalValue()} on
     * first access: numeric types carry the converted number, booleans are 1.0/0.0,
     * strings remain {@code null}.
     */
    private transient Double numericValue;

    /**
     * Returns the final point value after applying scaling and type conversion rules
     * defined by the point metadata. As a side effect this also populates
     * {@link #numericValue} so callers can retrieve the typed double without
     * re-parsing.
     *
     * @return final point value as a string
     */
    public String getFinalValue() {
        if (Objects.isNull(point)) {
            throw new EmptyException("Point is empty");
        }

        PointTypeFlagEnum valueType = Optional.ofNullable(point.getPointTypeFlag()).orElse(PointTypeFlagEnum.STRING);
        BigDecimal base = Optional.ofNullable(point.getBaseValue()).orElse(defaultBase);
        BigDecimal multiple = Optional.ofNullable(point.getMultiple()).orElse(defaultMultiple);
        byte decimal = Optional.ofNullable(point.getValueDecimal()).orElse((byte) 6);

        return switch (valueType) {
            case STRING -> {
                numericValue = null;
                yield value;
            }
            case BYTE -> {
                byte v = getByteValue(value, base, multiple);
                numericValue = (double) v;
                yield String.valueOf(v);
            }
            case SHORT -> {
                short v = getShortValue(value, base, multiple);
                numericValue = (double) v;
                yield String.valueOf(v);
            }
            case INT -> {
                int v = getIntegerValue(value, base, multiple);
                numericValue = (double) v;
                yield String.valueOf(v);
            }
            case LONG -> {
                long v = getLongValue(value, base, multiple);
                numericValue = (double) v;
                yield String.valueOf(v);
            }
            case FLOAT -> {
                float v = getFloatValue(value, base, multiple, decimal);
                numericValue = (double) v;
                yield String.valueOf(v);
            }
            case DOUBLE -> {
                double v = getDoubleValue(value, base, multiple, decimal);
                numericValue = v;
                yield String.valueOf(v);
            }
            case BOOLEAN -> {
                boolean v = getBooleanValue(value);
                numericValue = v ? 1.0 : 0.0;
                yield String.valueOf(v);
            }
        };
    }

    /**
     * Returns the numeric projection computed by the last {@link #getFinalValue()} call.
     *
     * @return numeric value or {@code null} for non-numeric point types
     */
    public Double getNumericValue() {
        return numericValue;
    }
    /**
     * Convenience constructor for test fixtures.
     */
    public RValue(DeviceBO device, PointBO point, String value) {
        this.device = device;
        this.point = point;
        this.value = value;
    }


    /**
     * Applies a linear transformation using the formula {@code y = ax + b}.
     *
     * @param a multiplier
     * @param x raw value
     * @param b offset
     * @return transformed decimal value
     */
    private BigDecimal getLinearValue(BigDecimal a, String x, BigDecimal b) {
        BigDecimal bigDecimal = new BigDecimal(x);
        if (defaultMultiple.compareTo(a) == 0 && defaultBase.compareTo(b) == 0) {
            return bigDecimal;
        }
        if (defaultMultiple.compareTo(a) != 0 && defaultBase.compareTo(b) == 0) {
            return bigDecimal.multiply(a);
        }
        if (defaultMultiple.compareTo(a) == 0 && defaultBase.compareTo(b) != 0) {
            return bigDecimal.add(b);
        }
        BigDecimal multiply = a.multiply(bigDecimal);
        return multiply.add(b);
    }

    /**
     * Converts the raw value to a byte after linear scaling.
     *
     * @param rawValue raw value
     * @return converted byte value
     */
    private byte getByteValue(String rawValue, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            return multiply.byteValueExact();
        } catch (Exception e) {
            throw new OutRangeException("Out of byte range: {} ~ {}, current: {}", Byte.MIN_VALUE, Byte.MAX_VALUE,
                    rawValue);
        }
    }

    /**
     * Converts the raw value to a short after linear scaling.
     *
     * @param rawValue raw value
     * @return converted short value
     */
    private short getShortValue(String rawValue, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            return multiply.shortValueExact();
        } catch (Exception e) {
            throw new OutRangeException("Out of short range: {} ~ {}, current: {}", Short.MIN_VALUE, Short.MAX_VALUE,
                    rawValue);
        }
    }

    /**
     * Converts the raw value to an integer after linear scaling.
     *
     * @param rawValue raw value
     * @return converted integer value
     */
    private int getIntegerValue(String rawValue, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            return multiply.intValueExact();
        } catch (Exception e) {
            throw new OutRangeException("Out of int range: {} ~ {}, current: {}", Integer.MIN_VALUE, Integer.MAX_VALUE,
                    rawValue);
        }
    }

    /**
     * Converts the raw value to a long after linear scaling.
     *
     * @param rawValue raw value
     * @return converted long value
     */
    private long getLongValue(String rawValue, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            return multiply.longValueExact();
        } catch (Exception e) {
            throw new OutRangeException("Out of long range: {} ~ {}, current: {}", Long.MIN_VALUE, Long.MAX_VALUE,
                    rawValue);
        }
    }

    /**
     * Converts the raw value to a rounded float after linear scaling.
     *
     * @param rawValue raw value
     * @return converted float value
     */
    private float getFloatValue(String rawValue, BigDecimal base, BigDecimal multiple, byte decimal) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            float result = multiply.floatValue();
            if (!Float.isFinite(result)) {
                throw new OutRangeException();
            }
            return ArithmeticUtil.round(result, decimal);
        } catch (Exception e) {
            throw new OutRangeException("Out of float range: |{} ~ {}|, current: {}", Float.MIN_VALUE, Float.MAX_VALUE,
                    rawValue);
        }
    }

    /**
     * Converts the raw value to a rounded double after linear scaling.
     *
     * @param rawValue raw value
     * @return converted double value
     */
    private double getDoubleValue(String rawValue, BigDecimal base, BigDecimal multiple, byte decimal) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            double result = multiply.doubleValue();
            if (!Double.isFinite(result)) {
                throw new OutRangeException();
            }
            return ArithmeticUtil.round(result, decimal);
        } catch (Exception e) {
            throw new OutRangeException("Out of double range: |{} ~ {}|, current: {}", Double.MIN_VALUE,
                    Double.MAX_VALUE, rawValue);
        }
    }

    /**
     * Converts the raw value to a boolean.
     *
     * @param rawValue raw value
     * @return converted boolean value
     */
    private boolean getBooleanValue(String rawValue) {
        return Boolean.parseBoolean(rawValue);
    }

}

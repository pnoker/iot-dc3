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

package io.github.pnoker.common.driver.support;

import io.github.pnoker.common.driver.entity.bean.CalculatedPointValue;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.OutRangeException;
import io.github.pnoker.common.exception.TypeException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.utils.ArithmeticUtil;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared conversion rules for driver attribute values, point write values, and raw
 * point readings.
 *
 * @author pnoker
 * @version 2026.5.15
 * @since 2016.10.1
 */
public final class TypedValueConverter {

    private static final BigDecimal DEFAULT_BASE = BigDecimal.ZERO;

    private static final BigDecimal DEFAULT_MULTIPLE = BigDecimal.ONE;

    private static final byte DEFAULT_DECIMAL = 6;

    private TypedValueConverter() {
    }

    /**
     * Converts a driver or point configuration attribute to the requested Java type.
     *
     * @param value      raw attribute value
     * @param type       declared attribute type
     * @param targetType requested Java type
     * @param <T>        target type parameter
     * @return converted value
     */
    public static <T> T convertAttributeValue(String value, AttributeTypeEnum type, Class<T> targetType) {
        if (Objects.isNull(type)) {
            throw new UnSupportException("Unsupported attribute type of " + type);
        }
        return convert(value, scalarType(type), type.getCode(), targetType, "Attribute");
    }

    /**
     * Converts a point write command value to the requested Java type.
     *
     * @param value      raw point value
     * @param type       declared point type
     * @param targetType requested Java type
     * @param <T>        target type parameter
     * @return converted value
     */
    public static <T> T convertPointValue(String value, PointTypeEnum type, Class<T> targetType) {
        if (Objects.isNull(type)) {
            throw new UnSupportException("Unsupported point type of " + type);
        }
        return convert(value, scalarType(type), type.getCode(), targetType, "Point");
    }

    /**
     * Applies point metadata conversion rules to a raw device reading.
     *
     * @param rawValue raw device value
     * @param point    point metadata
     * @return calculated value and numeric projection
     */
    public static CalculatedPointValue calculatePointValue(String rawValue, PointBO point) {
        if (Objects.isNull(point)) {
            throw new EmptyException("Point is empty");
        }

        PointTypeEnum pointType = Optional.ofNullable(point.getPointTypeFlag()).orElse(PointTypeEnum.STRING);
        ScalarType type = scalarType(pointType);
        BigDecimal base = Optional.ofNullable(point.getBaseValue()).orElse(DEFAULT_BASE);
        BigDecimal multiple = Optional.ofNullable(point.getMultiple()).orElse(DEFAULT_MULTIPLE);
        byte decimal = Optional.ofNullable(point.getValueDecimal()).orElse(DEFAULT_DECIMAL);

        return switch (type) {
            case STRING -> new CalculatedPointValue(requireValue(rawValue, type, "Point"), null);
            case BYTE -> {
                byte value = exactByte(linearValue(multiple, rawValue, base, "Point", pointType.getCode()), rawValue,
                        "Point", pointType.getCode());
                yield new CalculatedPointValue(String.valueOf(value), (double) value);
            }
            case SHORT -> {
                short value = exactShort(linearValue(multiple, rawValue, base, "Point", pointType.getCode()), rawValue,
                        "Point", pointType.getCode());
                yield new CalculatedPointValue(String.valueOf(value), (double) value);
            }
            case INT -> {
                int value = exactInt(linearValue(multiple, rawValue, base, "Point", pointType.getCode()), rawValue,
                        "Point", pointType.getCode());
                yield new CalculatedPointValue(String.valueOf(value), (double) value);
            }
            case LONG -> {
                long value = exactLong(linearValue(multiple, rawValue, base, "Point", pointType.getCode()), rawValue,
                        "Point", pointType.getCode());
                yield new CalculatedPointValue(String.valueOf(value), (double) value);
            }
            case FLOAT -> {
                float value = roundedFloat(linearValue(multiple, rawValue, base, "Point", pointType.getCode()),
                        rawValue, decimal, "Point", pointType.getCode());
                yield new CalculatedPointValue(String.valueOf(value), (double) value);
            }
            case DOUBLE -> {
                double value = roundedDouble(linearValue(multiple, rawValue, base, "Point", pointType.getCode()),
                        rawValue, decimal, "Point", pointType.getCode());
                yield new CalculatedPointValue(String.valueOf(value), value);
            }
            case BOOLEAN -> {
                boolean value = strictBoolean(rawValue, "Point", pointType.getCode());
                yield new CalculatedPointValue(String.valueOf(value), value ? 1.0 : 0.0);
            }
        };
    }

    /**
     * Convert a raw string value to the target scalar type, dispatching by type and
     * applying range/exactness validation on numeric values.
     *
     * @param value      the raw string value
     * @param type       the resolved scalar type
     * @param typeCode   the original type code, for error messages
     * @param targetType the expected boxed return type
     * @param label      the value label, for error messages
     * @param <T>        the return type
     * @return the converted value
     */
    @SuppressWarnings("unchecked")
    private static <T> T convert(String value, ScalarType type, String typeCode, Class<T> targetType, String label) {
        requireTarget(type, typeCode, targetType, label);
        return switch (type) {
            case STRING -> (T) requireValue(value, type, label);
            case BYTE -> (T) Byte.valueOf(exactByte(decimal(value, type, label, typeCode), value, label, typeCode));
            case SHORT -> (T) Short.valueOf(exactShort(decimal(value, type, label, typeCode), value, label, typeCode));
            case INT -> (T) Integer.valueOf(exactInt(decimal(value, type, label, typeCode), value, label, typeCode));
            case LONG -> (T) Long.valueOf(exactLong(decimal(value, type, label, typeCode), value, label, typeCode));
            case FLOAT -> (T) Float.valueOf(finiteFloat(decimal(value, type, label, typeCode), value, label, typeCode));
            case DOUBLE ->
                    (T) Double.valueOf(finiteDouble(decimal(value, type, label, typeCode), value, label, typeCode));
            case BOOLEAN -> (T) Boolean.valueOf(strictBoolean(value, label, typeCode));
        };
    }

    /**
     * Apply a linear transform (value × multiple + base) to a raw value, short-circuiting
     * when the multiple and base are at their identity defaults (1 and 0).
     *
     * @param multiple the multiplier
     * @param rawValue the raw string value
     * @param base     the additive base
     * @param label    the value label, for error messages
     * @param typeCode the type code, for error messages
     * @return the transformed value
     */
    private static BigDecimal linearValue(BigDecimal multiple, String rawValue, BigDecimal base, String label,
                                          String typeCode) {
        BigDecimal value = decimal(rawValue, ScalarType.DOUBLE, label, typeCode);
        if (DEFAULT_MULTIPLE.compareTo(multiple) == 0 && DEFAULT_BASE.compareTo(base) == 0) {
            return value;
        }
        if (DEFAULT_MULTIPLE.compareTo(multiple) != 0 && DEFAULT_BASE.compareTo(base) == 0) {
            return value.multiply(multiple);
        }
        if (DEFAULT_MULTIPLE.compareTo(multiple) == 0 && DEFAULT_BASE.compareTo(base) != 0) {
            return value.add(base);
        }
        return multiple.multiply(value).add(base);
    }

    private static BigDecimal decimal(String value, ScalarType type, String label, String typeCode) {
        String raw = requireValue(value, type, label).trim();
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException ignored) {
            throw new TypeException("{} value type is: {}, invalid numeric value: {}", label, typeCode, value);
        }
    }

    private static byte exactByte(BigDecimal value, String rawValue, String label, String typeCode) {
        if (value.compareTo(BigDecimal.valueOf(Byte.MIN_VALUE)) < 0
                || value.compareTo(BigDecimal.valueOf(Byte.MAX_VALUE)) > 0) {
            throw new OutRangeException("{} value out of byte range: {} ~ {}, current: {}", label, Byte.MIN_VALUE,
                    Byte.MAX_VALUE, rawValue);
        }
        if (!isInteger(value)) {
            throw new TypeException("{} value type is: {}, expected exact byte value: {}", label, typeCode, rawValue);
        }
        return value.byteValueExact();
    }

    private static short exactShort(BigDecimal value, String rawValue, String label, String typeCode) {
        if (value.compareTo(BigDecimal.valueOf(Short.MIN_VALUE)) < 0
                || value.compareTo(BigDecimal.valueOf(Short.MAX_VALUE)) > 0) {
            throw new OutRangeException("{} value out of short range: {} ~ {}, current: {}", label, Short.MIN_VALUE,
                    Short.MAX_VALUE, rawValue);
        }
        if (!isInteger(value)) {
            throw new TypeException("{} value type is: {}, expected exact short value: {}", label, typeCode, rawValue);
        }
        return value.shortValueExact();
    }

    private static int exactInt(BigDecimal value, String rawValue, String label, String typeCode) {
        if (value.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0
                || value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new OutRangeException("{} value out of int range: {} ~ {}, current: {}", label, Integer.MIN_VALUE,
                    Integer.MAX_VALUE, rawValue);
        }
        if (!isInteger(value)) {
            throw new TypeException("{} value type is: {}, expected exact int value: {}", label, typeCode, rawValue);
        }
        return value.intValueExact();
    }

    private static long exactLong(BigDecimal value, String rawValue, String label, String typeCode) {
        if (value.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) < 0
                || value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw new OutRangeException("{} value out of long range: {} ~ {}, current: {}", label, Long.MIN_VALUE,
                    Long.MAX_VALUE, rawValue);
        }
        if (!isInteger(value)) {
            throw new TypeException("{} value type is: {}, expected exact long value: {}", label, typeCode, rawValue);
        }
        return value.longValueExact();
    }

    private static float roundedFloat(BigDecimal value, String rawValue, byte decimal, String label, String typeCode) {
        return ArithmeticUtil.round(finiteFloat(value, rawValue, label, typeCode), decimal);
    }

    private static double roundedDouble(BigDecimal value, String rawValue, byte decimal, String label, String typeCode) {
        return ArithmeticUtil.round(finiteDouble(value, rawValue, label, typeCode), decimal);
    }

    private static float finiteFloat(BigDecimal value, String rawValue, String label, String typeCode) {
        float result = value.floatValue();
        if (!Float.isFinite(result)) {
            throw new OutRangeException("{} value out of float range: {} ~ {}, current: {}", label, -Float.MAX_VALUE,
                    Float.MAX_VALUE, rawValue);
        }
        return result;
    }

    private static double finiteDouble(BigDecimal value, String rawValue, String label, String typeCode) {
        double result = value.doubleValue();
        if (!Double.isFinite(result)) {
            throw new OutRangeException("{} value out of double range: {} ~ {}, current: {}", label, -Double.MAX_VALUE,
                    Double.MAX_VALUE, rawValue);
        }
        return result;
    }

    private static boolean strictBoolean(String value, String label, String typeCode) {
        String raw = requireValue(value, ScalarType.BOOLEAN, label).trim().toLowerCase(Locale.ROOT);
        return switch (raw) {
            case "true", "1" -> true;
            case "false", "0" -> false;
            default -> throw new TypeException("{} value type is: {}, invalid boolean value: {}", label, typeCode,
                    value);
        };
    }

    private static String requireValue(String value, ScalarType type, String label) {
        if (Objects.isNull(value) || (type != ScalarType.STRING && StringUtils.isBlank(value))) {
            throw new EmptyException("{} value is empty", label);
        }
        return value;
    }

    private static void requireTarget(ScalarType type, String typeCode, Class<?> targetType, String label) {
        if (Objects.isNull(targetType)) {
            throw new TypeException("{} type is: {}, can't be cast to null class", label, typeCode);
        }
        Class<?> expected = boxedType(type);
        Class<?> primitive = primitiveType(type);
        if (!targetType.equals(expected) && !targetType.equals(primitive)) {
            throw new TypeException("{} type is: {}, can't be cast to class: {}", label, typeCode,
                    targetType.getName());
        }
    }

    private static ScalarType scalarType(AttributeTypeEnum type) {
        return switch (type) {
            case STRING -> ScalarType.STRING;
            case BYTE -> ScalarType.BYTE;
            case SHORT -> ScalarType.SHORT;
            case INT -> ScalarType.INT;
            case LONG -> ScalarType.LONG;
            case FLOAT -> ScalarType.FLOAT;
            case DOUBLE -> ScalarType.DOUBLE;
            case BOOLEAN -> ScalarType.BOOLEAN;
        };
    }

    private static ScalarType scalarType(PointTypeEnum type) {
        return switch (type) {
            case STRING -> ScalarType.STRING;
            case BYTE -> ScalarType.BYTE;
            case SHORT -> ScalarType.SHORT;
            case INT -> ScalarType.INT;
            case LONG -> ScalarType.LONG;
            case FLOAT -> ScalarType.FLOAT;
            case DOUBLE -> ScalarType.DOUBLE;
            case BOOLEAN -> ScalarType.BOOLEAN;
        };
    }

    private static Class<?> boxedType(ScalarType type) {
        return switch (type) {
            case STRING -> String.class;
            case BYTE -> Byte.class;
            case SHORT -> Short.class;
            case INT -> Integer.class;
            case LONG -> Long.class;
            case FLOAT -> Float.class;
            case DOUBLE -> Double.class;
            case BOOLEAN -> Boolean.class;
        };
    }

    private static Class<?> primitiveType(ScalarType type) {
        return switch (type) {
            case STRING -> String.class;
            case BYTE -> Byte.TYPE;
            case SHORT -> Short.TYPE;
            case INT -> Integer.TYPE;
            case LONG -> Long.TYPE;
            case FLOAT -> Float.TYPE;
            case DOUBLE -> Double.TYPE;
            case BOOLEAN -> Boolean.TYPE;
        };
    }

    private static boolean isInteger(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 0;
    }

    /**
     * Set of supported scalar value types handled by the converter.
     */
    private enum ScalarType {
        STRING, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN
    }

}

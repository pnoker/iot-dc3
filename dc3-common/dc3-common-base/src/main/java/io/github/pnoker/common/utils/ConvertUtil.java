/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.utils;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.enums.PointValueTypeEnum;
import io.github.pnoker.common.exception.OutRangeException;
import io.github.pnoker.common.exception.UnsupportException;
import io.github.pnoker.common.model.Point;
import lombok.extern.slf4j.Slf4j;

/**
 * 类型转换相关工具类集合
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class ConvertUtil {

    private ConvertUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 位号数据处理
     * 当出现精度问题，向上调整
     * 例如：byte 类型的数据经过 base 和 multiple 之后超出范围，将其调整为float类型
     *
     * @param point    Point
     * @param rawValue Raw Value
     * @return Value
     */
    public static String convertValue(Point point, String rawValue) {
        PointValueTypeEnum valueType = PointValueTypeEnum.of(point.getType());
        if (ObjectUtil.isNull(valueType)) {
            throw new UnsupportException("Unsupported type of {}", point.getType());
        }

        float base = null != point.getBase() ? point.getBase() : 0;
        float multiple = null != point.getMultiple() ? point.getMultiple() : 1;
        int decimal = null != point.getDecimal() ? point.getDecimal() : 6;

        Object value;
        switch (valueType) {
            case BYTE:
                value = convertByte(rawValue, base, multiple);
                break;
            case SHORT:
                value = convertShort(rawValue, base, multiple);
                break;
            case INT:
                value = convertInteger(rawValue, base, multiple);
                break;
            case LONG:
                value = convertLong(rawValue, base, multiple);
                break;
            case FLOAT:
                value = convertFloat(rawValue, base, multiple, decimal);
                break;
            case DOUBLE:
                value = convertDouble(rawValue, base, multiple, decimal);
                break;
            case BOOLEAN:
                value = convertBoolean(rawValue);
                break;
            default:
                value = rawValue;
                break;
        }

        return String.valueOf(value);
    }

    /**
     * 字符串转短字节值
     * -128 ~ 127
     *
     * @param content 字符串
     * @return short
     */
    private static byte convertByte(String content, float base, float multiple) {
        try {
            byte value = Byte.parseByte(content);
            float v = (value + base) * multiple;
            if (Float.isInfinite(value)) {
                throw new OutRangeException();
            }
            return Byte.parseByte(String.valueOf(v));
        } catch (Exception e) {
            throw new OutRangeException("Out of byte range: {} ~ {}, current: {}", Byte.MIN_VALUE, Byte.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转短整数值
     * -32768 ~ 32767
     *
     * @param content 字符串
     * @return short
     */
    private static short convertShort(String content, float base, float multiple) {
        try {
            short value = Short.parseShort(content);
            float v = (value + base) * multiple;
            if (Float.isInfinite(value)) {
                throw new OutRangeException();
            }
            return Short.parseShort(String.valueOf(v));
        } catch (Exception e) {
            throw new OutRangeException("Out of short range: {} ~ {}, current: {}", Short.MIN_VALUE, Short.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转整数值
     * -2147483648 ~ 2147483647
     *
     * @param content 字符串
     * @return int
     */
    private static int convertInteger(String content, float base, float multiple) {
        try {
            int value = Integer.parseInt(content);
            float v = (value + base) * multiple;
            if (Float.isInfinite(value)) {
                throw new OutRangeException();
            }
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            throw new OutRangeException("Out of int range: {} ~ {}, current: {}", Integer.MIN_VALUE, Integer.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转长整数值
     * -9223372036854775808 ~ 9223372036854775807
     *
     * @param content 字符串
     * @return long
     */
    private static long convertLong(String content, float base, float multiple) {
        try {
            long value = Long.parseLong(content);
            float v = (value + base) * multiple;
            if (Float.isInfinite(value)) {
                throw new OutRangeException();
            }
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            throw new OutRangeException("Out of long range: {} ~ {}, current: {}", Long.MIN_VALUE, Long.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转浮点值
     *
     * @param content 字符串
     * @return float
     */
    private static float convertFloat(String content, float base, float multiple, int decimal) {
        try {
            float value = Float.parseFloat(content);
            value = (value + base) * multiple;
            if (Float.isInfinite(value)) {
                throw new OutRangeException();
            }
            return ArithmeticUtil.round(value, decimal);
        } catch (Exception e) {
            throw new OutRangeException("Out of float range: |{} ~ {}|, current: {}", Float.MIN_VALUE, Float.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转双精度浮点值
     *
     * @param content 字符串
     * @return double
     */
    private static double convertDouble(String content, float base, float multiple, int decimal) {
        try {
            double value = Double.parseDouble(content);
            value = (value + base) * multiple;
            if (Double.isInfinite(value)) {
                throw new OutRangeException();
            }
            return ArithmeticUtil.round(value, decimal);
        } catch (Exception e) {
            throw new OutRangeException("Out of double range: |{} ~ {}|, current: {}", Double.MIN_VALUE, Double.MAX_VALUE, content);
        }
    }

    /**
     * 字符串转布尔值
     *
     * @param content 字符串
     * @return boolean
     */
    private static boolean convertBoolean(String content) {
        return Boolean.parseBoolean(content);
    }

}

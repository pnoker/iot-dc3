/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.driver.entity.bean;

import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.OutRangeException;
import io.github.pnoker.common.utils.ArithmeticUtil;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * 读数据实体类
 *
 * @author pnoker
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
     * 设备
     */
    private DeviceBO device;

    /**
     * 位号
     */
    private PointBO point;

    /**
     * 值, string, 需要根据type确定真实的数据类型
     */
    private String value;

    /**
     * 位号数据处理
     * 当出现精度问题, 向上调整
     * 例如: byte 类型的数据经过 base 和 multiple 之后超出范围, 将其调整为float类型
     *
     * @return 根据位号配置计算返回计算值
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
            case STRING -> value;
            case BYTE -> String.valueOf(getByteValue(value, base, multiple));
            case SHORT -> String.valueOf(getShortValue(value, base, multiple));
            case INT -> String.valueOf(getIntegerValue(value, base, multiple));
            case LONG -> String.valueOf(getLongValue(value, base, multiple));
            case FLOAT -> String.valueOf(getFloatValue(value, base, multiple, decimal));
            case DOUBLE -> String.valueOf(getDoubleValue(value, base, multiple, decimal));
            case BOOLEAN -> String.valueOf(getBooleanValue(value));
        };
    }

    /**
     * 线性函数: y = ax + b
     *
     * @param x A
     * @param b B
     * @param a X
     * @return BigDecimal
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
     * 字符串转短字节值
     * -128 ~ 127
     *
     * @param rawValue 原始值
     * @return short
     */
    private byte getByteValue(String rawValue, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            return multiply.byteValue();
        } catch (Exception e) {
            throw new OutRangeException("Out of byte range: {} ~ {}, current: {}", Byte.MIN_VALUE, Byte.MAX_VALUE, rawValue);
        }
    }

    /**
     * 字符串转短整数值
     * -32768 ~ 32767
     *
     * @param rawValue 原始值
     * @return short
     */
    private short getShortValue(String rawValue, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            return multiply.shortValue();
        } catch (Exception e) {
            throw new OutRangeException("Out of short range: {} ~ {}, current: {}", Short.MIN_VALUE, Short.MAX_VALUE, rawValue);
        }
    }

    /**
     * 字符串转整数值
     * -2147483648 ~ 2147483647
     *
     * @param rawValue 原始值
     * @return int
     */
    private int getIntegerValue(String rawValue, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            return multiply.intValue();
        } catch (Exception e) {
            throw new OutRangeException("Out of int range: {} ~ {}, current: {}", Integer.MIN_VALUE, Integer.MAX_VALUE, rawValue);
        }
    }

    /**
     * 字符串转长整数值
     * -9223372036854775808 ~ 9223372036854775807
     *
     * @param rawValue 原始值
     * @return long
     */
    private long getLongValue(String rawValue, BigDecimal base, BigDecimal multiple) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            return multiply.longValue();
        } catch (Exception e) {
            throw new OutRangeException("Out of long range: {} ~ {}, current: {}", Long.MIN_VALUE, Long.MAX_VALUE, rawValue);
        }
    }

    /**
     * 字符串转浮点值
     *
     * @param rawValue 原始值
     * @return float
     */
    private float getFloatValue(String rawValue, BigDecimal base, BigDecimal multiple, byte decimal) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            if (Float.isInfinite(multiply.floatValue())) {
                throw new OutRangeException();
            }
            return ArithmeticUtil.round(multiply.floatValue(), decimal);
        } catch (Exception e) {
            throw new OutRangeException("Out of float range: |{} ~ {}|, current: {}", Float.MIN_VALUE, Float.MAX_VALUE, rawValue);
        }
    }

    /**
     * 字符串转双精度浮点值
     *
     * @param rawValue 原始值
     * @return double
     */
    private double getDoubleValue(String rawValue, BigDecimal base, BigDecimal multiple, byte decimal) {
        try {
            BigDecimal multiply = getLinearValue(multiple, rawValue, base);
            if (Double.isInfinite(multiply.doubleValue())) {
                throw new OutRangeException();
            }
            return ArithmeticUtil.round(multiply.doubleValue(), decimal);
        } catch (Exception e) {
            throw new OutRangeException("Out of double range: |{} ~ {}|, current: {}", Double.MIN_VALUE, Double.MAX_VALUE, rawValue);
        }
    }

    /**
     * 字符串转布尔值
     *
     * @param rawValue 原始值
     * @return boolean
     */
    private boolean getBooleanValue(String rawValue) {
        return Boolean.parseBoolean(rawValue);
    }

}

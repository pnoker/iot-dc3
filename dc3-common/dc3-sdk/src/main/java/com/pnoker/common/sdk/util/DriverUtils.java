package com.pnoker.common.sdk.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * @author pnoker
 */
@Slf4j
public class DriverUtils {
    /**
     * 参数类型
     */
    public interface ValueType {
        String STRING = "string";
        String INT = "int";
        String DOUBLE = "double";
        String FLOAT = "float";
        String LONG = "long";
        String BOOLEAN = "boolean";
    }

    /**
     * 将字符串数据转换为对应的数据类型
     *
     * @param value
     * @param type  string/int/double/float/long/boolean
     * @return
     */
    public static Object convertValue(String value, String type) {
        value = value.trim();
        switch (type.trim()) {
            case ValueType.INT:
                int intValue = 0;
                try {
                    intValue = Integer.parseInt(value);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                return intValue;
            case ValueType.DOUBLE:
                double doubleValue = 0;
                try {
                    BigDecimal decimal = new BigDecimal(value);
                    doubleValue = decimal.doubleValue();
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                return doubleValue;
            case ValueType.FLOAT:
                float floatValue = 0;
                try {
                    floatValue = Float.parseFloat(value);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                return floatValue;
            case ValueType.LONG:
                long longValue = 0;
                try {
                    longValue = Long.parseLong(value);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                return longValue;
            case ValueType.BOOLEAN:
                boolean booleanValue = false;
                try {
                    booleanValue = Boolean.parseBoolean(value);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                return booleanValue;
            default:
                return value;
        }
    }
}

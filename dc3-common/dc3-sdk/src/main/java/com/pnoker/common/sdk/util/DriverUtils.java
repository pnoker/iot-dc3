package com.pnoker.common.sdk.util;

import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Point;
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
     * 处理数值
     *
     * @param value
     * @param point point.type : string/int/double/float/long/boolean
     * @return
     */
    public static String processValue(String value, Point point) {
        value = value.trim();
        switch (point.getType()) {
            case ValueType.STRING:
                break;
            case ValueType.INT:
            case ValueType.LONG:
                try {
                    value = String.format("%.0f", (getDoubleValue(value) + point.getBase()) * point.getMultiple());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                break;
            case ValueType.DOUBLE:
            case ValueType.FLOAT:
                try {
                    value = String.format(point.getFormat(), (getDoubleValue(value) + point.getBase()) * point.getMultiple());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                break;
            case ValueType.BOOLEAN:
                try {
                    value = String.valueOf(Boolean.parseBoolean(value));
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                break;
            default:
                throw new ServiceException("invalid point value type");
        }
        return value;
    }

    private static double getDoubleValue(String value) {
        BigDecimal decimal = new BigDecimal(value);
        return decimal.doubleValue();
    }
}

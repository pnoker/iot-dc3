package com.pnoker.common.sdk.util;

import cn.hutool.core.convert.Convert;
import com.pnoker.common.constant.Common;
import com.pnoker.common.sdk.bean.AttributeInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
public class Utils {

    /**
     * 获取 属性值
     *
     * @param infoMap
     * @param attribute
     * @param <T>
     * @return
     */
    public static <T> T attribute(Map<String, AttributeInfo> infoMap, String attribute) {
        return convertValue(infoMap.get(attribute).getType(), infoMap.get(attribute).getValue());
    }

    /**
     * 通过类型转换数据
     *
     * @param type
     * @param value
     * @param <T>
     * @return
     */
    public static <T> T convertValue(String type, String value) {
        return Convert.convertByClassName(getTypeClassName(type), value);
    }

    /**
     * 获取基本类型 Class Name
     *
     * @param type
     * @return
     */
    private static String getTypeClassName(String type) {
        String className = "java.lang.String";
        switch (type.toLowerCase()) {
            case Common.ValueType.INT:
                className = "java.lang.Integer";
                break;
            case Common.ValueType.DOUBLE:
                className = "java.lang.Double";
                break;
            case Common.ValueType.FLOAT:
                className = "java.lang.Float";
                break;
            case Common.ValueType.LONG:
                className = "java.lang.Long";
                break;
            case Common.ValueType.BOOLEAN:
                className = "java.lang.Boolean";
                break;
            default:
                break;
        }
        return className;
    }

}

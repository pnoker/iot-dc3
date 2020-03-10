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
public class DriverUtils {

    /**
     * 获取 属性值
     *
     * @param infoMap
     * @param attribute
     * @param <T>
     * @return
     */
    public static <T> T attribute(Map<String, AttributeInfo> infoMap, String attribute) {
        return value(infoMap.get(attribute).getType(), infoMap.get(attribute).getValue());
    }

    /**
     * 通过类型转换数据
     *
     * @param type
     * @param value
     * @param <T>
     * @return
     */
    public static <T> T value(String type, String value) {
        return Convert.convertByClassName(getTypeClassName(type), value);
    }

    /**
     * 将bcd byte[]转成string
     *
     * @param bytes
     * @return
     */
    public static String bcdToString(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            builder.append((byte) ((b & 0xF0) >>> 4));
            builder.append((byte) (b & 0x0F));
        }
        return "0".equalsIgnoreCase(builder.toString().substring(0, 1)) ? builder.toString().substring(1) : builder.toString();
    }

    /**
     * 合并byte[]
     *
     * @param bytes
     * @return
     */
    public static byte[] mergerBytes(byte[]... bytes) {
        int lengthByte = 0;
        for (byte[] value : bytes) {
            lengthByte += value.length;
        }
        byte[] allByte = new byte[lengthByte];
        int countLength = 0;
        for (byte[] b : bytes) {
            System.arraycopy(b, 0, allByte, countLength, b.length);
            countLength += b.length;
        }
        return allByte;
    }

    /**
     * 获取字节间的异或值
     *
     * @param bytes
     * @return
     */
    public static byte xorBytes(byte[]... bytes) {
        byte xor = 0x00;
        for (byte[] value : bytes) {
            for (byte b : value) {
                xor ^= b;
            }
        }
        return xor;
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

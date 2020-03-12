package com.github.pnoker.common.sdk.util;

import cn.hutool.core.convert.Convert;
import com.github.pnoker.common.sdk.bean.AttributeInfo;
import com.github.pnoker.common.constant.Common;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
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
     * 将byte[]转成十六进制字符串
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 将byte[]转成Ascii码
     *
     * @param bytes
     * @return
     */
    public static String bytesToAscii(byte[] bytes) {
        String asciiStr = null;
        try {
            asciiStr = new String(bytes, "ISO8859-1");
        } catch (UnsupportedEncodingException ignored) {
        }
        return asciiStr;
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

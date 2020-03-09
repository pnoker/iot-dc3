package com.pnoker.common.sdk.util;

import cn.hutool.core.convert.Convert;
import com.pnoker.common.constant.Common;
import com.pnoker.common.sdk.bean.AttributeInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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
     * 将byte[]转成string
     *
     * @param bytes
     * @param charset
     * @return
     */
    public static String bytesToString(byte[] bytes, String charset) {
        return new String(bytes, Charset.forName(charset));
    }

    /**
     * 将bcd byte[]转成string
     *
     * @param bytes
     * @return
     */
    public static String bcdToString(byte[] bytes) {
        StringBuilder temp = new StringBuilder(bytes.length * 2);

        for (byte aByte : bytes) {
            temp.append((byte) ((aByte & 0xf0) >>> 4));
            temp.append((byte) (aByte & 0x0f));
        }
        return "0".equalsIgnoreCase(temp.toString().substring(0, 1)) ? temp.toString().substring(1) : temp.toString();
    }

    /**
     * 将byte[]转成Ascii码
     *
     * @param bytes
     * @return
     */
    public static String bytesToAscii(byte[] bytes) {
        return bytesToAscii(bytes, 0, bytes.length);
    }

    /**
     * 以小端模式将byte[]转成short
     *
     * @param bytes
     * @param offset
     * @return
     */
    public static short bytesToLittleShort(byte[] bytes, int offset) {
        short value;
        value = (short) ((bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8));
        return value;
    }

    /**
     * 以大端模式将byte[]转成short
     *
     * @param bytes
     * @param offset
     * @return
     */
    public static short bytesToBigShort(byte[] bytes, int offset) {
        short value;
        value = (short) (((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF));
        return value;
    }

    /**
     * 以小端模式将byte[]转成int
     *
     * @param bytes
     * @param offset
     * @return
     */
    public static int bytesToLittleInt(byte[] bytes, int offset) {
        int value;
        value = (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
        return value;
    }

    /**
     * 以大端模式将byte[]转成int
     *
     * @param bytes
     * @param offset
     * @return
     */
    public static int bytesToBigInt(byte[] bytes, int offset) {
        int value;
        value = ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
        return value;
    }

    /**
     * 将string转成byte[]
     *
     * @param value
     * @param charset
     * @return
     */
    public static byte[] stringToBytes(String value, String charset) {
        return value.getBytes(Charset.forName(charset));
    }

    /**
     * 以小端模式将short转成byte[]
     *
     * @param value
     * @return
     */
    public static byte[] shortToLittleBytes(short value) {
        byte[] src = new byte[2];
        src[0] = (byte) (value & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        return src;
    }

    /**
     * 以大端模式将short转成byte[]
     *
     * @param value
     * @return
     */
    public static byte[] shortToBigBytes(short value) {
        byte[] src = new byte[2];
        src[0] = (byte) ((value >> 8) & 0xFF);
        src[1] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 以小端模式将int转成byte[]
     *
     * @param value
     * @return
     */
    public static byte[] intToLittleBytes(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) (value & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[3] = (byte) ((value >> 24) & 0xFF);
        return src;
    }

    /**
     * 以大端模式将int转成byte[]
     *
     * @param value
     * @return
     */
    public static byte[] intToBigBytes(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
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

    /**
     * bytes 转 Ascii
     *
     * @param bytes
     * @param offset
     * @param dateLen
     * @return
     */
    private static String bytesToAscii(byte[] bytes, int offset, int dateLen) {
        if ((bytes == null) || (bytes.length == 0) || (offset < 0) || (dateLen <= 0)) {
            return null;
        }
        if ((offset >= bytes.length) || (bytes.length - offset < dateLen)) {
            return null;
        }

        String asciiStr = null;
        byte[] data = new byte[dateLen];
        System.arraycopy(bytes, offset, data, 0, dateLen);
        try {
            asciiStr = new String(data, "ISO8859-1");
        } catch (UnsupportedEncodingException e) {
        }
        return asciiStr;
    }

}

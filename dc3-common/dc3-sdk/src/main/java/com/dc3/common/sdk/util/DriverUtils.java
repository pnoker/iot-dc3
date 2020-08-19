/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.sdk.util;

import cn.hutool.core.convert.Convert;
import com.dc3.common.constant.Common;
import com.dc3.common.sdk.bean.AttributeInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
public class DriverUtils {

    /**
     * 获取 属性值
     *
     * @param infoMap   Attribute Info
     * @param attribute String Attribute Name
     * @param <T>       T
     * @return T
     */
    public static <T> T attribute(Map<String, AttributeInfo> infoMap, String attribute) {
        return value(infoMap.get(attribute).getType(), infoMap.get(attribute).getValue());
    }

    /**
     * 通过类型转换数据
     *
     * @param type  String Type, short/int/long/float/double/boolean/string
     * @param value String Value
     * @param <T>   T
     * @return T
     */
    public static <T> T value(String type, String value) {
        return Convert.convertByClassName(getTypeClassName(type), value);
    }

    /**
     * Base 64 解码
     *
     * @param content string
     * @return string
     */
    public static String base64Encode(String content) {
        byte[] bytes = content.getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Base 64 编码
     *
     * @param content string
     * @return string
     */
    public static String base64Decode(String content) {
        byte[] bytes = content.getBytes();
        return new String(Base64.getDecoder().decode(bytes));
    }

    /**
     * 将byte[]转成十六进制字符串
     *
     * @param bytes Byte Array
     * @return String
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
     * @param bytes Byte Array
     * @return String
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
     * @param bytes Byte Array
     * @return Byte Array
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
     * @param bytes Byte Array
     * @return Byte
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
     * @param type String Type, short/int/long/float/double/boolean/string
     * @return Class Name
     */
    public static String getTypeClassName(String type) {
        String className = "java.lang.String";
        switch (type.toLowerCase()) {
            case Common.ValueType.SHORT:
                className = "java.lang.Short";
                break;
            case Common.ValueType.INT:
                className = "java.lang.Integer";
                break;
            case Common.ValueType.LONG:
                className = "java.lang.Long";
                break;
            case Common.ValueType.FLOAT:
                className = "java.lang.Float";
                break;
            case Common.ValueType.DOUBLE:
                className = "java.lang.Double";
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

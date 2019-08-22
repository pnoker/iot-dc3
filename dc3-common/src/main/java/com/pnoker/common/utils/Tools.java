/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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
package com.pnoker.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 工具类
 */
public class Tools {
    /**
     * 将字符串进行Base64编码
     *
     * @param str
     * @return 返回字节流
     */
    public static byte[] encode(String str) {
        return Base64.getEncoder().encode(str.getBytes(Charsets.UTF_8));
    }

    /**
     * 将字节流进行Base64编码
     *
     * @param bytes
     * @return 返回字符串
     */
    public static String encodeToString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 必须配合encode使用，用于encode编码之后解码
     *
     * @param str 字符串
     * @return 返回字节流
     */
    public static byte[] decode(String str) {
        return Base64.getDecoder().decode(str);
    }

    /**
     * 必须配合encode使用，用于encode编码之后解码
     *
     * @param input 字节流
     * @return 返回字节流
     */
    public static byte[] decode(byte[] input) {
        return Base64.getDecoder().decode(input);
    }

    public static boolean ping(String host) throws IOException {
        return InetAddress.getByName(host).isReachable(3000);
    }

    /**
     * 判断是否为文件 或 文件是否存在
     *
     * @param path
     * @return
     */
    public static boolean isFile(String path) {
        File file = new File(path);
        if (file.isFile()) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为文件目录 或 文件目录是否存在
     *
     * @param path
     * @return
     */
    public static boolean isDirectory(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            return true;
        }
        return false;
    }

    /**
     * 正则判断
     *
     * @param str
     * @param regex
     * @return
     */
    private static boolean pattern(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    /**
     * 判断字符串是否为 Json格式
     *
     * @param json
     * @return
     */
    public static boolean isJson(String json) {
        try {
            return JSONObject.isValid(json);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 Int
     *
     * @param number
     * @return
     */
    public static boolean isInt(String number) {
        String regex = "^-?[0-9]*$";
        return pattern(number, regex);
    }

    /**
     * 判断字符串是否为 Double
     *
     * @param number
     * @return
     */
    public static boolean isDouble(String number) {
        String regex = "^-?[0-9]*.?[0-9]*$";
        return pattern(number, regex);
    }

    /**
     * 判断字符串是否为 手机号码格式
     *
     * @param phone
     * @return
     */
    public static boolean isPhone(String phone) {
        String regex = "^[1][3-9]+\\d{9}$";
        return pattern(phone, regex);
    }

    /**
     * 判断字符串是否为 邮箱地址格式
     *
     * @param mail
     * @return
     */
    public static boolean isMail(String mail) {
        String regex = "^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$";
        return pattern(mail, regex);
    }

}

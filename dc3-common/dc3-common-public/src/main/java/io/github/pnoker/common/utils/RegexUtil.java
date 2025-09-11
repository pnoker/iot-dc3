/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * 常用正则工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class RegexUtil {

    private RegexUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 判断字符串是否为 数字格式
     *
     * @param content 字符串
     * @return boolean
     */
    public static boolean isNumeric(String content) {
        if (StringUtils.isEmpty(content)) return false;
        try {
            new BigDecimal(content);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 名称格式(2-32)
     *
     * @param name String
     * @return boolean
     */
    public static boolean isName(String name) {
        String regex = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$";
        return name.matches(regex);
    }

    /**
     * 判断字符串是否为 手机号码格式
     *
     * @param phone String
     * @return boolean
     */
    public static boolean isPhone(String phone) {
        String regex = "^1([3-9])\\d{9}$";
        return phone.matches(regex);
    }

    /**
     * 判断字符串是否为 邮箱地址格式
     *
     * @param mail String
     * @return boolean
     */
    public static boolean isMail(String mail) {
        String regex = "^[A-Za-z0-9_.-]+@[A-Za-z0-9]+\\.[A-Za-z0-9]+$";
        return mail.matches(regex);
    }

    /**
     * 判断字符串是否为 密码格式(8-16)
     *
     * @param password String
     * @return boolean
     */
    public static boolean isPassword(String password) {
        String regex = "^[a-zA-Z]\\w{7,15}$";
        return password.matches(regex);
    }

    /**
     * 判断字符串是否为 Host格式
     *
     * @param host String
     * @return boolean
     */
    public static boolean isHost(String host) {
        String regex = "^((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}$";
        return host.matches(regex);
    }

    /**
     * 判断字符串是否为 驱动端口格式
     *
     * @param port Integer
     * @return boolean
     */
    public static boolean isDriverPort(int port) {
        return port >= 8600 && port <= 8799;
    }
}

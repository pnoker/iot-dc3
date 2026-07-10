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
 * Common regex-based string validation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class RegexUtil {

    private RegexUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Check if string is in numeric format
     *
     * @param content String to check
     * @return true if numeric, false otherwise
     */
    public static boolean isNumeric(String content) {
        if (StringUtils.isEmpty(content)) {
            return false;
        }
        try {
            new BigDecimal(content);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Check if string is in name format (2-32 characters)
     *
     * @param name String to check
     * @return true if valid name format, false otherwise
     */
    public static boolean isName(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        String regex = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$";
        return name.matches(regex);
    }

    /**
     * Check if string is in mobile phone format
     *
     * @param phone Phone number string to check
     * @return true if valid mobile format, false otherwise
     */
    public static boolean isPhone(String phone) {
        if (StringUtils.isEmpty(phone)) {
            return false;
        }
        String regex = "^1([3-9])\\d{9}$";
        return phone.matches(regex);
    }

    /**
     * Check if string is in email address format
     *
     * @param mail Email address to check
     * @return true if valid email format, false otherwise
     */
    public static boolean isMail(String mail) {
        if (StringUtils.isEmpty(mail)) {
            return false;
        }
        String regex = "^[A-Za-z0-9_.-]+@[A-Za-z0-9]+\\.[A-Za-z0-9]+$";
        return mail.matches(regex);
    }

    /**
     * Check if string is in password format (8-16 characters)
     *
     * @param password Password string to check
     * @return true if valid password format, false otherwise
     */
    public static boolean isPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        String regex = "^[a-zA-Z]\\w{7,15}$";
        return password.matches(regex);
    }

    /**
     * Check if string is in host format
     *
     * @param host Host string to check
     * @return true if valid host format, false otherwise
     */
    public static boolean isHost(String host) {
        if (StringUtils.isEmpty(host)) {
            return false;
        }
        String regex = "^((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}$";
        return host.matches(regex);
    }

    /**
     * Check if port is in valid driver port range
     *
     * @param port Port number to check
     * @return true if valid driver port (8600-8799), false otherwise
     */
    public static boolean isDriverPort(int port) {
        return port >= 8600 && port <= 8799;
    }

}

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
import io.github.pnoker.common.constant.common.TimeConstant;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 时间(不时区, 默认上海时区) 相关工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class LocalDateTimeUtil {

    private LocalDateTimeUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取默认的时间格式
     * <p>
     * yyyy-MM-dd HH:mm:ss
     *
     * @return {@link DateTimeFormatter}
     */
    public static DateTimeFormatter getDefaultDateTimeFormatter() {
        return DateTimeFormatter.ofPattern(TimeConstant.DEFAULT_DATE_FORMAT).withZone(TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * 获取完整的时间格式
     * <p>
     * yyyy-MM-dd HH:mm:ss.SSS
     *
     * @return {@link DateTimeFormatter}
     */
    public static DateTimeFormatter getCompleteDateTimeFormatter() {
        return DateTimeFormatter.ofPattern(TimeConstant.COMPLETE_DATE_FORMAT).withZone(TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * 获取当前时间
     *
     * @return LocalDateTime {@link LocalDateTime}
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * 获取毫秒
     *
     * @param localDateTime {@link LocalDateTime}
     * @return 毫秒
     */
    public static long milliSeconds(LocalDateTime localDateTime) {
        return localDateTime.atZone(TimeConstant.DEFAULT_ZONEID).toInstant().toEpochMilli();
    }

    /**
     * 获取 DateTime
     *
     * @param milliSeconds 毫秒
     * @return LocalDateTime {@link LocalDateTime}
     */
    public static LocalDateTime dateTime(long milliSeconds) {
        Instant instant = Instant.ofEpochMilli(milliSeconds);
        return LocalDateTime.ofInstant(instant, TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * 推迟时间 HOUR/MINUTE/...
     *
     * @param amount Integer
     * @param field  ChronoUnit field : {@link ChronoUnit ChronoUnit.HOUR/MINUTE/...}
     * @return Date
     */
    public static LocalDateTime expireTime(int amount, ChronoUnit field) {
        LocalDateTime localDateTime = LocalDateTimeUtil.now();
        return localDateTime.plus(amount, field);
    }

    /**
     * 使用 yyyy-MM-dd HH:mm:ss 格式化时间
     *
     * @param localDateTime {@link LocalDateTime}
     * @return R of String
     */
    public static String defaultFormat(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = getDefaultDateTimeFormatter();
        return localDateTime.format(formatter);
    }

    /**
     * 使用 yyyy-MM-dd HH:mm:ss.SSS 格式化时间
     *
     * @param localDateTime {@link LocalDateTime}
     * @return R of String
     */
    public static String completeFormat(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = getCompleteDateTimeFormatter();
        return localDateTime.format(formatter);
    }

    /**
     * 将时间字符串 yyyy-MM-dd HH:mm:ss 转为时间类型
     *
     * @param dateString yyyy-MM-dd HH:mm:ss
     * @return Date
     */
    public static LocalDateTime defaultDate(String dateString) {
        try {
            DateTimeFormatter formatter = getDefaultDateTimeFormatter();
            return LocalDateTime.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 将时间字符串 yyyy-MM-dd HH:mm:ss.SSS
     *
     * @param dateString Date String
     * @return Date
     */
    public static LocalDateTime completeDate(String dateString) {
        try {
            DateTimeFormatter formatter = getCompleteDateTimeFormatter();
            return LocalDateTime.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

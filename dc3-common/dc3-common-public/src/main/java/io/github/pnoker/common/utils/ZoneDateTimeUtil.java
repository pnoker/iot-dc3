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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 时间(带时区, 默认上海时区) 相关工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class ZoneDateTimeUtil {

    private ZoneDateTimeUtil() {
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
     * @return ZonedDateTime {@link ZonedDateTime}
     */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * 获取毫秒
     *
     * @param zonedDateTime {@link ZonedDateTime}
     * @return 毫秒
     */
    public static long milliSeconds(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * 获取 DateTime
     *
     * @param milliSeconds 毫秒
     * @return ZonedDateTime {@link ZonedDateTime}
     */
    public static ZonedDateTime dateTime(long milliSeconds) {
        Instant instant = Instant.ofEpochMilli(milliSeconds);
        return ZonedDateTime.ofInstant(instant, TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * 推迟时间 HOUR/MINUTE/...
     *
     * @param amount Integer
     * @param field  ChronoUnit field : {@link ChronoUnit ChronoUnit.HOUR/MINUTE/...}
     * @return Date
     */
    public static ZonedDateTime expireTime(int amount, ChronoUnit field) {
        ZonedDateTime zonedDateTime = ZoneDateTimeUtil.now();
        return zonedDateTime.plus(amount, field);
    }

    /**
     * 使用 yyyy-MM-dd HH:mm:ss 格式化时间
     *
     * @param zonedDateTime {@link ZonedDateTime}
     * @return R of String
     */
    public static String defaultFormat(ZonedDateTime zonedDateTime) {
        DateTimeFormatter formatter = getDefaultDateTimeFormatter();
        return zonedDateTime.format(formatter);
    }

    /**
     * 使用 yyyy-MM-dd HH:mm:ss.SSS 格式化时间
     *
     * @param zonedDateTime {@link ZonedDateTime}
     * @return R of String
     */
    public static String completeFormat(ZonedDateTime zonedDateTime) {
        DateTimeFormatter formatter = getCompleteDateTimeFormatter();
        return zonedDateTime.format(formatter);
    }

    /**
     * 将时间字符串 yyyy-MM-dd HH:mm:ss 转为时间类型
     *
     * @param dateString yyyy-MM-dd HH:mm:ss
     * @return Date
     */
    public static ZonedDateTime defaultDate(String dateString) {
        try {
            DateTimeFormatter formatter = getDefaultDateTimeFormatter();
            return ZonedDateTime.parse(dateString, formatter);
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
    public static ZonedDateTime completeDate(String dateString) {
        try {
            DateTimeFormatter formatter = getCompleteDateTimeFormatter();
            return ZonedDateTime.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

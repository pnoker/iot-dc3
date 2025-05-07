/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @version 2025.2.5
 * @since 2022.1.0
 */
@Slf4j
public class ZonedDateTimeUtil {

    private ZonedDateTimeUtil() {
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
        ZonedDateTime zonedDateTime = ZonedDateTime.now(TimeConstant.DEFAULT_ZONEID);
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

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 时间 相关工具类
 *
 * @author pnoker
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
     * 获取毫秒
     *
     * @param localDateTime {@link LocalDateTime}
     * @return 毫秒
     */
    public static long milliSeconds(LocalDateTime localDateTime) {
        return localDateTime.atZone(TimeConstant.DEFAULT_ZONEID).toInstant().toEpochMilli();
    }

    /**
     * 获取 LocalDateTime
     *
     * @param milliSeconds 毫秒
     * @return LocalDateTime {@link LocalDateTime}
     */
    public static LocalDateTime localDateTime(long milliSeconds) {
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
        LocalDateTime localDateTime = LocalDateTime.now();
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

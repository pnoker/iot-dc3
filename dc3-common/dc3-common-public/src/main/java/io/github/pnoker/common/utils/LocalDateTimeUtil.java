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
 * Local date-time utility class (without time zone, default Shanghai time zone).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class LocalDateTimeUtil {

    private LocalDateTimeUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get the default date-time format.
     * <p>
     * Pattern: yyyy-MM-dd HH:mm:ss
     *
     * @return {@link DateTimeFormatter}
     */
    public static DateTimeFormatter getDefaultDateTimeFormatter() {
        return DateTimeFormatter.ofPattern(TimeConstant.DEFAULT_DATE_FORMAT).withZone(TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * Get the complete date-time format.
     * <p>
     * Pattern: yyyy-MM-dd HH:mm:ss.SSS
     *
     * @return {@link DateTimeFormatter}
     */
    public static DateTimeFormatter getCompleteDateTimeFormatter() {
        return DateTimeFormatter.ofPattern(TimeConstant.COMPLETE_DATE_FORMAT).withZone(TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * Get the current time with default time zone.
     *
     * @return LocalDateTime {@link LocalDateTime}
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * Get milliseconds from the given {@link LocalDateTime}.
     *
     * @param localDateTime {@link LocalDateTime}
     * @return Milliseconds
     */
    public static long milliSeconds(LocalDateTime localDateTime) {
        return localDateTime.atZone(TimeConstant.DEFAULT_ZONEID).toInstant().toEpochMilli();
    }

    /**
     * Get a {@link LocalDateTime} from milliseconds.
     *
     * @param milliSeconds Milliseconds
     * @return LocalDateTime {@link LocalDateTime}
     */
    public static LocalDateTime dateTime(long milliSeconds) {
        Instant instant = Instant.ofEpochMilli(milliSeconds);
        return LocalDateTime.ofInstant(instant, TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * Delay time by the specified amount and unit, e.g., HOUR/MINUTE/...
     *
     * @param amount Integer
     * @param field  ChronoUnit field : {@link ChronoUnit ChronoUnit.HOUR/MINUTE/...}
     * @return LocalDateTime
     */
    public static LocalDateTime expireTime(int amount, ChronoUnit field) {
        LocalDateTime localDateTime = LocalDateTimeUtil.now();
        return localDateTime.plus(amount, field);
    }

    /**
     * Format time using the pattern yyyy-MM-dd HH:mm:ss.
     *
     * @param localDateTime {@link LocalDateTime}
     * @return Formatted time string
     */
    public static String defaultFormat(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = getDefaultDateTimeFormatter();
        return localDateTime.format(formatter);
    }

    /**
     * Format time using the pattern yyyy-MM-dd HH:mm:ss.SSS.
     *
     * @param localDateTime {@link LocalDateTime}
     * @return Formatted time string
     */
    public static String completeFormat(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = getCompleteDateTimeFormatter();
        return localDateTime.format(formatter);
    }

    /**
     * Parse a time string in the pattern yyyy-MM-dd HH:mm:ss to {@link LocalDateTime}.
     *
     * @param dateString yyyy-MM-dd HH:mm:ss
     * @return LocalDateTime or {@code null} if parsing fails
     */
    public static LocalDateTime defaultDate(String dateString) {
        try {
            DateTimeFormatter formatter = getDefaultDateTimeFormatter();
            return LocalDateTime.parse(dateString, formatter);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * Parse a time string in the pattern yyyy-MM-dd HH:mm:ss.SSS to
     * {@link LocalDateTime}.
     *
     * @param dateString Date string
     * @return LocalDateTime or {@code null} if parsing fails
     */
    public static LocalDateTime completeDate(String dateString) {
        try {
            DateTimeFormatter formatter = getCompleteDateTimeFormatter();
            return LocalDateTime.parse(dateString, formatter);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

}

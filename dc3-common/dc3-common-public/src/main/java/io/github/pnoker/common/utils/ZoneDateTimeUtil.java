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
 * Zoned date-time utility class (with time zone, default Shanghai time zone).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class ZoneDateTimeUtil {

    private ZoneDateTimeUtil() {
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
     * @return ZonedDateTime {@link ZonedDateTime}
     */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * Get milliseconds from the given {@link ZonedDateTime}.
     *
     * @param zonedDateTime {@link ZonedDateTime}
     * @return Milliseconds
     */
    public static long milliSeconds(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * Get a {@link ZonedDateTime} from milliseconds.
     *
     * @param milliSeconds Milliseconds
     * @return ZonedDateTime {@link ZonedDateTime}
     */
    public static ZonedDateTime dateTime(long milliSeconds) {
        Instant instant = Instant.ofEpochMilli(milliSeconds);
        return ZonedDateTime.ofInstant(instant, TimeConstant.DEFAULT_ZONEID);
    }

    /**
     * Delay time by the specified amount and unit, e.g., HOUR/MINUTE/...
     *
     * @param amount Integer
     * @param field  ChronoUnit field : {@link ChronoUnit ChronoUnit.HOUR/MINUTE/...}
     * @return ZonedDateTime
     */
    public static ZonedDateTime expireTime(int amount, ChronoUnit field) {
        ZonedDateTime zonedDateTime = ZoneDateTimeUtil.now();
        return zonedDateTime.plus(amount, field);
    }

    /**
     * Format time using the pattern yyyy-MM-dd HH:mm:ss.
     *
     * @param zonedDateTime {@link ZonedDateTime}
     * @return Formatted time string
     */
    public static String defaultFormat(ZonedDateTime zonedDateTime) {
        DateTimeFormatter formatter = getDefaultDateTimeFormatter();
        return zonedDateTime.format(formatter);
    }

    /**
     * Format time using the pattern yyyy-MM-dd HH:mm:ss.SSS.
     *
     * @param zonedDateTime {@link ZonedDateTime}
     * @return Formatted time string
     */
    public static String completeFormat(ZonedDateTime zonedDateTime) {
        DateTimeFormatter formatter = getCompleteDateTimeFormatter();
        return zonedDateTime.format(formatter);
    }

    /**
     * Parse a time string in the pattern yyyy-MM-dd HH:mm:ss to {@link ZonedDateTime}.
     *
     * @param dateString yyyy-MM-dd HH:mm:ss
     * @return ZonedDateTime or {@code null} if parsing fails
     */
    public static ZonedDateTime defaultDate(String dateString) {
        try {
            DateTimeFormatter formatter = getDefaultDateTimeFormatter();
            return ZonedDateTime.parse(dateString, formatter);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * Parse a time string in the pattern yyyy-MM-dd HH:mm:ss.SSS to
     * {@link ZonedDateTime}.
     *
     * @param dateString Date string
     * @return ZonedDateTime or {@code null} if parsing fails
     */
    public static ZonedDateTime completeDate(String dateString) {
        try {
            DateTimeFormatter formatter = getCompleteDateTimeFormatter();
            return ZonedDateTime.parse(dateString, formatter);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

}

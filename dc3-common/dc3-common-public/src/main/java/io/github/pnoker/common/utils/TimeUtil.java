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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Legacy {@link Date} formatting and time calculation with thread-safe formatters.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class TimeUtil {

    /**
     * ThreadLocal SimpleDateFormat to ensure thread safety
     */
    private static final ThreadLocal<SimpleDateFormat> DEFAULT_DATE_FORMAT_THREAD_LOCAL = ThreadLocal
            .withInitial(() -> new SimpleDateFormat(TimeConstant.DEFAULT_DATE_FORMAT));

    private static final ThreadLocal<SimpleDateFormat> COMPLETE_DATE_FORMAT_THREAD_LOCAL = ThreadLocal
            .withInitial(() -> new SimpleDateFormat(TimeConstant.COMPLETE_DATE_FORMAT));

    private TimeUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get milliseconds from Date
     *
     * @param date {@link Date}
     * @return Milliseconds since epoch
     */
    public static long milliSeconds(Date date) {
        return date.getTime();
    }

    /**
     * Get Date from milliseconds
     *
     * @param milliSeconds Milliseconds since epoch
     * @return Date {@link Date}
     */
    public static Date localDateTime(long milliSeconds) {
        return new Date(milliSeconds);
    }

    /**
     * Calculate expire time by adding amount to specified calendar field
     *
     * @param amount Integer amount to add
     * @param field  Calendar field : {@link Calendar Calendar.HOUR/MINUTE/...}
     * @return Date with calculated expire time
     */
    public static Date expireTime(int amount, int field) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(field, amount);
        return calendar.getTime();
    }

    /**
     * Format date using yyyy-MM-dd HH:mm:ss pattern
     *
     * @param date {@link Date}
     * @return Formatted date string
     */
    public static String defaultFormat(Date date) {
        return DEFAULT_DATE_FORMAT_THREAD_LOCAL.get().format(date);
    }

    /**
     * Format date using yyyy-MM-dd HH:mm:ss.SSS pattern
     *
     * @param date {@link Date}
     * @return Formatted date string with milliseconds
     */
    public static String completeFormat(Date date) {
        return COMPLETE_DATE_FORMAT_THREAD_LOCAL.get().format(date);
    }

    /**
     * Parse date string yyyy-MM-dd HH:mm:ss to Date
     *
     * @param dateString Date string in yyyy-MM-dd HH:mm:ss format
     * @return Date object
     */
    public static Date defaultDate(String dateString) {
        try {
            return DEFAULT_DATE_FORMAT_THREAD_LOCAL.get().parse(dateString);
        } catch (ParseException e) {
            log.debug("Failed to parse date string '{}' with default format", dateString);
            return null;
        }
    }

    /**
     * Parse date string yyyy-MM-dd HH:mm:ss.SSS to Date
     *
     * @param dateString Date string in yyyy-MM-dd HH:mm:ss.SSS format
     * @return Date object
     */
    public static Date completeDate(String dateString) {
        try {
            return COMPLETE_DATE_FORMAT_THREAD_LOCAL.get().parse(dateString);
        } catch (ParseException e) {
            log.debug("Failed to parse date string '{}' with complete format", dateString);
            return null;
        }
    }

    /**
     * Remove the current thread's values from the ThreadLocal variables.
     */
    public static void clean() {
        DEFAULT_DATE_FORMAT_THREAD_LOCAL.remove();
        COMPLETE_DATE_FORMAT_THREAD_LOCAL.remove();
    }

}

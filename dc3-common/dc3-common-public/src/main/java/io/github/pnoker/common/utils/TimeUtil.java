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
 * 时间 相关工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class TimeUtil {

    /**
     * SimpleDateFormat ThreadLocal 保证线程安全
     */
    private static final ThreadLocal<SimpleDateFormat> DEFAULT_DATE_FORMAT_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SimpleDateFormat(TimeConstant.DEFAULT_DATE_FORMAT));
    private static final ThreadLocal<SimpleDateFormat> COMPLETE_DATE_FORMAT_THREAD_LOCAL = ThreadLocal.withInitial(() -> new SimpleDateFormat(TimeConstant.COMPLETE_DATE_FORMAT));

    private TimeUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取毫秒
     *
     * @param date {@link Date}
     * @return 毫秒
     */
    public static long milliSeconds(Date date) {
        return date.getTime();
    }

    /**
     * 获取 Date
     *
     * @param milliSeconds 毫秒
     * @return Date {@link Date}
     */
    public static Date localDateTime(long milliSeconds) {
        return new Date(milliSeconds);
    }

    /**
     * 推迟时间 HOUR/MINUTE/...
     *
     * @param amount Integer
     * @param field  Calendar field : {@link Calendar Calendar.HOUR/MINUTE/...}
     * @return Date
     */
    public static Date expireTime(int amount, int field) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(field, amount);
        return calendar.getTime();
    }

    /**
     * 使用 yyyy-MM-dd HH:mm:ss 格式化时间
     *
     * @param date {@link Date}
     * @return R of String
     */
    public static String defaultFormat(Date date) {
        return DEFAULT_DATE_FORMAT_THREAD_LOCAL.get().format(date);
    }

    /**
     * 使用 yyyy-MM-dd HH:mm:ss.SSS 格式化时间
     *
     * @param date {@link Date}
     * @return R of String
     */
    public static String completeFormat(Date date) {
        return COMPLETE_DATE_FORMAT_THREAD_LOCAL.get().format(date);
    }

    /**
     * 将时间字符串 yyyy-MM-dd HH:mm:ss 转为时间类型
     *
     * @param dateString yyyy-MM-dd HH:mm:ss
     * @return Date
     */
    public static Date defaultDate(String dateString) {
        try {
            return DEFAULT_DATE_FORMAT_THREAD_LOCAL.get().parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 将时间字符串 yyyy-MM-dd HH:mm:ss.SSS 转为时间类型
     *
     * @param dateString yyyy-MM-dd HH:mm:ss.SSS
     * @return Date
     */
    public static Date completeDate(String dateString) {
        try {
            return COMPLETE_DATE_FORMAT_THREAD_LOCAL.get().parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 删除此线程局部变量的当前线程值
     */
    public static void clean() {
        DEFAULT_DATE_FORMAT_THREAD_LOCAL.remove();
        COMPLETE_DATE_FORMAT_THREAD_LOCAL.remove();
    }

}

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

package io.github.pnoker.common.constant.common;

import java.time.ZoneId;

/**
 * 时间 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class TimeConstant {

    /**
     * 时区, 默认为 Asia/Shanghai
     */
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    /**
     * 时区ID, 默认为 Asia/Shanghai
     */
    public static final ZoneId DEFAULT_ZONEID = ZoneId.of(DEFAULT_TIMEZONE);
    /**
     * 时间格式化
     */
    public static final String DAY_DATE_FORMAT1 = "yyyy.MM.dd";
    public static final String DAY_DATE_FORMAT2 = "yyyy-MM-dd";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String COMPLETE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private TimeConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}

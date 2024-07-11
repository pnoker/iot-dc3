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

package io.github.pnoker.common.constant.common;

import java.time.ZoneId;

/**
 * 时间 相关常量
 *
 * @author pnoker
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
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String COMPLETE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private TimeConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}

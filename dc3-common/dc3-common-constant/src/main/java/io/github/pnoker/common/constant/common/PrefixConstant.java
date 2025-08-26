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

/**
 * 前缀 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class PrefixConstant {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String ADD = "add";
    public static final String DELETE = "delete";
    public static final String UPDATE = "update";
    public static final String TENANT = "tenant";
    public static final String TENANT_BIND = "tenant_bind";
    public static final String USER = "user";
    public static final String DRIVER = "driver";
    public static final String DRIVER_ATTRIBUTE = "driver_attribute";
    public static final String DRIVER_ATTRIBUTE_CONFIG = "driver_attribute_config";
    public static final String PROFILE = "profile";
    public static final String PROFILE_BIND = "profile_bind";
    public static final String POINT = "point";
    public static final String POINT_ATTRIBUTE = "point_attribute";
    public static final String POINT_ATTRIBUTE_CONFIG = "point_attribute_config";
    public static final String DEVICE = "device";
    public static final String GROUP = "group";
    public static final String LABEL = "label";
    public static final String LABEL_BIND = "label_bind";
    public static final String DATA_STATISTICS = "data_statistics";
    public static final String REAL_TIME_VALUE_KEY_PREFIX = POINT + SuffixConstant.VALUE + SymbolConstant.COLON;
    public static final String DRIVER_STATUS_KEY_PREFIX = DRIVER + SuffixConstant.STATUS + SymbolConstant.COLON;
    public static final String DEVICE_STATUS_KEY_PREFIX = DEVICE + SuffixConstant.STATUS + SymbolConstant.COLON;

    private PrefixConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}

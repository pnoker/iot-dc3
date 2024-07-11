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

/**
 * 前缀 相关常量
 *
 * @author pnoker
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
    public static final String LIMITED_IP = "limited_ip";
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

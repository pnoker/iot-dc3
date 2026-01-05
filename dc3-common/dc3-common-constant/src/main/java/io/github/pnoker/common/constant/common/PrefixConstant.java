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
 * Prefix related constants
 * <p>
 * Provides prefix constants used for constructing cache keys, URL paths, and other identifiers.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public class PrefixConstant {

    /**
     * HTTP protocol prefix: "http"
     */
    public static final String HTTP = "http";
    /**
     * HTTPS protocol prefix: "https"
     */
    public static final String HTTPS = "https";
    /**
     * Add operation prefix: "add"
     */
    public static final String ADD = "add";
    /**
     * Delete operation prefix: "delete"
     */
    public static final String DELETE = "delete";
    /**
     * Update operation prefix: "update"
     */
    public static final String UPDATE = "update";
    /**
     * Tenant prefix: "tenant"
     */
    public static final String TENANT = "tenant";
    /**
     * Tenant bind prefix: "tenant_bind"
     */
    public static final String TENANT_BIND = "tenant_bind";
    /**
     * User prefix: "user"
     */
    public static final String USER = "user";
    /**
     * Driver prefix: "driver"
     */
    public static final String DRIVER = "driver";
    /**
     * Driver attribute prefix: "driver_attribute"
     */
    public static final String DRIVER_ATTRIBUTE = "driver_attribute";
    /**
     * Driver attribute config prefix: "driver_attribute_config"
     */
    public static final String DRIVER_ATTRIBUTE_CONFIG = "driver_attribute_config";
    /**
     * Profile prefix: "profile"
     */
    public static final String PROFILE = "profile";
    /**
     * Profile bind prefix: "profile_bind"
     */
    public static final String PROFILE_BIND = "profile_bind";
    /**
     * Point prefix: "point"
     */
    public static final String POINT = "point";
    /**
     * Point attribute prefix: "point_attribute"
     */
    public static final String POINT_ATTRIBUTE = "point_attribute";
    /**
     * Point attribute config prefix: "point_attribute_config"
     */
    public static final String POINT_ATTRIBUTE_CONFIG = "point_attribute_config";
    /**
     * Device prefix: "device"
     */
    public static final String DEVICE = "device";
    /**
     * Group prefix: "group"
     */
    public static final String GROUP = "group";
    /**
     * Label prefix: "label"
     */
    public static final String LABEL = "label";
    /**
     * Label bind prefix: "label_bind"
     */
    public static final String LABEL_BIND = "label_bind";
    /**
     * Data statistics prefix: "data_statistics"
     */
    public static final String DATA_STATISTICS = "data_statistics";
    /**
     * Real-time value cache key prefix: "point_value:"
     */
    public static final String REAL_TIME_VALUE_KEY_PREFIX = POINT + SuffixConstant.VALUE + SymbolConstant.COLON;
    /**
     * Driver status cache key prefix: "driver_status:"
     */
    public static final String DRIVER_STATUS_KEY_PREFIX = DRIVER + SuffixConstant.STATUS + SymbolConstant.COLON;
    /**
     * Device status cache key prefix: "device_status:"
     */
    public static final String DEVICE_STATUS_KEY_PREFIX = DEVICE + SuffixConstant.STATUS + SymbolConstant.COLON;

    private PrefixConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}

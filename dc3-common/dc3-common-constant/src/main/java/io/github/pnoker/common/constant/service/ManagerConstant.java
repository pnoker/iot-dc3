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

package io.github.pnoker.common.constant.service;

import io.github.pnoker.common.constant.common.BaseConstant;


/**
 * Constants for the manager service module.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public class ManagerConstant {

    /**
     * Service name
     */
    public static final String SERVICE_NAME = "dc3-center-manager";

    /**
     * URL prefix for the driver controller.
     */
    public static final String DRIVER_URL_PREFIX = "/driver";

    /**
     * URL prefix for batch operations.
     */
    public static final String BATCH_URL_PREFIX = "/batch";

    /**
     * URL prefix for the driver-attribute controller.
     */
    public static final String DRIVER_ATTRIBUTE_URL_PREFIX = "/driver_attribute";

    /**
     * URL prefix for the point-attribute controller.
     */
    public static final String POINT_ATTRIBUTE_URL_PREFIX = "/point_attribute";

    /**
     * URL prefix for the command-attribute controller.
     */
    public static final String COMMAND_ATTRIBUTE_URL_PREFIX = "/command_attribute";

    /**
     * URL prefix for the event-attribute controller.
     */
    public static final String EVENT_ATTRIBUTE_URL_PREFIX = "/event_attribute";

    /**
     * URL prefix for the profile controller.
     */
    public static final String PROFILE_URL_PREFIX = "/profile";

    /**
     * URL prefix for the point controller.
     */
    public static final String POINT_URL_PREFIX = "/point";

    /**
     * URL prefix for the group controller.
     */
    public static final String GROUP_URL_PREFIX = "/group";

    /**
     * URL prefix for the group-bind controller.
     */
    public static final String GROUP_BIND_URL_PREFIX = "/group_bind";

    /**
     * URL prefix for the device controller.
     */
    public static final String DEVICE_URL_PREFIX = "/device";

    /**
     * URL prefix for the dashboard controller.
     */
    public static final String DASHBOARD_URL_PREFIX = "/dashboard";

    /**
     * URL prefix for auto-completion endpoints.
     */
    public static final String AUTO_URL_PREFIX = "/auto";

    /**
     * URL prefix for the point-attribute config controller.
     */
    public static final String POINT_ATTRIBUTE_CONFIG_URL_PREFIX = "/point_attribute_config";

    /**
     * URL prefix for the driver-attribute config controller.
     */
    public static final String DRIVER_ATTRIBUTE_CONFIG_URL_PREFIX = "/driver_attribute_config";

    /**
     * URL prefix for the command-attribute config controller.
     */
    public static final String COMMAND_ATTRIBUTE_CONFIG_URL_PREFIX = "/command_attribute_config";

    /**
     * URL prefix for the event-attribute config controller.
     */
    public static final String EVENT_ATTRIBUTE_CONFIG_URL_PREFIX = "/event_attribute_config";

    /**
     * URL prefix for the label controller.
     */
    public static final String LABEL_URL_PREFIX = "/label";

    /**
     * URL prefix for the label-bind controller.
     */
    public static final String LABEL_BIND_URL_PREFIX = "/label_bind";

    /**
     * URL prefix for the dictionary controller.
     */
    public static final String DICTIONARY_URL_PREFIX = "/dictionary";

    /**
     * URL prefix for the topic controller.
     */
    public static final String TOPIC_URL_PREFIX = "/topic";

    /**
     * URL prefix for the command controller.
     */
    public static final String COMMAND_URL_PREFIX = "/command";

    /**
     * URL prefix for the command-param controller.
     */
    public static final String COMMAND_PARAM_URL_PREFIX = "/command_param";

    /**
     * URL prefix for the event controller.
     */
    public static final String EVENT_URL_PREFIX = "/event";

    /**
     * URL prefix for the event-param controller.
     */
    public static final String EVENT_PARAM_URL_PREFIX = "/event_param";

    private ManagerConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}

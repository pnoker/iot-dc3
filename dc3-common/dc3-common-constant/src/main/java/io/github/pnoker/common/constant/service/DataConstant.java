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
 * Constants for the data service module.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class DataConstant {

    /**
     * Service name
     */
    public static final String SERVICE_NAME = "dc3-center-data";

    public static final String POINT_VALUE_URL_PREFIX = "/point_value";

    public static final String POINT_COMMAND_URL_PREFIX = "/point_command";

    public static final String DASHBOARD_URL_PREFIX = "/dashboard";

    public static final String RULE_URL_PREFIX = "/rule";

    public static final String RULE_STATE_URL_PREFIX = "/rule/state";

    public static final String NOTIFY_URL_PREFIX = "/notify";

    public static final String NOTIFY_CHANNEL_URL_PREFIX = "/notify/channel";

    public static final String NOTIFY_CHANNEL_BIND_URL_PREFIX = "/notify/channel/bind";

    public static final String NOTIFY_HISTORY_URL_PREFIX = "/notify/history";

    public static final String MESSAGE_URL_PREFIX = "/message";

    public static final String DRIVER_STATUS_URL_PREFIX = "/driver/status";

    public static final String DRIVER_EVENT_URL_PREFIX = "/driver/event";

    public static final String DEVICE_STATUS_URL_PREFIX = "/device/status";

    public static final String DEVICE_EVENT_URL_PREFIX = "/device/event";

    public static final String RABBITMQ_CONNECTION_URL_PREFIX = "/rabbitmq/connection";

    public static final String RABBITMQ_MESSAGE_URL_PREFIX = "/rabbitmq/message";

    public static final String RABBITMQ_PUBLISHER_URL_PREFIX = "/rabbitmq/publisher";

    public static final String RABBITMQ_QUEUE_URL_PREFIX = "/rabbitmq/queue";

    public static final String RABBITMQ_CONSUMER_URL_PREFIX = "/rabbitmq/consumer";

    public static final String RABBITMQ_CHANNEL_URL_PREFIX = "/rabbitmq/channel";

    public static final String RABBITMQ_NODE_URL_PREFIX = "/rabbitmq/node";

    public static final String RABBITMQ_CLUSTER_URL_PREFIX = "/rabbitmq/cluster";

    private DataConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Point value API constants.
     *
     * @author pnoker
     * @version 2026.5.19
     * @since 2016.10.1
     */
    public static class PointValue {

        /**
         * Placeholder for a bound point that has no latest sample yet.
         */
        public static final String NO_LATEST_VALUE = "NaN";

        private PointValue() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * System health probe API constants. The string values are part of the
     * dashboard banner contract — frontend keys off them as-is.
     */
    public static class Health {

        /**
         * Status reported when a probe target is reachable.
         */
        public static final String STATUS_UP = "up";

        /**
         * Status reported when a probe target is unreachable, returned false,
         * or did not finish within the probe deadline.
         */
        public static final String STATUS_DOWN = "down";

        private Health() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

}

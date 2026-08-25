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
 * @since 2016.10.1
 */
public class DataConstant {

    /**
     * Service name
     */
    public static final String SERVICE_NAME = "dc3-center-data";

    /**
     * Fallback status when an entity status cannot be resolved.
     */
    public static final String STATUS_UNKNOWN = "unknown";

    /** URL prefix for the point-value controller. */
    public static final String POINT_VALUE_URL_PREFIX = "/point_value";

    /** URL prefix for the point-command controller. */
    public static final String POINT_COMMAND_URL_PREFIX = "/point_command";

    /** URL prefix for the point-command history controller. */
    public static final String POINT_COMMAND_HISTORY_URL_PREFIX = "/point_command_history";

    /** URL prefix for the dashboard controller. */
    public static final String DASHBOARD_URL_PREFIX = "/dashboard";

    /** URL prefix for the alarm-rule controller. */
    public static final String RULE_URL_PREFIX = "/rule";

    /** URL prefix for the rule-state controller. */
    public static final String RULE_STATE_URL_PREFIX = "/rule/state";

    /** URL prefix for the notify-policy controller. */
    public static final String NOTIFY_URL_PREFIX = "/notify";

    /** URL prefix for the notify-channel controller. */
    public static final String NOTIFY_CHANNEL_URL_PREFIX = "/notify/channel";

    /** URL prefix for the notify-channel bind controller. */
    public static final String NOTIFY_CHANNEL_BIND_URL_PREFIX = "/notify/channel/bind";

    /** URL prefix for the notify-history controller. */
    public static final String NOTIFY_HISTORY_URL_PREFIX = "/notify/history";

    /** URL prefix for the message controller. */
    public static final String MESSAGE_URL_PREFIX = "/message";

    /** URL prefix for the command-history controller. */
    public static final String COMMAND_HISTORY_URL_PREFIX = "/command_history";

    /** URL prefix for the event-history controller. */
    public static final String EVENT_HISTORY_URL_PREFIX = "/event_history";

    /** URL prefix for the driver-status controller. */
    public static final String DRIVER_STATUS_URL_PREFIX = "/driver/status";

    /** URL prefix for the driver-event controller. */
    public static final String DRIVER_EVENT_URL_PREFIX = "/driver/event";

    /** URL prefix for the device-status controller. */
    public static final String DEVICE_STATUS_URL_PREFIX = "/device/status";

    /** URL prefix for the device-event controller. */
    public static final String DEVICE_EVENT_URL_PREFIX = "/device/event";

    /** URL prefix for the RabbitMQ connection admin endpoint. */
    public static final String RABBITMQ_CONNECTION_URL_PREFIX = "/rabbitmq/connection";

    /** URL prefix for the RabbitMQ message admin endpoint. */
    public static final String RABBITMQ_MESSAGE_URL_PREFIX = "/rabbitmq/message";

    /** URL prefix for the RabbitMQ publisher admin endpoint. */
    public static final String RABBITMQ_PUBLISHER_URL_PREFIX = "/rabbitmq/publisher";

    /** URL prefix for the RabbitMQ queue admin endpoint. */
    public static final String RABBITMQ_QUEUE_URL_PREFIX = "/rabbitmq/queue";

    /** URL prefix for the RabbitMQ consumer admin endpoint. */
    public static final String RABBITMQ_CONSUMER_URL_PREFIX = "/rabbitmq/consumer";

    /** URL prefix for the RabbitMQ channel admin endpoint. */
    public static final String RABBITMQ_CHANNEL_URL_PREFIX = "/rabbitmq/channel";

    /** URL prefix for the RabbitMQ node admin endpoint. */
    public static final String RABBITMQ_NODE_URL_PREFIX = "/rabbitmq/node";

    /** URL prefix for the RabbitMQ cluster admin endpoint. */
    public static final String RABBITMQ_CLUSTER_URL_PREFIX = "/rabbitmq/cluster";

    private DataConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Point value API constants.
     *
     * @author pnoker
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

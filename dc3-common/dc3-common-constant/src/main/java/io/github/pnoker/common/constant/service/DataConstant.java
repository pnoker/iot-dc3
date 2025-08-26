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

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 数据服务 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class DataConstant {

    /**
     * 服务名
     */
    public static final String SERVICE_NAME = "dc3-center-data";
    public static final String POINT_VALUE_URL_PREFIX = "/point_value";
    public static final String POINT_VALUE_COMMAND_URL_PREFIX = "/point_value_command";
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
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}

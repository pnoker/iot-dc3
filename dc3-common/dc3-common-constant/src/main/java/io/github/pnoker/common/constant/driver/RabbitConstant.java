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

package io.github.pnoker.common.constant.driver;

import io.github.pnoker.common.constant.common.BaseConstant;


/**
 * RabbitMQ exchange, queue, and routing key constants for the DC3 platform.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class RabbitConstant {

    // Arguments
    public static final String MESSAGE_TTL = "x-message-ttl";

    public static final String AUTO_DELETE = "x-auto-delete";

    // Routing
    public static final String ROUTING_REGISTER_UP_PREFIX = "dc3.r.register.up.";

    public static final String ROUTING_REGISTER_DOWN_PREFIX = "dc3.r.register.down.";

    public static final String ROUTING_DRIVER_STATE_PREFIX = "dc3.r.state.driver.";

    public static final String ROUTING_DEVICE_STATE_PREFIX = "dc3.r.state.device.";

    public static final String ROUTING_DRIVER_ALARM_PREFIX = "dc3.r.alarm.driver.";

    public static final String ROUTING_DEVICE_ALARM_PREFIX = "dc3.r.alarm.device.";

    public static final String ROUTING_DRIVER_METADATA_PREFIX = "dc3.r.metadata.driver.";

    public static final String ROUTING_DRIVER_COMMAND_PREFIX = "dc3.r.command.driver.";

    public static final String ROUTING_DEVICE_COMMAND_PREFIX = "dc3.r.command.device.";

    public static final String ROUTING_POINT_VALUE_PREFIX = "dc3.r.value.point.";

    public static final String ROUTING_MQTT_PREFIX = "dc3.r.mqtt.";

    // Register
    public static String TOPIC_EXCHANGE_REGISTER = "dc3.e.register";

    public static String QUEUE_REGISTER_UP = "dc3.q.register.up";

    public static String QUEUE_REGISTER_DOWN_PREFIX = "dc3.q.register.down.";

    // State
    public static String TOPIC_EXCHANGE_STATE = "dc3.e.state";

    public static String QUEUE_DRIVER_STATE = "dc3.q.state.driver";

    public static String QUEUE_DEVICE_STATE = "dc3.q.state.device";

    // Alarm
    public static String TOPIC_EXCHANGE_ALARM = "dc3.e.alarm";

    public static String QUEUE_DRIVER_ALARM = "dc3.q.alarm.driver";

    public static String QUEUE_DEVICE_ALARM = "dc3.q.alarm.device";

    public static final String ROUTING_NOTIFY_TASK_PREFIX = "dc3.r.notify.task.";

    public static String QUEUE_NOTIFY_TASK = "dc3.q.notify.task";

    // Metadata
    public static String TOPIC_EXCHANGE_METADATA = "dc3.e.metadata";

    public static String QUEUE_DRIVER_METADATA_PREFIX = "dc3.q.metadata.driver.";

    // Command
    public static String TOPIC_EXCHANGE_COMMAND = "dc3.e.command";

    public static String QUEUE_DRIVER_COMMAND_PREFIX = "dc3.q.command.driver.";

    public static String QUEUE_DEVICE_COMMAND_PREFIX = "dc3.q.command.device.";

    // Value
    public static String TOPIC_EXCHANGE_VALUE = "dc3.e.value";

    public static String QUEUE_POINT_VALUE = "dc3.q.value.point";

    // Mqtt
    public static String TOPIC_EXCHANGE_MQTT = "dc3.e.mqtt";

    public static String QUEUE_MQTT = "dc3.q.mqtt";

    private RabbitConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}

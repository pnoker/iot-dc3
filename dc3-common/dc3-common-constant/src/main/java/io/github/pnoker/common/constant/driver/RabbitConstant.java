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


    public static final String ROUTING_POINT_COMMAND_PREFIX = "dc3.r.point_command.";

    public static final String ROUTING_POINT_VALUE_PREFIX = "dc3.r.value.point.";

    public static final String ROUTING_MQTT_PREFIX = "dc3.r.mqtt.";
    public static final String ROUTING_NOTIFY_TASK_PREFIX = "dc3.r.notify.task.";
    // Routing keys
    public static final String ROUTING_DRIVER_TIMEOUT_DELAY = "state.timeout.driver.45s";
    public static final String ROUTING_DRIVER_TIMEOUT_CHECK = "state.timeout.driver.check";
    // Routing keys
    public static final String ROUTING_DEVICE_SCAN_TICK = "state.timeout.device.scan.tick";
    public static final String ROUTING_DEVICE_SCAN = "state.timeout.device.scan";
    public static final String ROUTING_POINT_COMMAND_RESULT = "dc3.r.point_command_result";
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
    public static String QUEUE_NOTIFY_TASK = "dc3.q.notify.task";
    // Metadata
    public static String TOPIC_EXCHANGE_METADATA = "dc3.e.metadata";
    public static String QUEUE_DRIVER_METADATA_PREFIX = "dc3.q.metadata.driver.";
    // Point Command
    public static String TOPIC_EXCHANGE_POINT_COMMAND = "dc3.e.point_command";
    public static String QUEUE_POINT_COMMAND_PREFIX = "dc3.q.point_command.";
    // Value
    public static String TOPIC_EXCHANGE_VALUE = "dc3.e.value";
    public static String QUEUE_POINT_VALUE = "dc3.q.value.point";
    // Mqtt
    public static String TOPIC_EXCHANGE_MQTT = "dc3.e.mqtt";
    public static String QUEUE_MQTT = "dc3.q.mqtt";
    // State Timeout - Delay Exchange (receives messages to be delayed)
    public static String TOPIC_EXCHANGE_STATE_TIMEOUT_DELAY = "dc3.e.state_timeout_delay";
    // State Timeout - Check Exchange (receives expired messages from DLX)
    public static String TOPIC_EXCHANGE_STATE_TIMEOUT_CHECK = "dc3.e.state_timeout_check";
    // Driver timeout delay queue (45s TTL, dead-letter to check exchange)
    public static String QUEUE_DRIVER_TIMEOUT_DELAY = "dc3.q.state_timeout.driver.45s";
    // Driver timeout check queue (consumed by Data Center)
    public static String QUEUE_DRIVER_TIMEOUT_CHECK = "dc3.q.state_timeout.driver_check";
    // Device scan tick delay queue (10s TTL, dead-letter to scan queue)
    public static String QUEUE_DEVICE_SCAN_TICK = "dc3.q.state_timeout.device_scan_tick.10s";
    // Device scan execution queue (consumed by Data Center)
    public static String QUEUE_DEVICE_SCAN = "dc3.q.state_timeout.device_scan";
    // Point Command Dead Letter
    public static String TOPIC_EXCHANGE_POINT_COMMAND_DEAD = "dc3.e.point_command_dead";
    public static String QUEUE_POINT_COMMAND_DEAD = "dc3.q.point_command_dead";
    // Point Command Result
    public static String TOPIC_EXCHANGE_POINT_COMMAND_RESULT = "dc3.e.point_command_result";
    public static String QUEUE_POINT_COMMAND_RESULT = "dc3.q.point_command_result";
    // Custom Command
    public static String TOPIC_EXCHANGE_COMMAND = "dc3.e.command";
    public static String QUEUE_COMMAND_PREFIX = "dc3.q.command.";
    public static String ROUTING_COMMAND_PREFIX = "dc3.r.command.";
    // Custom Command Result
    public static String TOPIC_EXCHANGE_COMMAND_RESULT = "dc3.e.command_result";
    public static String QUEUE_COMMAND_RESULT = "dc3.q.command_result";
    public static String ROUTING_COMMAND_RESULT = "dc3.r.command_result";
    // Custom Command Dead Letter
    public static String TOPIC_EXCHANGE_COMMAND_DEAD = "dc3.e.command_dead";
    public static String QUEUE_COMMAND_DEAD = "dc3.q.command_dead";
    // Event Report
    public static String TOPIC_EXCHANGE_EVENT = "dc3.e.event";
    public static String QUEUE_EVENT_PREFIX = "dc3.q.event.";
    public static String ROUTING_EVENT_PREFIX = "dc3.r.event.";

    private RabbitConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}

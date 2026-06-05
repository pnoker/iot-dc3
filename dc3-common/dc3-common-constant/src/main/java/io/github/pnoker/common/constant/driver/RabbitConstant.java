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
import io.github.pnoker.common.constant.common.SymbolConstant;


/**
 * RabbitMQ exchange, queue, and routing key constants for the DC3 platform.
 * <p>
 * All fields are {@code final}. The environment/group tag prefix is read from
 * the {@code dc3.rabbit.tag} system property during class initialization.
 * {@link io.github.pnoker.common.config.RabbitmqEnvironmentConfig} sets this
 * property during {@code EnvironmentPostProcessor} execution, which runs
 * before any application context beans are created.
 *
 * @author pnoker
 * @version 2026.5.22
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

    public static final String ROUTING_POINT_COMMAND_RESULT_PREFIX = ROUTING_POINT_COMMAND_RESULT + SymbolConstant.DOT;

    // --- Tag-aware fields initialized from system property ---
    // Register
    public static final String TOPIC_EXCHANGE_REGISTER;
    public static final String QUEUE_REGISTER_UP;
    public static final String QUEUE_REGISTER_DOWN_PREFIX;
    // State
    public static final String TOPIC_EXCHANGE_STATE;
    public static final String QUEUE_DRIVER_STATE;
    public static final String QUEUE_DEVICE_STATE;
    // Alarm
    public static final String TOPIC_EXCHANGE_ALARM;
    public static final String QUEUE_DRIVER_ALARM;
    public static final String QUEUE_DEVICE_ALARM;
    public static final String QUEUE_NOTIFY_TASK;
    // Metadata
    public static final String TOPIC_EXCHANGE_METADATA;
    public static final String QUEUE_DRIVER_METADATA_PREFIX;
    // Point Command
    public static final String TOPIC_EXCHANGE_POINT_COMMAND;
    public static final String QUEUE_POINT_COMMAND_PREFIX;
    // Value
    public static final String TOPIC_EXCHANGE_VALUE;
    public static final String QUEUE_POINT_VALUE;
    // Mqtt
    public static final String TOPIC_EXCHANGE_MQTT;
    public static final String QUEUE_MQTT;
    // State Timeout - Delay Exchange (receives messages to be delayed)
    public static final String TOPIC_EXCHANGE_STATE_TIMEOUT_DELAY;
    // State Timeout - Check Exchange (receives expired messages from DLX)
    public static final String TOPIC_EXCHANGE_STATE_TIMEOUT_CHECK;
    // Driver timeout delay queue (45s TTL, dead-letter to check exchange)
    public static final String QUEUE_DRIVER_TIMEOUT_DELAY;
    // Driver timeout check queue (consumed by Data Center)
    public static final String QUEUE_DRIVER_TIMEOUT_CHECK;
    // Device scan tick delay queue (10s TTL, dead-letter to scan queue)
    public static final String QUEUE_DEVICE_SCAN_TICK;
    // Device scan execution queue (consumed by Data Center)
    public static final String QUEUE_DEVICE_SCAN;
    // Point Command Dead Letter
    public static final String TOPIC_EXCHANGE_POINT_COMMAND_DEAD;
    public static final String QUEUE_POINT_COMMAND_DEAD;
    // Point Command Result
    public static final String TOPIC_EXCHANGE_POINT_COMMAND_RESULT;
    public static final String QUEUE_POINT_COMMAND_RESULT;
    // Custom Command
    public static final String TOPIC_EXCHANGE_COMMAND;
    public static final String QUEUE_COMMAND_PREFIX;
    public static final String ROUTING_COMMAND_PREFIX;
    // Custom Command Result
    public static final String TOPIC_EXCHANGE_COMMAND_RESULT;
    public static final String QUEUE_COMMAND_RESULT;
    public static final String ROUTING_COMMAND_RESULT;

    public static final String ROUTING_COMMAND_RESULT_PREFIX;
    // Custom Command Dead Letter
    public static final String TOPIC_EXCHANGE_COMMAND_DEAD;
    public static final String QUEUE_COMMAND_DEAD;
    // Event Report
    public static final String TOPIC_EXCHANGE_EVENT;
    public static final String QUEUE_EVENT_PREFIX;
    public static final String ROUTING_EVENT_PREFIX;
    // Point Value Dead Letter
    public static final String TOPIC_EXCHANGE_POINT_VALUE_DEAD;
    public static final String QUEUE_POINT_VALUE_DEAD;

    static {
        String tag = System.getProperty("dc3.rabbit.tag", "");

        // Register
        TOPIC_EXCHANGE_REGISTER = tag + "dc3.e.register";
        QUEUE_REGISTER_UP = tag + "dc3.q.register.up";
        QUEUE_REGISTER_DOWN_PREFIX = tag + "dc3.q.register.down.";
        // State
        TOPIC_EXCHANGE_STATE = tag + "dc3.e.state";
        QUEUE_DRIVER_STATE = tag + "dc3.q.state.driver";
        QUEUE_DEVICE_STATE = tag + "dc3.q.state.device";
        // Alarm
        TOPIC_EXCHANGE_ALARM = tag + "dc3.e.alarm";
        QUEUE_DRIVER_ALARM = tag + "dc3.q.alarm.driver";
        QUEUE_DEVICE_ALARM = tag + "dc3.q.alarm.device";
        QUEUE_NOTIFY_TASK = tag + "dc3.q.notify.task";
        // Metadata
        TOPIC_EXCHANGE_METADATA = tag + "dc3.e.metadata";
        QUEUE_DRIVER_METADATA_PREFIX = tag + "dc3.q.metadata.driver.";
        // Point Command
        TOPIC_EXCHANGE_POINT_COMMAND = tag + "dc3.e.point_command";
        QUEUE_POINT_COMMAND_PREFIX = tag + "dc3.q.point_command.";
        // Value
        TOPIC_EXCHANGE_VALUE = tag + "dc3.e.value";
        QUEUE_POINT_VALUE = tag + "dc3.q.value.point";
        // Mqtt
        TOPIC_EXCHANGE_MQTT = tag + "dc3.e.mqtt";
        QUEUE_MQTT = tag + "dc3.q.mqtt";
        // State Timeout - Delay Exchange (receives messages to be delayed)
        TOPIC_EXCHANGE_STATE_TIMEOUT_DELAY = tag + "dc3.e.state_timeout_delay";
        // State Timeout - Check Exchange (receives expired messages from DLX)
        TOPIC_EXCHANGE_STATE_TIMEOUT_CHECK = tag + "dc3.e.state_timeout_check";
        // Driver timeout delay queue (45s TTL, dead-letter to check exchange)
        QUEUE_DRIVER_TIMEOUT_DELAY = tag + "dc3.q.state_timeout.driver.45s";
        // Driver timeout check queue (consumed by Data Center)
        QUEUE_DRIVER_TIMEOUT_CHECK = tag + "dc3.q.state_timeout.driver_check";
        // Device scan tick delay queue (10s TTL, dead-letter to scan queue)
        QUEUE_DEVICE_SCAN_TICK = tag + "dc3.q.state_timeout.device_scan_tick.10s";
        // Device scan execution queue (consumed by Data Center)
        QUEUE_DEVICE_SCAN = tag + "dc3.q.state_timeout.device_scan";
        // Point Command Dead Letter
        TOPIC_EXCHANGE_POINT_COMMAND_DEAD = tag + "dc3.e.point_command_dead";
        QUEUE_POINT_COMMAND_DEAD = tag + "dc3.q.point_command_dead";
        // Point Command Result
        TOPIC_EXCHANGE_POINT_COMMAND_RESULT = tag + "dc3.e.point_command_result";
        QUEUE_POINT_COMMAND_RESULT = tag + "dc3.q.point_command_result";
        // Custom Command
        TOPIC_EXCHANGE_COMMAND = tag + "dc3.e.command";
        QUEUE_COMMAND_PREFIX = tag + "dc3.q.command.";
        ROUTING_COMMAND_PREFIX = tag + "dc3.r.command.";
        // Custom Command Result
        TOPIC_EXCHANGE_COMMAND_RESULT = tag + "dc3.e.command_result";
        QUEUE_COMMAND_RESULT = tag + "dc3.q.command_result";
        ROUTING_COMMAND_RESULT = tag + "dc3.r.command_result";

        ROUTING_COMMAND_RESULT_PREFIX = ROUTING_COMMAND_RESULT + SymbolConstant.DOT;
        // Custom Command Dead Letter
        TOPIC_EXCHANGE_COMMAND_DEAD = tag + "dc3.e.command_dead";
        QUEUE_COMMAND_DEAD = tag + "dc3.q.command_dead";
        // Event Report
        TOPIC_EXCHANGE_EVENT = tag + "dc3.e.event";
        QUEUE_EVENT_PREFIX = tag + "dc3.q.event.";
        ROUTING_EVENT_PREFIX = tag + "dc3.r.event.";
        // Point Value Dead Letter
        TOPIC_EXCHANGE_POINT_VALUE_DEAD = tag + "dc3.e.point_value_dead";
        QUEUE_POINT_VALUE_DEAD = tag + "dc3.q.point_value_dead";
    }

    private RabbitConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}

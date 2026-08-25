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
 * {@code RabbitmqEnvironmentConfig} in the RabbitMQ integration module sets this
 * property during {@code EnvironmentPostProcessor} execution, which runs
 * before any application context beans are created.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public class RabbitConstant {

    // Arguments
    /**
     * message ttl constant.
     */
    public static final String MESSAGE_TTL = "x-message-ttl";

    /**
     * auto delete constant.
     */
    public static final String AUTO_DELETE = "x-auto-delete";

    // Routing
    /**
     * Routing key for register up prefix.
     */
    public static final String ROUTING_REGISTER_UP_PREFIX = "dc3.r.register.up.";

    /**
     * Routing key for register down prefix.
     */
    public static final String ROUTING_REGISTER_DOWN_PREFIX = "dc3.r.register.down.";

    /**
     * Routing key prefix for driver state events.
     */
    public static final String ROUTING_DRIVER_STATE_PREFIX = "dc3.r.state.driver.";

    /**
     * Routing key prefix for device state events.
     */
    public static final String ROUTING_DEVICE_STATE_PREFIX = "dc3.r.state.device.";

    /**
     * Routing key for driver alarm prefix.
     */
    public static final String ROUTING_DRIVER_ALARM_PREFIX = "dc3.r.alarm.driver.";

    /**
     * Routing key for device alarm prefix.
     */
    public static final String ROUTING_DEVICE_ALARM_PREFIX = "dc3.r.alarm.device.";

    /**
     * Routing key prefix for driver metadata events.
     */
    public static final String ROUTING_DRIVER_METADATA_PREFIX = "dc3.r.metadata.driver.";


    /**
     * Routing key prefix for point commands.
     */
    public static final String ROUTING_POINT_COMMAND_PREFIX = "dc3.r.point_command.";

    /**
     * Routing key prefix for point values.
     */
    public static final String ROUTING_POINT_VALUE_PREFIX = "dc3.r.value.point.";

    /**
     * Routing key for mqtt prefix.
     */
    public static final String ROUTING_MQTT_PREFIX = "dc3.r.mqtt.";
    /**
     * Routing key for notify task prefix.
     */
    public static final String ROUTING_NOTIFY_TASK_PREFIX = "dc3.r.notify.task.";
    // Routing keys
    /**
     * Routing key for driver timeout delay.
     */
    public static final String ROUTING_DRIVER_TIMEOUT_DELAY = "state.timeout.driver.45s";
    /**
     * Routing key for driver timeout check.
     */
    public static final String ROUTING_DRIVER_TIMEOUT_CHECK = "state.timeout.driver.check";
    // Routing keys
    /**
     * Routing key for device scan tick.
     */
    public static final String ROUTING_DEVICE_SCAN_TICK = "state.timeout.device.scan.tick";
    /**
     * Routing key for device scan.
     */
    public static final String ROUTING_DEVICE_SCAN = "state.timeout.device.scan";
    /**
     * Routing key for point command result.
     */
    public static final String ROUTING_POINT_COMMAND_RESULT = "dc3.r.point_command_result";

    /**
     * Routing key prefix for point command results.
     */
    public static final String ROUTING_POINT_COMMAND_RESULT_PREFIX = ROUTING_POINT_COMMAND_RESULT + SymbolConstant.DOT;

    // --- Tag-aware fields initialized from system property ---
    // Register
    /**
     * Exchange carrying driver registration requests.
     */
    public static final String TOPIC_EXCHANGE_REGISTER;
    /**
     * Queue for driver registration requests.
     */
    public static final String QUEUE_REGISTER_UP;
    /**
     * Queue prefix for registration acknowledgments.
     */
    public static final String QUEUE_REGISTER_DOWN_PREFIX;
    // State
    /**
     * Exchange carrying driver/device state events.
     */
    public static final String TOPIC_EXCHANGE_STATE;
    /**
     * Queue for driver state events.
     */
    public static final String QUEUE_DRIVER_STATE;
    /**
     * Queue for device state events.
     */
    public static final String QUEUE_DEVICE_STATE;
    // Alarm
    /**
     * Exchange carrying alarm events.
     */
    public static final String TOPIC_EXCHANGE_ALARM;
    /**
     * Queue for driver alarm events.
     */
    public static final String QUEUE_DRIVER_ALARM;
    /**
     * Queue for device alarm events.
     */
    public static final String QUEUE_DEVICE_ALARM;
    /**
     * Queue for notification tasks.
     */
    public static final String QUEUE_NOTIFY_TASK;
    // Metadata
    /**
     * Exchange carrying metadata-change events.
     */
    public static final String TOPIC_EXCHANGE_METADATA;
    /**
     * Queue prefix for per-driver metadata subscriptions.
     */
    public static final String QUEUE_DRIVER_METADATA_PREFIX;
    // Point Command
    /**
     * Exchange carrying point read/write commands.
     */
    public static final String TOPIC_EXCHANGE_POINT_COMMAND;
    /**
     * Queue prefix for per-node point commands.
     */
    public static final String QUEUE_POINT_COMMAND_PREFIX;
    // Value
    /**
     * Exchange carrying point values from drivers.
     */
    public static final String TOPIC_EXCHANGE_VALUE;
    /**
     * Queue for point values consumed by the data center.
     */
    public static final String QUEUE_POINT_VALUE;
    // Mqtt
    /**
     * Exchange carrying MQTT-ingested messages.
     */
    public static final String TOPIC_EXCHANGE_MQTT;
    /**
     * Queue for MQTT-ingested messages.
     */
    public static final String QUEUE_MQTT;
    // State Timeout - Delay Exchange (receives messages to be delayed)
    /**
     * Exchange carrying delayed timeout checks.
     */
    public static final String TOPIC_EXCHANGE_STATE_TIMEOUT_DELAY;
    // State Timeout - Check Exchange (receives expired messages from DLX)
    /**
     * Exchange carrying timeout check messages.
     */
    public static final String TOPIC_EXCHANGE_STATE_TIMEOUT_CHECK;
    // Driver timeout delay queue (45s TTL, dead-letter to check exchange)
    /**
     * Queue holding delayed driver timeout checks.
     */
    public static final String QUEUE_DRIVER_TIMEOUT_DELAY;
    // Driver timeout check queue (consumed by Data Center)
    /**
     * Queue for driver timeout checks.
     */
    public static final String QUEUE_DRIVER_TIMEOUT_CHECK;
    // Device scan tick delay queue (10s TTL, dead-letter to scan queue)
    /**
     * Queue driving the periodic device scan tick.
     */
    public static final String QUEUE_DEVICE_SCAN_TICK;
    // Device scan execution queue (consumed by Data Center)
    /**
     * Queue for device scan results.
     */
    public static final String QUEUE_DEVICE_SCAN;
    // Point Command Dead Letter
    /**
     * Dead-letter exchange for failed point commands.
     */
    public static final String TOPIC_EXCHANGE_POINT_COMMAND_DEAD;
    /**
     * Dead-letter queue for failed point commands.
     */
    public static final String QUEUE_POINT_COMMAND_DEAD;
    // Point Command Result
    /**
     * Exchange carrying point command results.
     */
    public static final String TOPIC_EXCHANGE_POINT_COMMAND_RESULT;
    /**
     * Queue for point command results.
     */
    public static final String QUEUE_POINT_COMMAND_RESULT;
    // Custom Command
    /**
     * Exchange carrying custom device commands.
     */
    public static final String TOPIC_EXCHANGE_COMMAND;
    /**
     * Queue prefix for per-node custom commands.
     */
    public static final String QUEUE_COMMAND_PREFIX;
    /**
     * Routing key prefix for custom commands.
     */
    public static final String ROUTING_COMMAND_PREFIX;
    // Custom Command Result
    /**
     * Exchange carrying custom command results.
     */
    public static final String TOPIC_EXCHANGE_COMMAND_RESULT;
    /**
     * Queue for custom command results.
     */
    public static final String QUEUE_COMMAND_RESULT;
    /**
     * Routing key for custom command results.
     */
    public static final String ROUTING_COMMAND_RESULT;

    /**
     * Routing key prefix for custom command results.
     */
    public static final String ROUTING_COMMAND_RESULT_PREFIX;
    // Custom Command Dead Letter
    /**
     * Dead-letter exchange for failed custom commands.
     */
    public static final String TOPIC_EXCHANGE_COMMAND_DEAD;
    /**
     * Dead-letter queue for failed custom commands.
     */
    public static final String QUEUE_COMMAND_DEAD;
    // Event Report
    /**
     * Exchange carrying reported domain events.
     */
    public static final String TOPIC_EXCHANGE_EVENT;
    /**
     * Queue prefix for reported domain events.
     */
    public static final String QUEUE_EVENT_PREFIX;
    /**
     * Routing key prefix for reported domain events.
     */
    public static final String ROUTING_EVENT_PREFIX;
    // Point Value Dead Letter
    /**
     * Dead-letter exchange for rejected point-value batches.
     */
    public static final String TOPIC_EXCHANGE_POINT_VALUE_DEAD;
    /**
     * Dead-letter queue for rejected point-value batches.
     */
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

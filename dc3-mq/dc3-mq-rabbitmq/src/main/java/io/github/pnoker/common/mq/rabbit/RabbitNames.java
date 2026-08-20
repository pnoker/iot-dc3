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

package io.github.pnoker.common.mq.rabbit;

/**
 * Physical RabbitMQ names — adapter-private. Values are byte-for-byte identical to the
 * pre-port {@code RabbitConstant} layout (including the {@code dc3.rabbit.tag}
 * environment prefix read at class initialization), so existing deployments keep
 * their topology. The dead register/mqtt constants are intentionally not carried over.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public final class RabbitNames {

    private static final String TAG = System.getProperty("dc3.rabbit.tag", "");

    // ===== Exchanges =====
    public static final String EXCHANGE_STATE = TAG + "dc3.e.state";
    public static final String EXCHANGE_ALARM = TAG + "dc3.e.alarm";
    public static final String EXCHANGE_METADATA = TAG + "dc3.e.metadata";
    public static final String EXCHANGE_POINT_COMMAND = TAG + "dc3.e.point_command";
    public static final String EXCHANGE_VALUE = TAG + "dc3.e.value";
    public static final String EXCHANGE_STATE_TIMEOUT_DELAY = TAG + "dc3.e.state_timeout_delay";
    public static final String EXCHANGE_STATE_TIMEOUT_CHECK = TAG + "dc3.e.state_timeout_check";
    public static final String EXCHANGE_COMMAND = TAG + "dc3.e.command";
    public static final String EXCHANGE_COMMAND_RESULT = TAG + "dc3.e.command_result";
    public static final String EXCHANGE_COMMAND_DEAD = TAG + "dc3.e.command_dead";
    public static final String EXCHANGE_EVENT = TAG + "dc3.e.event";
    public static final String EXCHANGE_POINT_VALUE_DEAD = TAG + "dc3.e.point_value_dead";
    public static final String EXCHANGE_POINT_COMMAND_DEAD = TAG + "dc3.e.point_command_dead";
    public static final String EXCHANGE_POINT_COMMAND_RESULT = TAG + "dc3.e.point_command_result";

    // ===== Queues (center-side, platform-shared) =====
    public static final String QUEUE_DRIVER_STATE = TAG + "dc3.q.state.driver";
    public static final String QUEUE_DEVICE_STATE = TAG + "dc3.q.state.device";
    public static final String QUEUE_DRIVER_ALARM = TAG + "dc3.q.alarm.driver";
    public static final String QUEUE_DEVICE_ALARM = TAG + "dc3.q.alarm.device";
    public static final String QUEUE_POINT_VALUE = TAG + "dc3.q.value.point";
    public static final String QUEUE_POINT_VALUE_DEAD = TAG + "dc3.q.point_value_dead";
    public static final String QUEUE_NOTIFY_TASK = TAG + "dc3.q.notify.task";
    public static final String QUEUE_DRIVER_TIMEOUT_DELAY = TAG + "dc3.q.state_timeout.driver.45s";
    public static final String QUEUE_DRIVER_TIMEOUT_CHECK = TAG + "dc3.q.state_timeout.driver_check";
    public static final String QUEUE_DEVICE_SCAN_TICK = TAG + "dc3.q.state_timeout.device_scan_tick.10s";
    public static final String QUEUE_DEVICE_SCAN = TAG + "dc3.q.state_timeout.device_scan";
    public static final String QUEUE_POINT_COMMAND_DEAD = TAG + "dc3.q.point_command_dead";
    public static final String QUEUE_POINT_COMMAND_RESULT = TAG + "dc3.q.point_command_result";
    public static final String QUEUE_COMMAND_DEAD = TAG + "dc3.q.command_dead";
    public static final String QUEUE_COMMAND_RESULT = TAG + "dc3.q.command_result";
    public static final String QUEUE_EVENT_REPORT = TAG + "dc3.q.event.report";

    // ===== Queue name templates (driver-side, per instance) =====
    public static final String QUEUE_DRIVER_METADATA_PREFIX = TAG + "dc3.q.metadata.driver.";
    public static final String QUEUE_POINT_COMMAND_PREFIX = TAG + "dc3.q.point_command.";
    public static final String QUEUE_COMMAND_PREFIX = TAG + "dc3.q.command.";

    // ===== Routing prefixes (publish side composes prefix + semantic partition key) =====
    // The tag situation deliberately mirrors the pre-port constants byte-for-byte: most
    // routing-key prefixes were declared WITHOUT the environment tag while command,
    // command_result and event were declared WITH it. Bindings and publish routing keys
    // always matched within each pair, so the inconsistency was invisible on the wire —
    // and must now be preserved exactly.
    public static final String ROUTING_STATE_PREFIX = "dc3.r.state.";
    public static final String ROUTING_ALARM_PREFIX = "dc3.r.alarm.";
    public static final String ROUTING_DRIVER_METADATA_PREFIX = "dc3.r.metadata.driver.";
    public static final String ROUTING_POINT_COMMAND_PREFIX = "dc3.r.point_command.";
    public static final String ROUTING_POINT_VALUE_PREFIX = "dc3.r.value.point.";
    public static final String ROUTING_NOTIFY_TASK_PREFIX = "dc3.r.notify.task.";
    public static final String ROUTING_POINT_COMMAND_RESULT_PREFIX = "dc3.r.point_command_result.";
    public static final String ROUTING_EVENT_PREFIX = TAG + "dc3.r.event.";
    public static final String ROUTING_COMMAND_PREFIX = TAG + "dc3.r.command.";
    public static final String ROUTING_COMMAND_RESULT_PREFIX = TAG + "dc3.r.command_result.";

    // ===== Fixed routing keys for the TTL + DLX delay queues (untagged, as before) =====
    public static final String ROUTING_DRIVER_TIMEOUT_DELAY = "state.timeout.driver.45s";
    public static final String ROUTING_DRIVER_TIMEOUT_CHECK = "state.timeout.driver.check";
    public static final String ROUTING_DEVICE_SCAN_TICK = "state.timeout.device.scan.tick";
    public static final String ROUTING_DEVICE_SCAN = "state.timeout.device.scan";

    private RabbitNames() {
        throw new IllegalStateException("Utility class");
    }
}

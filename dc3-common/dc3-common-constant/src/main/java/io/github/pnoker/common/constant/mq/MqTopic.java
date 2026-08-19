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

package io.github.pnoker.common.constant.mq;

/**
 * Logical messaging destinations. Business code references these only; physical
 * exchange/queue/topic names are adapter internals (see docs/design/mq-abstraction.md).
 *
 * @author pnoker
 * @since 2026.8.19
 */
public enum MqTopic {

    /** Driver/device lifecycle states. */
    STATE,

    /** Driver/device alarms; also carries notify tasks. */
    ALARM,

    /** Driver metadata sync (broadcast to every driver instance). */
    METADATA,

    /** Point read/write commands, data center to driver. */
    POINT_COMMAND,

    /** Telemetry point values, driver to data center. */
    POINT_VALUE,

    /** Driver/device event reports. */
    EVENT,

    /** Device-level custom commands, data center to driver. */
    COMMAND,

    /** Custom command results, driver to data center. */
    COMMAND_RESULT,

    /** Point command results, driver to data center. */
    POINT_COMMAND_RESULT,

    /** Outbound notification tasks. */
    NOTIFY_TASK,

    /** Driver lease timeout check (delayed trigger, DB lease is the source of truth). */
    STATE_TIMEOUT,

    /** Device scan tick (10 s delayed self-perpetuating cycle). */
    DEVICE_SCAN,

    /** Quarantine for point values (no consumer by design). */
    POINT_VALUE_DEAD,

    /** Dead letter for point commands. */
    POINT_COMMAND_DEAD,

    /** Dead letter for custom commands. */
    COMMAND_DEAD
}

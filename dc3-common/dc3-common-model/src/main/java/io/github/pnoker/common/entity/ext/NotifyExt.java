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

package io.github.pnoker.common.entity.ext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * JSON extension object for notification configuration.
 * <p>
 * Extended information related to notifications.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON extension object for notification configuration, embedded inside VO extension fields")
public class NotifyExt extends BaseExt {

    /**
     * Extended content.
     * <p>
     * The content can be distinguished by Type and Version.
     */
    @Schema(description = "Extended notification content, distinguished by the type and version fields of the base extension")
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Notification configuration content, grouping the individual notification behavior policies")
    public static class Content {

        /**
         * Duplicate suppression configuration.
         */
        @Schema(description = "Duplicate suppression (deduplication) configuration")
        private Dedup dedup;

        /**
         * Notification rate limit configuration.
         */
        @Schema(description = "Notification rate limit configuration")
        private RateLimit rateLimit;

        /**
         * Notification silence windows.
         */
        @Schema(description = "Notification silence window configuration")
        private Silence silence;

        /**
         * Repeated reminder configuration while an alarm is still firing.
         */
        @Schema(description = "Repeated reminder configuration applied while an alarm is still firing")
        private Repeat repeat;

        /**
         * Recovery notification configuration.
         */
        @Schema(description = "Recovery notification configuration")
        private Recovery recovery;

        /**
         * Escalation policies for unresolved alarms.
         */
        @Schema(description = "Escalation policies applied to unresolved alarms")
        private List<Escalation> escalation;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Duplicate suppression (deduplication) configuration for notifications")
    public static class Dedup {

        @Schema(description = "Whether deduplication is enabled", example = "true")
        private Boolean enabled;

        @Schema(description = "Deduplication key used to group notifications that should be suppressed as duplicates")
        private String key;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Notification rate limit configuration")
    public static class RateLimit {

        @Schema(description = "Rate limit window length in milliseconds", example = "60000")
        private Long intervalMs;

        @Schema(description = "Maximum number of notifications allowed within the interval window", example = "10")
        private Integer maxCount;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Notification silence window configuration")
    public static class Silence {

        @Schema(description = "Whether silence windows are enabled", example = "true")
        private Boolean enabled;

        @Schema(description = "List of silence windows during which notifications are suppressed")
        private List<Window> windows;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "A single silence window definition")
    public static class Window {

        @Schema(description = "Timezone identifier the start/end times are evaluated in", example = "Asia/Shanghai")
        private String timezone;

        @Schema(description = "Window start time of day in HH:mm format", example = "22:00")
        private String start;

        @Schema(description = "Window end time of day in HH:mm format", example = "08:00")
        private String end;

        @Schema(description = "Days of week the window applies to")
        private List<String> daysOfWeek;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Repeated reminder configuration applied while an alarm is still firing")
    public static class Repeat {

        @Schema(description = "Whether repeated reminders are enabled", example = "true")
        private Boolean enabled;

        @Schema(description = "Interval between repeated reminders in milliseconds", example = "300000")
        private Long intervalMs;

        @Schema(description = "Maximum number of repeated reminders to send", example = "5")
        private Integer maxTimes;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Recovery notification configuration")
    public static class Recovery {

        @Schema(description = "Whether recovery handling is enabled", example = "true")
        private Boolean enabled;

        @Schema(description = "Whether to send a notification message when an alarm recovers", example = "true")
        private Boolean sendRecoveryMessage;

        @Schema(description = "Whether to automatically confirm the alarm when it recovers", example = "false")
        private Boolean autoConfirmOnRecovery;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Escalation policy applied to an unresolved alarm")
    public static class Escalation {

        @Schema(description = "Delay after which this escalation is triggered, in milliseconds", example = "600000")
        private Long afterMs;

        @Schema(description = "Message template code used for the escalation notification")
        private String messageCode;

    }

}

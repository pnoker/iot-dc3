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
public class NotifyExt extends BaseExt {

    /**
     * Extended content.
     * <p>
     * The content can be distinguished by Type and Version.
     */
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        /**
         * Duplicate suppression configuration.
         */
        private Dedup dedup;

        /**
         * Notification rate limit configuration.
         */
        private RateLimit rateLimit;

        /**
         * Notification silence windows.
         */
        private Silence silence;

        /**
         * Repeated reminder configuration while an alarm is still firing.
         */
        private Repeat repeat;

        /**
         * Recovery notification configuration.
         */
        private Recovery recovery;

        /**
         * Escalation policies for unresolved alarms.
         */
        private List<Escalation> escalation;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dedup {

        private Boolean enabled;

        private String key;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimit {

        private Long intervalMs;

        private Integer maxCount;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Silence {

        private Boolean enabled;

        private List<Window> windows;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Window {

        private String timezone;

        private String start;

        private String end;

        private List<String> daysOfWeek;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Repeat {

        private Boolean enabled;

        private Long intervalMs;

        private Integer maxTimes;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recovery {

        private Boolean enabled;

        private Boolean sendRecoveryMessage;

        private Boolean autoConfirmOnRecovery;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Escalation {

        private Long afterMs;

        private String messageCode;

    }

}

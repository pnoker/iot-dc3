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

package io.github.pnoker.common.entity.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Event report DTO sent from driver to data center via RabbitMQ.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
public record EventReportDTO(
        String recordId,
        Long tenantId,
        Long deviceId,
        Long eventId,
        String eventCode,
        Byte eventTypeFlag,
        Byte eventLevelFlag,
        Map<String, String> paramValues,
        String configSnapshot,
        String message,
        Instant occurTime,
        int schemaVersion
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String recordId;
        private Long tenantId;
        private Long deviceId;
        private Long eventId;
        private String eventCode;
        private Byte eventTypeFlag;
        private Byte eventLevelFlag;
        private Map<String, String> paramValues;
        private String configSnapshot;
        private String message;
        private Instant occurTime;
        private int schemaVersion;

        public Builder recordId(String recordId) {
            this.recordId = recordId;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder deviceId(Long deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder eventId(Long eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventCode(String eventCode) {
            this.eventCode = eventCode;
            return this;
        }

        public Builder eventTypeFlag(Byte eventTypeFlag) {
            this.eventTypeFlag = eventTypeFlag;
            return this;
        }

        public Builder eventLevelFlag(Byte eventLevelFlag) {
            this.eventLevelFlag = eventLevelFlag;
            return this;
        }

        public Builder paramValues(Map<String, String> paramValues) {
            this.paramValues = paramValues;
            return this;
        }

        public Builder configSnapshot(String configSnapshot) {
            this.configSnapshot = configSnapshot;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder occurTime(Instant occurTime) {
            this.occurTime = occurTime;
            return this;
        }

        public Builder schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public EventReportDTO build() {
            return new EventReportDTO(recordId, tenantId, deviceId, eventId, eventCode,
                    eventTypeFlag, eventLevelFlag, paramValues, configSnapshot, message, occurTime, schemaVersion);
        }
    }
}

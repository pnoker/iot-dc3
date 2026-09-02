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
        int schemaVersion) {

    /**
     * Builder.
     *
     * @return converted value
     */
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

        /**
         * Record identifier.
         *
         * @param recordId record identifier
         * @return record identifier result
         */
        public Builder recordId(String recordId) {
            this.recordId = recordId;
            return this;
        }

        /**
         * Tenant identifier.
         *
         * @param tenantId tenant identifier
         * @return tenant identifier result
         */
        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Device identifier.
         *
         * @param deviceId device identifier
         * @return device identifier result
         */
        public Builder deviceId(Long deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        /**
         * Event identifier.
         *
         * @param eventId event identifier
         * @return event identifier result
         */
        public Builder eventId(Long eventId) {
            this.eventId = eventId;
            return this;
        }

        /**
         * Event code.
         *
         * @param eventCode event code
         * @return event code result
         */
        public Builder eventCode(String eventCode) {
            this.eventCode = eventCode;
            return this;
        }

        /**
         * Event type flag.
         *
         * @param eventTypeFlag event type flag
         * @return event type flag result
         */
        public Builder eventTypeFlag(Byte eventTypeFlag) {
            this.eventTypeFlag = eventTypeFlag;
            return this;
        }

        /**
         * Event level flag.
         *
         * @param eventLevelFlag event level flag
         * @return event level flag result
         */
        public Builder eventLevelFlag(Byte eventLevelFlag) {
            this.eventLevelFlag = eventLevelFlag;
            return this;
        }

        /**
         * Param values.
         *
         * @param paramValues param values
         * @return param values result
         */
        public Builder paramValues(Map<String, String> paramValues) {
            this.paramValues = paramValues;
            return this;
        }

        /**
         * Config snapshot.
         *
         * @param configSnapshot config snapshot
         * @return config snapshot result
         */
        public Builder configSnapshot(String configSnapshot) {
            this.configSnapshot = configSnapshot;
            return this;
        }

        /**
         * Message.
         *
         * @param message message
         * @return message result
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Occur time.
         *
         * @param occurTime occur time
         * @return occur time result
         */
        public Builder occurTime(Instant occurTime) {
            this.occurTime = occurTime;
            return this;
        }

        /**
         * Schema version.
         *
         * @param schemaVersion schema version
         * @return schema version result
         */
        public Builder schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        /**
         * Build an immutable event report from the accumulated fields.
         *
         * @return the event report DTO
         */
        public EventReportDTO build() {
            return new EventReportDTO(
                    recordId,
                    tenantId,
                    deviceId,
                    eventId,
                    eventCode,
                    eventTypeFlag,
                    eventLevelFlag,
                    paramValues,
                    configSnapshot,
                    message,
                    occurTime,
                    schemaVersion);
        }
    }
}

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
 * Custom command call dispatch DTO sent via RabbitMQ to the driver.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
public record CommandCallDTO(
        String recordId,
        Long tenantId,
        Long deviceId,
        Long commandId,
        String commandCode,
        Map<String, String> paramValues,
        String source,
        Long sourceUserId,
        Instant occurredAt,
        Instant expireAt,
        int schemaVersion
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String recordId;
        private Long tenantId;
        private Long deviceId;
        private Long commandId;
        private String commandCode;
        private Map<String, String> paramValues;
        private String source;
        private Long sourceUserId;
        private Instant occurredAt;
        private Instant expireAt;
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

        public Builder commandId(Long commandId) {
            this.commandId = commandId;
            return this;
        }

        public Builder commandCode(String commandCode) {
            this.commandCode = commandCode;
            return this;
        }

        public Builder paramValues(Map<String, String> paramValues) {
            this.paramValues = paramValues;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder sourceUserId(Long sourceUserId) {
            this.sourceUserId = sourceUserId;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder expireAt(Instant expireAt) {
            this.expireAt = expireAt;
            return this;
        }

        public Builder schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public CommandCallDTO build() {
            return new CommandCallDTO(recordId, tenantId, deviceId, commandId, commandCode,
                    paramValues, source, sourceUserId, occurredAt, expireAt, schemaVersion);
        }
    }
}

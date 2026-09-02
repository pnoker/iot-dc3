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

import io.github.pnoker.common.enums.CommandHistorySourceEnum;
import java.time.Instant;
import java.util.Map;

/**
 * Custom command call dispatch DTO sent via RabbitMQ to the driver.
 *
 * @author pnoker
 * @since 2026.5.23
 */
public record CommandCallDTO(
        String recordId,
        Long tenantId,
        String ownerNode,
        Long fencingToken,
        Long deviceId,
        Long commandId,
        String commandCode,
        Map<String, String> paramValues,
        CommandHistorySourceEnum source,
        Long sourceUserId,
        Instant occurredAt,
        Instant expireAt,
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
        private String ownerNode;
        private Long fencingToken;
        private Long deviceId;
        private Long commandId;
        private String commandCode;
        private Map<String, String> paramValues;
        private CommandHistorySourceEnum source;
        private Long sourceUserId;
        private Instant occurredAt;
        private Instant expireAt;
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
         * Owner node.
         *
         * @param ownerNode owner node
         * @return owner node result
         */
        public Builder ownerNode(String ownerNode) {
            this.ownerNode = ownerNode;
            return this;
        }

        /**
         * Fencing token.
         *
         * @param fencingToken fencing token
         * @return fencing token result
         */
        public Builder fencingToken(Long fencingToken) {
            this.fencingToken = fencingToken;
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
         * Command identifier.
         *
         * @param commandId command identifier
         * @return command identifier result
         */
        public Builder commandId(Long commandId) {
            this.commandId = commandId;
            return this;
        }

        /**
         * Command code.
         *
         * @param commandCode command code
         * @return command code result
         */
        public Builder commandCode(String commandCode) {
            this.commandCode = commandCode;
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
         * Source.
         *
         * @param source source
         * @return source result
         */
        public Builder source(CommandHistorySourceEnum source) {
            this.source = source;
            return this;
        }

        /**
         * Source user identifier.
         *
         * @param sourceUserId source user identifier
         * @return source user identifier result
         */
        public Builder sourceUserId(Long sourceUserId) {
            this.sourceUserId = sourceUserId;
            return this;
        }

        /**
         * Occurred at.
         *
         * @param occurredAt occurred at
         * @return occurred at result
         */
        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        /**
         * Expire at.
         *
         * @param expireAt expire at
         * @return expire at result
         */
        public Builder expireAt(Instant expireAt) {
            this.expireAt = expireAt;
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
         * Build an immutable command call from the accumulated fields.
         *
         * @return the command call DTO
         */
        public CommandCallDTO build() {
            return new CommandCallDTO(
                    recordId,
                    tenantId,
                    ownerNode,
                    fencingToken,
                    deviceId,
                    commandId,
                    commandCode,
                    paramValues,
                    source,
                    sourceUserId,
                    occurredAt,
                    expireAt,
                    schemaVersion);
        }
    }
}

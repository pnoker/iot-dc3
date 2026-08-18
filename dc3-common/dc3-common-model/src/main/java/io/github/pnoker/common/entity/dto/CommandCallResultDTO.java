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

import io.github.pnoker.common.enums.PointCommandStatusEnum;

import java.time.Instant;
import java.util.Map;

/**
 * Result receipt sent by the driver after executing a custom command.
 *
 * @author pnoker
 * @since 2026.5.23
 */
public record CommandCallResultDTO(
        String recordId,
        Long tenantId,
        PointCommandStatusEnum status,
        Map<String, String> resultValues,
        String configSnapshot,
        String errorCode,
        String errorMessage,
        Instant finishedAt,
        int schemaVersion
) {

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
        private PointCommandStatusEnum status;
        private Map<String, String> resultValues;
        private String configSnapshot;
        private String errorCode;
        private String errorMessage;
        private Instant finishedAt;
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
         * Status.
         *
         * @param status status
         * @return status result
         */
        public Builder status(PointCommandStatusEnum status) {
            this.status = status;
            return this;
        }

        /**
         * Result values.
         *
         * @param resultValues result values
         * @return result values result
         */
        public Builder resultValues(Map<String, String> resultValues) {
            this.resultValues = resultValues;
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
         * Error code.
         *
         * @param errorCode error code
         * @return error code result
         */
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * Error message.
         *
         * @param errorMessage error message
         * @return error message result
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Finished at.
         *
         * @param finishedAt finished at
         * @return finished at result
         */
        public Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
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
         * Build an immutable command result from the accumulated fields.
         *
         * @return the command result DTO
         */
        public CommandCallResultDTO build() {
            return new CommandCallResultDTO(recordId, tenantId, status, resultValues, configSnapshot,
                    errorCode, errorMessage, finishedAt, schemaVersion);
        }
    }
}

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

/**
 * Result receipt sent by the driver after executing a point command.
 * Received by {@code PointCommandResultReceiver} on the data-center side.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
public record PointCommandResultDTO(
        String commandId,
        Long tenantId,
        String status,
        String responseValue,
        String errorCode,
        String errorMessage,
        Instant finishedAt,
        int schemaVersion
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String commandId;
        private Long tenantId;
        private String status;
        private String responseValue;
        private String errorCode;
        private String errorMessage;
        private Instant finishedAt;
        private int schemaVersion;

        public Builder commandId(String commandId) {
            this.commandId = commandId;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder responseValue(String responseValue) {
            this.responseValue = responseValue;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public Builder schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public PointCommandResultDTO build() {
            return new PointCommandResultDTO(commandId, tenantId, status, responseValue, errorCode, errorMessage,
                    finishedAt, schemaVersion);
        }
    }
}

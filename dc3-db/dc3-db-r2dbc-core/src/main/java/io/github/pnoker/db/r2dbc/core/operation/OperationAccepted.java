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
package io.github.pnoker.db.r2dbc.core.operation;

import java.util.Objects;
import java.util.UUID;
/** Receipt for an accepted asynchronous operation, linking its status resource. */

public record OperationAccepted(UUID operationId, String statusUri) {
    public OperationAccepted {
        Objects.requireNonNull(operationId, "operationId must not be null");
        if (statusUri == null || statusUri.isBlank()) {
            throw new IllegalArgumentException("statusUri must not be blank");
        }
    }
}

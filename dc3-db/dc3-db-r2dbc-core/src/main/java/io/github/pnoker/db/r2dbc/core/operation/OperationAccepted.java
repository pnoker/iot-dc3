package io.github.pnoker.db.r2dbc.core.operation;

import java.util.Objects;
import java.util.UUID;

public record OperationAccepted(UUID operationId, String statusUri) {
    public OperationAccepted {
        Objects.requireNonNull(operationId, "operationId must not be null");
        if (statusUri == null || statusUri.isBlank()) {
            throw new IllegalArgumentException("statusUri must not be blank");
        }
    }
}

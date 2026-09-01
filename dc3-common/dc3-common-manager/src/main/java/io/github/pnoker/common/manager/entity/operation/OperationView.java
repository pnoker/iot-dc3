package io.github.pnoker.common.manager.entity.operation;

import io.github.pnoker.db.r2dbc.core.operation.OperationState;

import java.time.Instant;
import java.util.UUID;

public record OperationView(
        UUID operationId,
        OperationState.Status status,
        int progress,
        Object result,
        Object error,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt) {
}

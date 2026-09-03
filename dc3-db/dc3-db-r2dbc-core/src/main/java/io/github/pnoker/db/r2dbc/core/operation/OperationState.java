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

import io.github.pnoker.db.r2dbc.core.time.DatabaseInstant;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable operation state shared by HTTP 202 responses, gRPC and workers.
 * State transitions are explicit so retries cannot silently resurrect a
 * terminal operation or move it backwards.
 */
public record OperationState(
        UUID operationId,
        Long tenantId,
        String idempotencyKey,
        Status status,
        int progress,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        String requestHash,
        String result,
        String error) {

    public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 191;

    public OperationState {
        Objects.requireNonNull(operationId, "operationId must not be null");
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.codePoints().count() > MAX_IDEMPOTENCY_KEY_LENGTH
                || !idempotencyKey.equals(idempotencyKey.trim())
                || idempotencyKey.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "idempotencyKey must be 1..191 printable characters without surrounding whitespace");
        }
        Objects.requireNonNull(status, "status must not be null");
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("progress must be between 0 and 100");
        }
        createdAt = DatabaseInstant.normalize(createdAt);
        updatedAt = DatabaseInstant.normalize(updatedAt);
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not precede createdAt");
        }
        expiresAt = expiresAt == null ? null : DatabaseInstant.normalize(expiresAt);
        if (expiresAt != null && expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("expiresAt must not precede createdAt");
        }
        if (status == Status.SUCCEEDED && progress != 100) {
            throw new IllegalArgumentException("succeeded operation must have 100% progress");
        }
        if ((status == Status.PENDING || status == Status.EXPIRED) && progress != 0) {
            throw new IllegalArgumentException(status + " operation must have 0% progress");
        }
        if (requestHash == null || !requestHash.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("requestHash must be a SHA-256 hexadecimal digest");
        }
        requestHash = requestHash.toLowerCase(java.util.Locale.ROOT);
        if (status == Status.SUCCEEDED && error != null) {
            throw new IllegalArgumentException("succeeded operation must not have an error");
        }
        if (status == Status.FAILED && (error == null || error.isBlank())) {
            throw new IllegalArgumentException("failed operation must have an error");
        }
    }

    /** Create a PENDING operation at zero progress. */
    public static OperationState pending(
            UUID operationId,
            Long tenantId,
            String idempotencyKey,
            String requestHash,
            Instant now,
            Instant expiresAt) {
        return new OperationState(
                operationId,
                tenantId,
                idempotencyKey,
                Status.PENDING,
                0,
                now,
                now,
                expiresAt,
                requestHash.toLowerCase(java.util.Locale.ROOT),
                null,
                null);
    }

    /** Return the operation moved to the next status and progress. */
    public OperationState transition(Status nextStatus, int nextProgress, Instant now) {
        return transition(nextStatus, nextProgress, result, error, now);
    }

    /** Return the operation moved to the next status and progress. */
    public OperationState transition(
            Status nextStatus, int nextProgress, String nextResult, String nextError, Instant now) {
        Objects.requireNonNull(nextStatus, "nextStatus must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (!canTransitionTo(nextStatus)) {
            throw new IllegalStateException("invalid operation transition: " + status + " -> " + nextStatus);
        }
        if (nextStatus == status && nextProgress < progress) {
            throw new IllegalArgumentException("operation progress must not move backwards");
        }
        if (now.isBefore(updatedAt)) {
            throw new IllegalArgumentException("transition time must not precede updatedAt");
        }
        if (nextStatus == Status.EXPIRED && (expiresAt == null || now.isBefore(expiresAt))) {
            throw new IllegalStateException("operation cannot expire before expiresAt");
        }
        return new OperationState(
                operationId,
                tenantId,
                idempotencyKey,
                nextStatus,
                nextProgress,
                createdAt,
                now,
                expiresAt,
                requestHash,
                nextResult,
                nextError);
    }

    /** Report whether the status transition is legal. */
    public static boolean isTransitionAllowed(Status current, Status next) {
        Objects.requireNonNull(current, "current status must not be null");
        Objects.requireNonNull(next, "next status must not be null");
        return switch (current) {
            case PENDING -> next == Status.RUNNING || next == Status.CANCELLED || next == Status.EXPIRED;
            case RUNNING ->
                next == Status.RUNNING
                        || next == Status.SUCCEEDED
                        || next == Status.FAILED
                        || next == Status.CANCELLED
                        || next == Status.EXPIRED;
            case SUCCEEDED, FAILED, CANCELLED, EXPIRED -> false;
        };
    }

    private boolean canTransitionTo(Status nextStatus) {
        return isTransitionAllowed(status, nextStatus);
    }

    public enum Status {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED,
        EXPIRED
    }
}

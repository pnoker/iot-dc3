package io.github.pnoker.db.r2dbc.core.operation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationStateTest {

    private static final UUID OPERATION_ID = UUID.fromString("0198f1d4-3400-7000-8000-000000000001");
    private static final Long TENANT_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");

    @Test
    void allowsOnlyForwardTransitionsAndRequiresCompletedProgress() {
        OperationState pending = OperationState.pending(OPERATION_ID, TENANT_ID, "request-1", "a".repeat(64), NOW, null);
        OperationState running = pending.transition(OperationState.Status.RUNNING, 10, NOW.plusSeconds(1));
        OperationState progressed = running.transition(OperationState.Status.RUNNING, 60, NOW.plusMillis(1500));
        OperationState succeeded = progressed.transition(OperationState.Status.SUCCEEDED, 100, NOW.plusSeconds(2));

        assertEquals(OperationState.Status.SUCCEEDED, succeeded.status());
        assertThrows(IllegalStateException.class,
                () -> succeeded.transition(OperationState.Status.RUNNING, 20, NOW.plusSeconds(3)));
        assertThrows(IllegalArgumentException.class,
                () -> running.transition(OperationState.Status.SUCCEEDED, 99, NOW.plusSeconds(2)));
        assertTrue(OperationState.isTransitionAllowed(
                OperationState.Status.PENDING, OperationState.Status.RUNNING));
        assertFalse(OperationState.isTransitionAllowed(
                OperationState.Status.SUCCEEDED, OperationState.Status.RUNNING));
        assertThrows(IllegalArgumentException.class,
                () -> progressed.transition(OperationState.Status.RUNNING, 59, NOW.plusSeconds(2)));
    }

    @Test
    void supportsCancellationAndRejectsMalformedKeys() {
        OperationState pending = OperationState.pending(OPERATION_ID, TENANT_ID, "request-1", "a".repeat(64), NOW, null);
        assertEquals(OperationState.Status.CANCELLED,
                pending.transition(OperationState.Status.CANCELLED, 0, NOW.plusSeconds(1)).status());
        assertThrows(IllegalArgumentException.class,
                () -> OperationState.pending(OPERATION_ID, TENANT_ID, "\n", "a".repeat(64), NOW, null));
        assertThrows(IllegalArgumentException.class,
                () -> OperationState.pending(OPERATION_ID, TENANT_ID, " request-1", "a".repeat(64), NOW, null));
        assertThrows(IllegalArgumentException.class,
                () -> OperationState.pending(OPERATION_ID, TENANT_ID, "😀".repeat(192), "a".repeat(64), NOW, null));
    }

    @Test
    void normalizesAllPersistedTimesToUtcMicroseconds() {
        OperationState state = new OperationState(OPERATION_ID, TENANT_ID, "request-1",
                OperationState.Status.PENDING, 0,
                NOW.plusNanos(1234), NOW.plusNanos(5678), NOW.plusNanos(9999), "a".repeat(64), null, null);

        assertEquals(NOW.plusNanos(1_000), state.createdAt());
        assertEquals(NOW.plusNanos(5_000), state.updatedAt());
        assertEquals(NOW.plusNanos(9_000), state.expiresAt());
    }

    @Test
    void requiresFailureDetailsAndKeepsTerminalPayloadsConsistent() {
        OperationState running = OperationState.pending(OPERATION_ID, TENANT_ID, "request-1", "a".repeat(64), NOW, null)
                .transition(OperationState.Status.RUNNING, 10, NOW.plusSeconds(1));
        OperationState failed = running.transition(OperationState.Status.FAILED, 10, null,
                "{\"code\":\"IMPORT_FAILED\"}", NOW.plusSeconds(2));

        assertEquals("{\"code\":\"IMPORT_FAILED\"}", failed.error());
        assertThrows(IllegalArgumentException.class, () -> running.transition(
                OperationState.Status.FAILED, 10, null, null, NOW.plusSeconds(2)));
    }

    @Test
    void validatesRequestHashAndCarriesItAcrossTransitions() {
        String requestHash = "a".repeat(64);
        OperationState pending = OperationState.pending(OPERATION_ID, TENANT_ID, "request-1", requestHash, NOW, null);
        OperationState running = pending.transition(OperationState.Status.RUNNING, 10, NOW.plusSeconds(1));

        assertEquals(requestHash, running.requestHash());
        assertThrows(IllegalArgumentException.class,
                () -> OperationState.pending(OPERATION_ID, TENANT_ID, "request-1", "not-a-digest", NOW, null));
    }
}

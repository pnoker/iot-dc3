package io.github.pnoker.db.r2dbc.runtime.operation;

import io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect;
import io.github.pnoker.db.r2dbc.core.operation.OperationState;
import io.github.pnoker.db.r2dbc.core.tenant.TenantScope;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class R2dbcOperationRepositoryTest {

    private static final UUID OPERATION_ID = UUID.fromString("0198f1d4-3400-7000-8000-000000000001");
    private static final Long TENANT_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");

    @Test
    void rejectsCrossTenantCreateLazily() {
        R2dbcOperationRepository repository = repository();
        OperationState state = OperationState.pending(OPERATION_ID, TENANT_ID, "request-1", "a".repeat(64), NOW, null);

        StepVerifier.create(repository.create(new TenantScope(8L), state))
                .expectErrorMessage("operation tenant does not match scope")
                .verify();
    }

    @Test
    void rejectsInvalidIdempotencyKeyLazily() {
        R2dbcOperationRepository repository = repository();

        StepVerifier.create(repository.findByIdempotencyKey(new TenantScope(TENANT_ID), " bad"))
                .expectErrorMessage("idempotencyKey is invalid")
                .verify();
    }

    @Test
    void rejectsAStaleTransitionBeforeTouchingTheDatabase() {
        DatabaseClient client = mock(DatabaseClient.class);
        R2dbcOperationRepository repository = new R2dbcOperationRepository(
                client,
                mock(TransactionalOperator.class),
                new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint", '"', true));
        OperationState next = OperationState.pending(OPERATION_ID, TENANT_ID, "request-1", "a".repeat(64), NOW, null);

        StepVerifier.create(repository.transition(new TenantScope(TENANT_ID), OPERATION_ID,
                        OperationState.Status.SUCCEEDED, next))
                .expectErrorMessage("invalid operation transition: SUCCEEDED -> PENDING")
                .verify();
        verifyNoInteractions(client);
    }

    private static R2dbcOperationRepository repository() {
        return new R2dbcOperationRepository(
                mock(DatabaseClient.class),
                mock(TransactionalOperator.class),
                new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint", '"', true));
    }
}

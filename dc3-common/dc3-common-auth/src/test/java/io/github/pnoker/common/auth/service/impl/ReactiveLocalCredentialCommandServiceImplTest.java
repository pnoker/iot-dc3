package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.builder.LocalCredentialBuilder;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReactiveLocalCredentialCommandServiceImplTest {

    @Mock DatabaseClient databaseClient;
    @Mock TransactionalOperator transactionalOperator;
    @Mock ReactiveLocalCredentialService reactiveService;
    @Mock LocalCredentialBuilder builder;

    @Test
    void addRejectsInvalidInputBeforeDatabaseAccess() {
        ReactiveLocalCredentialCommandServiceImpl service = service();

        StepVerifier.create(service.add(0L, new LocalCredentialBO(), 1L, "admin"))
                .expectError().verify();

        verifyNoInteractions(databaseClient, transactionalOperator, reactiveService, builder);
    }

    @Test
    void updateRejectsMissingIdBeforeDatabaseAccess() {
        ReactiveLocalCredentialCommandServiceImpl service = service();

        StepVerifier.create(service.update(1L, new LocalCredentialBO(), 1L, "admin"))
                .expectError().verify();

        verifyNoInteractions(databaseClient, transactionalOperator, reactiveService, builder);
    }

    private ReactiveLocalCredentialCommandServiceImpl service() {
        return new ReactiveLocalCredentialCommandServiceImpl(databaseClient, transactionalOperator,
                new StandardR2dbcDialect("postgres", "public.fingerprint", '"', true), reactiveService, builder);
    }
}

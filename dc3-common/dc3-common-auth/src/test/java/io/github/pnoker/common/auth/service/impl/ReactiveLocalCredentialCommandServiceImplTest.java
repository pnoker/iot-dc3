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
package io.github.pnoker.common.auth.service.impl;

import static org.mockito.Mockito.verifyNoInteractions;

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

@ExtendWith(MockitoExtension.class)
class ReactiveLocalCredentialCommandServiceImplTest {

    @Mock
    DatabaseClient databaseClient;

    @Mock
    TransactionalOperator transactionalOperator;

    @Mock
    ReactiveLocalCredentialService reactiveService;

    @Mock
    LocalCredentialBuilder builder;

    @Test
    void addRejectsInvalidInputBeforeDatabaseAccess() {
        ReactiveLocalCredentialCommandServiceImpl service = service();

        StepVerifier.create(service.add(0L, new LocalCredentialBO(), 1L, "admin"))
                .expectError()
                .verify();

        verifyNoInteractions(databaseClient, transactionalOperator, reactiveService, builder);
    }

    @Test
    void updateRejectsMissingIdBeforeDatabaseAccess() {
        ReactiveLocalCredentialCommandServiceImpl service = service();

        StepVerifier.create(service.update(1L, new LocalCredentialBO(), 1L, "admin"))
                .expectError()
                .verify();

        verifyNoInteractions(databaseClient, transactionalOperator, reactiveService, builder);
    }

    private ReactiveLocalCredentialCommandServiceImpl service() {
        return new ReactiveLocalCredentialCommandServiceImpl(
                databaseClient,
                transactionalOperator,
                new StandardR2dbcDialect("postgres", "public.fingerprint"),
                reactiveService,
                builder);
    }
}

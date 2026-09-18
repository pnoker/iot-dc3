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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveUserCommandServiceImplTest {

    @Mock
    DatabaseClient databaseClient;

    @Mock
    TransactionalOperator transactionalOperator;

    @Mock
    ReactiveUserService reactiveUserService;

    @Mock
    UserBuilder userBuilder;

    @Test
    void addRejectsInvalidTenantBeforeOpeningDatabaseStatements() {
        ReactiveUserCommandServiceImpl service = new ReactiveUserCommandServiceImpl(
                databaseClient,
                transactionalOperator,
                new StandardR2dbcDialect("postgres", "public.fingerprint"),
                reactiveUserService,
                userBuilder);
        StepVerifier.create(service.add(0L, new UserBO(), 1L, "admin"))
                .expectError()
                .verify();
    }

    @Test
    void updateRejectsInvalidUserBeforeOpeningDatabaseStatements() {
        ReactiveUserCommandServiceImpl service = new ReactiveUserCommandServiceImpl(
                databaseClient,
                transactionalOperator,
                new StandardR2dbcDialect("postgres", "public.fingerprint"),
                reactiveUserService,
                userBuilder);
        StepVerifier.create(service.update(1L, new UserBO(), 1L, "admin"))
                .expectError()
                .verify();
    }

    @Test
    void updateSynchronizesPrincipalIdentityAndPreservesExistingEnableFlag() {
        DatabaseClient.GenericExecuteSpec spec = org.mockito.Mockito.mock(DatabaseClient.GenericExecuteSpec.class);
        FetchSpec<java.util.Map<String, Object>> fetch = org.mockito.Mockito.mock(FetchSpec.class);
        UserBO current = new UserBO();
        current.setId(10L);
        current.setPrincipalId(20L);
        current.setUserName("alice");
        current.setNickName("Alice");
        current.setEnableFlag(io.github.pnoker.common.enums.EnableFlagEnum.DISABLE);
        UserBO update = new UserBO();
        update.setId(10L);
        update.setUserName("alice-new");
        update.setNickName("Alice New");
        UserDO row = new UserDO();
        when(reactiveUserService.getById(7L, 10L)).thenReturn(Mono.just(current));
        when(userBuilder.buildDOByBO(update)).thenReturn(row);
        when(databaseClient.sql(anyString())).thenReturn(spec);
        when(spec.bind(anyString(), any())).thenReturn(spec);
        when(spec.fetch()).thenReturn(fetch);
        when(fetch.rowsUpdated()).thenReturn(Mono.just(1L));
        doAnswer(invocation -> invocation.getArgument(0))
                .when(transactionalOperator)
                .transactional(org.mockito.ArgumentMatchers.<Mono<Void>>any());

        StepVerifier.create(service().update(7L, update, 1L, "admin"))
                .expectNext(current)
                .verifyComplete();

        verify(databaseClient).sql(org.mockito.ArgumentMatchers.contains("principal_name"));
        verify(databaseClient).sql(org.mockito.ArgumentMatchers.contains("user_name"));
    }

    @Test
    void deleteFailsClosedWhenMembershipWasRemovedConcurrently() {
        DatabaseClient.GenericExecuteSpec spec = org.mockito.Mockito.mock(DatabaseClient.GenericExecuteSpec.class);
        FetchSpec<java.util.Map<String, Object>> fetch = org.mockito.Mockito.mock(FetchSpec.class);
        UserBO current = new UserBO();
        current.setId(10L);
        current.setPrincipalId(20L);
        when(reactiveUserService.getById(7L, 10L)).thenReturn(Mono.just(current));
        when(databaseClient.sql(anyString())).thenReturn(spec);
        when(spec.bind(anyString(), any())).thenReturn(spec);
        when(spec.fetch()).thenReturn(fetch);
        when(fetch.rowsUpdated()).thenReturn(Mono.just(0L));
        doAnswer(invocation -> invocation.getArgument(0))
                .when(transactionalOperator)
                .transactional(org.mockito.ArgumentMatchers.<Mono<Boolean>>any());

        StepVerifier.create(service().delete(7L, 10L, 1L, "admin"))
                .expectError(io.github.pnoker.common.exception.NotFoundException.class)
                .verify();

        verify(databaseClient, atLeastOnce()).sql(org.mockito.ArgumentMatchers.contains("dc3_tenant_membership"));
    }

    private ReactiveUserCommandServiceImpl service() {
        return new ReactiveUserCommandServiceImpl(
                databaseClient,
                transactionalOperator,
                new io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect("postgres", "public.fingerprint"),
                reactiveUserService,
                userBuilder);
    }
}

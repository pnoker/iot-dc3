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
package io.github.pnoker.common.manager.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.EventFilter;
import io.github.pnoker.common.manager.repository.ReactiveEventParamStore;
import io.github.pnoker.common.manager.repository.ReactiveEventStore;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveEventServiceImplTest {
    @Mock
    ReactiveEventStore store;

    @Mock
    ReactiveProfileStore profiles;

    @Mock
    MetadataEventPublisher publisher;

    @Mock
    ReactiveEventParamStore params;

    @Mock
    TransactionalOperator transactions;

    @Test
    void listDelegatesFilter() {
        var service = new ReactiveEventServiceImpl(store, profiles, publisher, params, transactions);
        var filter = new EventFilter(7L, "alarm", null, null, null, null, null, null, null, 0, 20, List.of());
        var page = OffsetPage.of(List.<EventBO>of(), 0, 20, 0);
        when(store.list(filter)).thenReturn(Mono.just(page));
        StepVerifier.create(service.list(filter)).expectNext(page).verifyComplete();
        verify(store).list(filter);
    }

    @Test
    void rejectsMissingIdentity() {
        StepVerifier.create(new ReactiveEventServiceImpl(store, profiles, publisher, params, transactions)
                        .getById(null, 1L))
                .expectErrorMessage("Tenant ID and event ID are required")
                .verify();
    }

    @Test
    void rejectsInvalidAddWithoutDatabaseAccess() {
        EventBO event = new EventBO();
        event.setTenantId(7L);
        StepVerifier.create(new ReactiveEventServiceImpl(store, profiles, publisher, params, transactions).add(event))
                .expectErrorMessage("Tenant ID, profile ID and event name are required")
                .verify();
        verifyNoInteractions(store, profiles, publisher);
    }

    @Test
    void deleteCascadesParamsInsideReactiveTransaction() {
        EventBO event = new EventBO();
        event.setId(1L);
        event.setTenantId(7L);
        when(store.get(7L, 1L)).thenReturn(Mono.just(event));
        when(params.deleteByEventId(7L, 1L, 9L, "operator")).thenReturn(Mono.just(2L));
        when(store.delete(7L, 1L, 3, 9L, "operator")).thenReturn(Mono.just(true));
        when(transactions.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StepVerifier.create(new ReactiveEventServiceImpl(store, profiles, publisher, params, transactions)
                        .delete(7L, 1L, 3, 9L, "operator"))
                .expectNext(true)
                .verifyComplete();
        verify(params).deleteByEventId(7L, 1L, 9L, "operator");
        verify(transactions).transactional(any(Mono.class));
    }
}

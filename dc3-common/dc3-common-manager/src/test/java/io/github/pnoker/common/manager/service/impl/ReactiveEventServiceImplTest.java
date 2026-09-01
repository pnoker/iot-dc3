package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.repository.EventFilter;
import io.github.pnoker.common.manager.repository.ReactiveEventStore;
import io.github.pnoker.common.manager.repository.ReactiveEventParamStore;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.test.StepVerifier;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ReactiveEventServiceImplTest {
    @Mock ReactiveEventStore store;
    @Mock ReactiveProfileStore profiles;
    @Mock MetadataEventPublisher publisher;
    @Mock ReactiveEventParamStore params;
    @Mock TransactionalOperator transactions;
    @Test void listDelegatesFilter() {
        var service = new ReactiveEventServiceImpl(store, profiles, publisher, params, transactions);
        var filter = new EventFilter(7L, "alarm", null, null, null, null, null, null, null, 0, 20, List.of());
        var page = OffsetPage.of(List.<EventBO>of(), 0, 20, 0);
        when(store.list(filter)).thenReturn(Mono.just(page));
        StepVerifier.create(service.list(filter)).expectNext(page).verifyComplete();
        verify(store).list(filter);
    }
    @Test void rejectsMissingIdentity() {
        StepVerifier.create(new ReactiveEventServiceImpl(store, profiles, publisher, params, transactions).getById(null, 1L)).expectErrorMessage("Tenant ID and event ID are required").verify();
    }
    @Test void rejectsInvalidAddWithoutDatabaseAccess() {
        EventBO event = new EventBO(); event.setTenantId(7L);
        StepVerifier.create(new ReactiveEventServiceImpl(store, profiles, publisher, params, transactions).add(event)).expectErrorMessage("Tenant ID, profile ID and event name are required").verify();
        verifyNoInteractions(store, profiles, publisher);
    }
    @Test void deleteCascadesParamsInsideReactiveTransaction() {
        EventBO event = new EventBO(); event.setId(1L); event.setTenantId(7L);
        when(store.get(7L, 1L)).thenReturn(Mono.just(event));
        when(params.deleteByEventId(7L, 1L, 9L, "operator")).thenReturn(Mono.just(2L));
        when(store.delete(7L, 1L, 3, 9L, "operator")).thenReturn(Mono.just(true));
        when(transactions.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StepVerifier.create(new ReactiveEventServiceImpl(store, profiles, publisher, params, transactions).delete(7L, 1L, 3, 9L, "operator"))
                .expectNext(true).verifyComplete();
        verify(params).deleteByEventId(7L, 1L, 9L, "operator");
        verify(transactions).transactional(any(Mono.class));
    }
}

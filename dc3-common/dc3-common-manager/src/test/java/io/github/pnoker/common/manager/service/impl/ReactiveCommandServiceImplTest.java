package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.repository.CommandFilter;
import io.github.pnoker.common.manager.repository.ReactiveCommandStore;
import io.github.pnoker.common.manager.repository.ReactiveCommandParamStore;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
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
class ReactiveCommandServiceImplTest {
    @Mock ReactiveCommandStore store;
    @Mock ReactiveProfileStore profiles;
    @Mock MetadataEventPublisher publisher;
    @Mock ReactiveCommandParamStore params;
    @Mock TransactionalOperator transactions;
    @Test void listDelegatesFilter() {
        var service = new ReactiveCommandServiceImpl(store, profiles, publisher, params, transactions);
        var filter = new CommandFilter(7L, "read", null, null, null, null, null, null, null, 0, 20, List.of());
        var page = OffsetPage.of(List.<CommandBO>of(), 0, 20, 0);
        when(store.list(filter)).thenReturn(Mono.just(page));
        StepVerifier.create(service.list(filter)).expectNext(page).verifyComplete();
        verify(store).list(filter);
    }
    @Test void rejectsMissingIdentity() {
        StepVerifier.create(new ReactiveCommandServiceImpl(store, profiles, publisher, params, transactions).getById(null, 1L)).expectErrorMessage("Tenant ID and command ID are required").verify();
    }
    @Test void rejectsInvalidAddWithoutDatabaseAccess() {
        CommandBO command = new CommandBO(); command.setTenantId(7L);
        StepVerifier.create(new ReactiveCommandServiceImpl(store, profiles, publisher, params, transactions).add(command)).expectErrorMessage("Tenant ID, profile ID, command name and timeout are required").verify();
        verifyNoInteractions(store, profiles, publisher);
    }
    @Test void deleteCascadesParamsInsideReactiveTransaction() {
        CommandBO command = new CommandBO(); command.setId(1L); command.setTenantId(7L);
        when(store.get(7L, 1L)).thenReturn(Mono.just(command));
        when(params.deleteByCommandId(7L, 1L, 9L, "operator")).thenReturn(Mono.just(2L));
        when(store.delete(7L, 1L, 3, 9L, "operator")).thenReturn(Mono.just(true));
        when(transactions.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StepVerifier.create(new ReactiveCommandServiceImpl(store, profiles, publisher, params, transactions).delete(7L, 1L, 3, 9L, "operator"))
                .expectNext(true).verifyComplete();
        verify(params).deleteByCommandId(7L, 1L, 9L, "operator");
        verify(transactions).transactional(any(Mono.class));
    }
}

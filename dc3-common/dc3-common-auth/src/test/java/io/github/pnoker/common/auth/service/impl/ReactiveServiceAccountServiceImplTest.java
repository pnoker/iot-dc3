package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.builder.ServiceAccountBuilder;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.common.auth.repository.ReactiveServiceAccountStore;
import io.github.pnoker.common.auth.repository.ServiceAccountFilter;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveServiceAccountServiceImplTest {
    @Mock ReactiveServiceAccountStore store;
    @Mock ServiceAccountBuilder builder;
    @Mock ReactiveTenantMembershipService membershipService;

    @Test
    void listMapsOffsetPageAndRows() {
        ServiceAccountDO row = new ServiceAccountDO();
        ServiceAccountBO bo = new ServiceAccountBO();
        when(store.list(any(ServiceAccountFilter.class))).thenReturn(Mono.just(OffsetPage.of(java.util.List.of(row), 20, 10, 31)));
        when(builder.buildBOByDO(row)).thenReturn(bo);
        StepVerifier.create(service().list(new ServiceAccountFilter(7L, null, null, null, null,
                        new io.github.pnoker.db.r2dbc.core.page.PageRequest(20, 10, java.util.List.of()))))
                .assertNext(page -> {
                    assertThat(page.items()).containsExactly(bo);
                    assertThat(page.offset()).isEqualTo(20);
                    assertThat(page.limit()).isEqualTo(10);
                    assertThat(page.total()).isEqualTo(31);
                }).verifyComplete();
    }

    @Test
    void missingAccountReturnsNotFound() {
        when(store.getById(7L, 99L)).thenReturn(Mono.empty());
        StepVerifier.create(service().getById(7L, 99L)).expectErrorMessage("Service account").verify();
    }

    @Test
    void deletePropagatesStoreResult() {
        when(store.delete(7L, 99L, 1L, "admin")).thenReturn(Mono.just(true));
        StepVerifier.create(service().delete(7L, 99L, 1L, "admin")).verifyComplete();
        verify(store).delete(7L, 99L, 1L, "admin");
    }

    @Test
    void addRequiresTenantMemberBeforeInsert() {
        ServiceAccountBO account = new ServiceAccountBO();
        account.setTenantId(7L);
        account.setOwnerPrincipalId(9L);
        ServiceAccountDO row = new ServiceAccountDO();
        when(membershipService.requireTenantMember(7L, 9L)).thenReturn(Mono.just(new io.github.pnoker.common.auth.entity.bo.TenantMembershipBO()));
        when(store.insert(account)).thenReturn(Mono.just(row));
        when(builder.buildBOByDO(row)).thenReturn(account);
        StepVerifier.create(service().add(account)).expectNext(account).verifyComplete();
        verify(membershipService).requireTenantMember(7L, 9L);
        verify(store).insert(account);
    }

    private ReactiveServiceAccountServiceImpl service() {
        return new ReactiveServiceAccountServiceImpl(store, builder, membershipService);
    }
}

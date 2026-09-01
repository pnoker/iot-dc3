package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.TenantMembershipBuilder;
import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import io.github.pnoker.common.auth.repository.ReactiveTenantMembershipStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveTenantMembershipServiceImplTest {

    @Mock ReactiveTenantMembershipStore store;
    @Mock TenantMembershipBuilder builder;

    @Test
    void invalidTenantIsRejectedBeforeStoreAccess() {
        ReactiveTenantMembershipServiceImpl service = new ReactiveTenantMembershipServiceImpl(store, builder);

        StepVerifier.create(service.isTenantMember(0L, 10L)).expectNext(false).verifyComplete();
        StepVerifier.create(service.getByTenantAndPrincipal(0L, 10L)).expectError().verify();

        verifyNoInteractions(store);
    }

    @Test
    void membershipLookupMapsAndRequiresActiveMembership() {
        TenantMembershipDO row = new TenantMembershipDO();
        TenantMembershipBO value = new TenantMembershipBO();
        when(store.getByTenantAndPrincipal(7L, 10L)).thenReturn(Mono.just(row));
        when(builder.buildBOByDO(row)).thenReturn(value);

        StepVerifier.create(new ReactiveTenantMembershipServiceImpl(store, builder)
                        .requireTenantMember(7L, 10L))
                .expectNext(value).verifyComplete();
    }
}

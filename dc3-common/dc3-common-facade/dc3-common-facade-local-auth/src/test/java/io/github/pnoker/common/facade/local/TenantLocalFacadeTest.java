package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.service.ReactiveTenantService;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.local.builder.FacadeTenantBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantLocalFacadeTest {

    @Mock
    private ReactiveTenantService tenantService;

    @Mock
    private FacadeTenantBuilder tenantBuilder;

    private TenantLocalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new TenantLocalFacade(tenantService, tenantBuilder);
    }

    @Test
    void getByCodeCompletesEmptyWhenTenantMissing() {
        when(tenantService.getByCode("acme")).thenReturn(Mono.empty());

        StepVerifier.create(facade.getByCode("acme")).verifyComplete();

        verify(tenantBuilder, never()).toFacadeBO(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getByCodeMapsTenantWithoutBlocking() {
        TenantBO tenant = new TenantBO();
        FacadeTenantBO mapped = new FacadeTenantBO();
        when(tenantService.getByCode("acme")).thenReturn(Mono.just(tenant));
        when(tenantBuilder.toFacadeBO(tenant)).thenReturn(mapped);

        StepVerifier.create(facade.getByCode("acme")).expectNext(mapped).verifyComplete();
    }
}

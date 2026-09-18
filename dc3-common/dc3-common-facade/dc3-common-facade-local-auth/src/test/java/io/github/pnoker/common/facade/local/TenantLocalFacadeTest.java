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
package io.github.pnoker.common.facade.local;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

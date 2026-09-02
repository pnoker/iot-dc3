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
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class ReactiveTenantMembershipServiceImplTest {

    @Mock
    ReactiveTenantMembershipStore store;

    @Mock
    TenantMembershipBuilder builder;

    @Test
    void invalidTenantIsRejectedBeforeStoreAccess() {
        ReactiveTenantMembershipServiceImpl service = new ReactiveTenantMembershipServiceImpl(store, builder);

        StepVerifier.create(service.isTenantMember(0L, 10L)).expectNext(false).verifyComplete();
        StepVerifier.create(service.getByTenantAndPrincipal(0L, 10L))
                .expectError()
                .verify();

        verifyNoInteractions(store);
    }

    @Test
    void membershipLookupMapsAndRequiresActiveMembership() {
        TenantMembershipDO row = new TenantMembershipDO();
        TenantMembershipBO value = new TenantMembershipBO();
        when(store.getByTenantAndPrincipal(7L, 10L)).thenReturn(Mono.just(row));
        when(builder.buildBOByDO(row)).thenReturn(value);

        StepVerifier.create(new ReactiveTenantMembershipServiceImpl(store, builder).requireTenantMember(7L, 10L))
                .expectNext(value)
                .verifyComplete();
    }
}

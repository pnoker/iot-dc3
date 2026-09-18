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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.auth.entity.bo.TenantMembershipBO;
import io.github.pnoker.common.auth.entity.builder.TenantMembershipBuilder;
import io.github.pnoker.common.auth.entity.model.TenantMembershipDO;
import io.github.pnoker.common.auth.repository.ReactiveTenantMembershipCommandStore;
import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveTenantMembershipCommandServiceImplTest {

    @Mock
    private ReactiveTenantMembershipCommandStore store;

    @Mock
    private TenantMembershipBuilder builder;

    @Test
    void addMapsAndPersistsMembership() {
        TenantMembershipBO input = membership(7L, 9L);
        TenantMembershipDO row = new TenantMembershipDO();
        TenantMembershipBO saved = membership(7L, 9L);
        when(builder.buildDOByBO(input)).thenReturn(row);
        when(store.insert(row)).thenReturn(Mono.just(row));
        when(builder.buildBOByDO(row)).thenReturn(saved);

        StepVerifier.create(new ReactiveTenantMembershipCommandServiceImpl(store, builder).add(input))
                .expectNext(saved)
                .verifyComplete();
        verify(store).insert(row);
    }

    @Test
    void invalidDeleteIsRejectedWithoutStoreAccess() {
        StepVerifier.create(
                        new ReactiveTenantMembershipCommandServiceImpl(store, builder).delete(0L, 1L, 2L, "operator"))
                .expectError()
                .verify();
    }

    private static TenantMembershipBO membership(Long tenantId, Long principalId) {
        TenantMembershipBO value = new TenantMembershipBO();
        value.setTenantId(tenantId);
        value.setPrincipalId(principalId);
        value.setPrincipalType(PrincipalTypeEnum.USER);
        value.setMembershipStatus(MembershipStatusEnum.ACTIVE);
        return value;
    }
}

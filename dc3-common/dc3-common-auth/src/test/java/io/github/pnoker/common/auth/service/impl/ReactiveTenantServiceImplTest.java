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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.builder.TenantBuilder;
import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.auth.repository.ReactiveTenantStore;
import io.github.pnoker.common.auth.repository.TenantFilter;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveTenantServiceImplTest {

    @Mock
    private ReactiveTenantStore tenantStore;

    @Mock
    private TenantBuilder tenantBuilder;

    private ReactiveTenantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReactiveTenantServiceImpl(tenantStore, tenantBuilder);
    }

    @Test
    void getByCodeReturnsEmptyForUnknownTenant() {
        when(tenantStore.getEnabledByCode("missing")).thenReturn(Mono.empty());

        StepVerifier.create(service.getByCode("missing")).verifyComplete();
    }

    @Test
    void addRejectsDuplicateBeforeInsert() {
        TenantBO tenant = tenant("acme", "acme");
        when(tenantStore.getByNameAndCode("acme", "acme")).thenReturn(Mono.just(new TenantDO()));

        StepVerifier.create(service.add(tenant))
                .expectError(DuplicateException.class)
                .verify();
        verify(tenantStore, never()).insert(any());
    }

    @Test
    void addMapsPersistedTenant() {
        TenantBO tenant = tenant("acme", "acme");
        TenantDO persisted = new TenantDO();
        persisted.setId(42L);
        TenantBO mapped = tenant("acme", "acme");
        mapped.setId(42L);
        when(tenantStore.getByNameAndCode("acme", "acme")).thenReturn(Mono.empty());
        when(tenantStore.insert(any(TenantDO.class))).thenReturn(Mono.just(persisted));
        when(tenantBuilder.buildDOByBO(tenant)).thenReturn(new TenantDO());
        when(tenantBuilder.buildBOByDO(persisted)).thenReturn(mapped);

        StepVerifier.create(service.add(tenant))
                .assertNext(result -> assertThat(result.getId()).isEqualTo(42L))
                .verifyComplete();
    }

    @Test
    void updateRequiresExistingTenant() {
        TenantBO tenant = tenant("acme", "acme");
        tenant.setId(42L);
        when(tenantStore.getById(42L)).thenReturn(Mono.empty());

        StepVerifier.create(service.update(tenant))
                .expectError(NotFoundException.class)
                .verify();
        verify(tenantStore, never()).update(any());
    }

    @Test
    void listUsesStorePageAndMapsItems() {
        TenantDO row = new TenantDO();
        TenantBO mapped = tenant("acme", "acme");
        when(tenantStore.list(any(TenantFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(row), 0, 50, 1)));
        when(tenantBuilder.buildBOByDO(row)).thenReturn(mapped);

        StepVerifier.create(service.list(new TenantFilter(null, null, EnableFlagEnum.ENABLE, 0, 50, List.of())))
                .assertNext(page -> {
                    assertThat(page.items()).containsExactly(mapped);
                    assertThat(page.total()).isEqualTo(1);
                    assertThat(page.hasNext()).isFalse();
                })
                .verifyComplete();
    }

    private static TenantBO tenant(String name, String code) {
        TenantBO tenant = new TenantBO();
        tenant.setTenantName(name);
        tenant.setTenantCode(code);
        return tenant;
    }
}

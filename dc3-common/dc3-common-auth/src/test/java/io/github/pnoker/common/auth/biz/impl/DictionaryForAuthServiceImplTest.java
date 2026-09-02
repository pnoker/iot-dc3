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
package io.github.pnoker.common.auth.biz.impl;

import static org.mockito.Mockito.when;

import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.auth.repository.ReactiveTenantDictionaryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DictionaryForAuthServiceImplTest {
    @Mock
    private ReactiveTenantDictionaryStore tenantStore;

    @InjectMocks
    private DictionaryForAuthServiceImpl service;

    @Test
    void listTenantOptionsMapsEnabledTenantsReactively() {
        TenantDO first = new TenantDO();
        first.setId(1L);
        first.setTenantName("Acme");
        TenantDO second = new TenantDO();
        second.setId(2L);
        second.setTenantName("Globex");
        when(tenantStore.listEnabled()).thenReturn(Flux.just(first, second));

        StepVerifier.create(service.listTenantOptions())
                .assertNext(options -> {
                    org.assertj.core.api.Assertions.assertThat(options)
                            .extracting(option -> option.label() + ":" + option.value())
                            .containsExactly("Acme:1", "Globex:2");
                })
                .verifyComplete();
    }
}

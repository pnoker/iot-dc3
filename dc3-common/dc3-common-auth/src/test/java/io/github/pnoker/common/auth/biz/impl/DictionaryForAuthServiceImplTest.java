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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.pnoker.common.auth.dal.TenantManager;
import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.dal.entity.bo.DictionaryBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryForAuthServiceImplTest {

    @Mock
    private TenantManager tenantManager;

    @InjectMocks
    private DictionaryForAuthServiceImpl service;

    @Test
    void tenantDictionaryReturnsEmptyWhenNoEnabledTenants() {
        when(tenantManager.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertThat(service.tenantDictionary()).isEmpty();
    }

    @Test
    void tenantDictionaryMapsTenantsToLabelValuePairs() {
        TenantDO first = new TenantDO();
        first.setId(1L);
        first.setTenantName("Acme");
        TenantDO second = new TenantDO();
        second.setId(2L);
        second.setTenantName("Globex");
        when(tenantManager.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(first, second));

        List<DictionaryBO> result = service.tenantDictionary();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLabel()).isEqualTo("Acme");
        assertThat(result.get(0).getValue()).isEqualTo("1");
        assertThat(result.get(1).getLabel()).isEqualTo("Globex");
        assertThat(result.get(1).getValue()).isEqualTo("2");
    }
}

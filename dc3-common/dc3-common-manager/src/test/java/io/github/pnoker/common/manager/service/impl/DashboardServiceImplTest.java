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
package io.github.pnoker.common.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.manager.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.manager.repository.ReactiveDashboardStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {
    @Mock
    private ReactiveDashboardStore dashboardStore;

    @Mock
    private PointValueFacade pointValueFacade;

    @Test
    void driverStatsUsesReactiveStoreAndPreservesTenantScope() {
        BucketRow enabled = new BucketRow();
        enabled.setBucketKey((byte) 0);
        enabled.setCount(2);
        when(dashboardStore.countDriverByEnable(7L)).thenReturn(Flux.just(enabled));
        when(dashboardStore.countDriverByType(7L)).thenReturn(Flux.empty());
        when(dashboardStore.countDriverByService(7L)).thenReturn(Flux.empty());

        var result = new DashboardServiceImpl(dashboardStore, pointValueFacade)
                .driverStats(7L)
                .block();

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getByEnable()).singleElement().satisfies(bucket -> {
            assertThat(bucket.getKey()).isEqualTo("ENABLE");
            assertThat(bucket.getCount()).isEqualTo(2);
        });
    }
}

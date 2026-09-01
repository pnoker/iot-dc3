package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.manager.repository.ReactiveDashboardStore;
import io.github.pnoker.common.facade.api.PointValueFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {
    @Mock private ReactiveDashboardStore dashboardStore;
    @Mock private PointValueFacade pointValueFacade;

    @Test
    void driverStatsUsesReactiveStoreAndPreservesTenantScope() {
        BucketRow enabled = new BucketRow();
        enabled.setBucketKey((byte) 0);
        enabled.setCount(2);
        when(dashboardStore.countDriverByEnable(7L)).thenReturn(Flux.just(enabled));
        when(dashboardStore.countDriverByType(7L)).thenReturn(Flux.empty());
        when(dashboardStore.countDriverByService(7L)).thenReturn(Flux.empty());

        var result = new DashboardServiceImpl(dashboardStore, pointValueFacade).driverStats(7L).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getByEnable()).singleElement().satisfies(bucket -> {
            assertThat(bucket.getKey()).isEqualTo("ENABLE");
            assertThat(bucket.getCount()).isEqualTo(2);
        });
    }
}

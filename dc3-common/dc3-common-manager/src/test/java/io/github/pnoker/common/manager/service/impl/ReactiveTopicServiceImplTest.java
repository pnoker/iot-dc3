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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.TopicOffsetQuery;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveTopicServiceImplTest {
    @Mock
    private ReactiveDeviceService deviceService;

    @Mock
    private ReactivePointService pointService;

    @Test
    void buildsTenantScopedTopicProjectionAndAppliesOffsetAfterFlattening() {
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        device.setTenantId(7L);
        device.setDeviceName("Gateway");
        PointBO pointA = new PointBO();
        pointA.setId(1L);
        pointA.setPointName("A");
        pointA.setTenantId(7L);
        PointBO pointB = new PointBO();
        pointB.setId(2L);
        pointB.setPointName("B");
        pointB.setTenantId(7L);
        when(deviceService.list(any())).thenReturn(Mono.just(OffsetPage.of(List.of(device), 0, 200, 1)));
        when(pointService.listByDeviceId(7L, 10L)).thenReturn(Flux.just(pointA, pointB));

        StepVerifier.create(new ReactiveTopicServiceImpl(deviceService, pointService)
                        .list(new TopicOffsetQuery(7L, null, null, 1, 1)))
                .assertNext(page -> {
                    assertThat(page.items()).hasSize(1);
                    assertThat(page.items().get(0).getPointName()).isEqualTo("B");
                    assertThat(page.offset()).isEqualTo(1);
                    assertThat(page.hasNext()).isFalse();
                })
                .verifyComplete();
    }
}

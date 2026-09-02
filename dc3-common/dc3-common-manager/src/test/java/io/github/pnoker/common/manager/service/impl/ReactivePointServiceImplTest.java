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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.repository.ReactiveDeviceStore;
import io.github.pnoker.common.manager.repository.ReactivePointAttributeConfigStore;
import io.github.pnoker.common.manager.repository.ReactivePointStore;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactivePointServiceImplTest {

    @Mock
    private ReactivePointStore pointStore;

    @Mock
    private ReactiveProfileStore profileStore;

    @Mock
    private ReactiveDeviceStore deviceStore;

    @Mock
    private ReactivePointAttributeConfigStore pointAttributeConfigStore;

    @Test
    void listsOnlyTenantScopedNonNullUnits() {
        PointBO temperature = point(1L, "°C");
        PointBO state = point(2L, null);
        when(pointStore.listByIds(7L, List.of(1L, 2L))).thenReturn(Flux.just(temperature, state));

        StepVerifier.create(service().listUnits(7L, List.of(1L, 2L)))
                .assertNext(units -> {
                    org.assertj.core.api.Assertions.assertThat(units).hasSize(1).containsEntry("1", "°C");
                    org.assertj.core.api.Assertions.assertThat(units).isUnmodifiable();
                })
                .verifyComplete();

        verify(pointStore).listByIds(7L, List.of(1L, 2L));
    }

    @Test
    void returnsConfiguredDevicesForOwnedPoint() {
        PointBO point = point(3L, "kPa");
        DeviceBO device = new DeviceBO();
        device.setId(11L);
        device.setTenantId(7L);
        when(pointStore.get(7L, 3L)).thenReturn(Mono.just(point));
        when(pointStore.listConfiguredDeviceIdsByPointId(7L, 3L)).thenReturn(Flux.just(11L));
        when(deviceStore.listByIds(7L, List.of(11L))).thenReturn(Flux.just(device));

        StepVerifier.create(service().getDeviceStatisticsByPointId(7L, 3L))
                .assertNext(result -> {
                    org.assertj.core.api.Assertions.assertThat(result.getCount())
                            .isEqualTo(1L);
                    org.assertj.core.api.Assertions.assertThat(result.getDevices())
                            .containsExactly(device);
                })
                .verifyComplete();
    }

    @Test
    void countsPointsOnlyAfterDeviceOwnershipValidation() {
        DeviceBO device = new DeviceBO();
        device.setId(11L);
        device.setTenantId(7L);
        when(deviceStore.get(7L, 11L)).thenReturn(Mono.just(device));
        when(pointStore.countByDeviceId(7L, 11L)).thenReturn(Mono.just(4L));

        StepVerifier.create(service().getCountByDeviceId(7L, 11L))
                .expectNext(4L)
                .verifyComplete();
    }

    @Test
    void separatesConfiguredAndUnconfiguredPoints() {
        DeviceBO device = new DeviceBO();
        device.setId(11L);
        device.setTenantId(7L);
        PointBO configured = point(1L, "°C");
        PointBO unconfigured = point(2L, "%");
        PointAttributeConfigBO config = new PointAttributeConfigBO();
        config.setPointId(1L);
        when(deviceStore.get(7L, 11L)).thenReturn(Mono.just(device));
        when(pointStore.listByDeviceId(7L, 11L)).thenReturn(Flux.just(configured, unconfigured));
        when(pointAttributeConfigStore.listByDeviceId(7L, 11L)).thenReturn(Flux.just(config));

        StepVerifier.create(service().getPointConfigByDeviceId(7L, 11L))
                .assertNext(result -> {
                    org.assertj.core.api.Assertions.assertThat(result.getConfigCount())
                            .isEqualTo(1L);
                    org.assertj.core.api.Assertions.assertThat(result.getUnConfigCount())
                            .isEqualTo(1L);
                    org.assertj.core.api.Assertions.assertThat(result.getPoints())
                            .containsExactly(configured);
                })
                .verifyComplete();
    }

    private ReactivePointServiceImpl service() {
        return new ReactivePointServiceImpl(pointStore, profileStore, deviceStore, pointAttributeConfigStore);
    }

    private PointBO point(Long id, String unit) {
        PointBO point = new PointBO();
        point.setId(id);
        point.setTenantId(7L);
        point.setUnit(unit);
        return point;
    }
}

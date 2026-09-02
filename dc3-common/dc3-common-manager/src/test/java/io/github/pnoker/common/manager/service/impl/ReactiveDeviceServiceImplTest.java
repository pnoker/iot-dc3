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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.ReactiveDeviceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveDeviceServiceImplTest {

    @Mock
    private ReactiveDeviceStore deviceStore;

    @Mock
    private MetadataEventPublisher metadataEventPublisher;

    private ReactiveDeviceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReactiveDeviceServiceImpl(deviceStore, metadataEventPublisher);
    }

    @Test
    void addRejectsDuplicateNameWithinTenant() {
        DeviceBO existing = device(10L, 7L, "sensor");
        when(deviceStore.getByName(7L, "sensor")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.add(device(null, 7L, "sensor")))
                .expectErrorMessage("Failed to create device: device has been duplicated")
                .verify();
        verify(deviceStore, never()).insert(any());
    }

    @Test
    void addPersistsAndPublishesMetadataEvent() {
        DeviceBO source = device(null, 7L, "sensor");
        DeviceBO saved = device(10L, 7L, "sensor");
        when(deviceStore.getByName(7L, "sensor")).thenReturn(Mono.empty());
        when(deviceStore.insert(source)).thenReturn(Mono.just(saved));

        StepVerifier.create(service.add(source))
                .assertNext(result -> assertThat(result).isSameAs(saved))
                .verifyComplete();
        verify(metadataEventPublisher).publishEvent(any());
    }

    @Test
    void updateRequiresVersionAndMapsConflictToError() {
        DeviceBO source = device(10L, 7L, "sensor");
        source.setVersion(null);

        StepVerifier.create(service.update(source))
                .expectErrorMessage("Device ID and version are required for update")
                .verify();
        verify(deviceStore, never()).update(any(), any(Integer.class));
    }

    @Test
    void deleteIsTenantScopedAndPublishesAfterSuccessfulDelete() {
        when(deviceStore.get(7L, 10L)).thenReturn(Mono.just(device(10L, 7L, "sensor")));
        when(deviceStore.delete(eq(7L), eq(10L), eq(4), eq(3L), eq("operator"))).thenReturn(Mono.just(true));

        StepVerifier.create(service.delete(7L, 10L, 4, 3L, "operator"))
                .expectNext(true)
                .verifyComplete();
        verify(metadataEventPublisher).publishEvent(any());
    }

    @Test
    void deleteRejectsStaleVersionAsConflict() {
        when(deviceStore.get(7L, 10L)).thenReturn(Mono.just(device(10L, 7L, "sensor")));
        when(deviceStore.delete(7L, 10L, 3, 4L, "operator")).thenReturn(Mono.just(false));

        StepVerifier.create(service.delete(7L, 10L, 3, 4L, "operator"))
                .expectErrorMessage("Device version conflict")
                .verify();
        verify(metadataEventPublisher, never()).publishEvent(any());
    }

    private DeviceBO device(Long id, Long tenantId, String name) {
        DeviceBO value = new DeviceBO();
        value.setId(id);
        value.setTenantId(tenantId);
        value.setDeviceName(name);
        value.setDeviceCode("code");
        value.setDriverId(20L);
        value.setVersion(0);
        return value;
    }
}

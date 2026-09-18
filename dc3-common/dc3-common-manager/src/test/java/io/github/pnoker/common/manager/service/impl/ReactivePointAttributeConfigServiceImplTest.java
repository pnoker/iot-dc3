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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.ReactivePointAttributeConfigStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactivePointAttributeConfigServiceImplTest {
    @Mock
    ReactivePointAttributeConfigStore store;

    @Mock
    ReactivePointAttributeService attributes;

    @Mock
    ReactiveDeviceService devices;

    @Mock
    ReactivePointService points;

    @Mock
    MetadataEventPublisher publisher;

    @Test
    void addRejectsDuplicateWithinTenant() {
        PointAttributeConfigBO value = value();
        validRelations();
        when(store.getByAttributeDevicePoint(7L, 11L, 12L, 13L)).thenReturn(Mono.just(value));

        StepVerifier.create(service().add(value))
                .expectError(DuplicateException.class)
                .verify();
        verify(store, never()).insert(any());
    }

    @Test
    void addRejectsPointFromDifferentProfile() {
        PointAttributeConfigBO value = value();
        validRelations();
        PointBO point = point();
        point.setProfileId(99L);
        when(points.getById(7L, 13L)).thenReturn(Mono.just(point));

        StepVerifier.create(service().add(value))
                .expectError(NotFoundException.class)
                .verify();
        verify(store, never()).getByAttributeDevicePoint(any(), any(), any(), any());
        verify(store, never()).insert(any());
    }

    @Test
    void updateUsesExpectedVersionAndPublishesMetadata() {
        PointAttributeConfigBO value = value();
        value.setId(99L);
        value.setVersion(2);
        PointAttributeConfigBO saved = value();
        saved.setId(99L);
        saved.setVersion(3);
        validRelations();
        when(store.get(7L, 99L)).thenReturn(Mono.just(value));
        when(store.getByAttributeDevicePoint(7L, 11L, 12L, 13L)).thenReturn(Mono.just(value));
        when(store.update(any(), eq(2))).thenReturn(Mono.just(saved));

        StepVerifier.create(service().update(value)).expectNext(saved).verifyComplete();
        verify(store).update(value, 2);
        verify(publisher).publishEvent(any());
    }

    @Test
    void addFailsWhenRelationLookupIsEmpty() {
        PointAttributeConfigBO value = value();
        when(attributes.getById(7L, 11L)).thenReturn(Mono.empty());
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));
        when(points.getById(7L, 13L)).thenReturn(Mono.just(point()));

        StepVerifier.create(service().add(value))
                .expectError(NotFoundException.class)
                .verify();
        verify(store, never()).insert(any());
    }

    private ReactivePointAttributeConfigServiceImpl service() {
        return new ReactivePointAttributeConfigServiceImpl(store, attributes, devices, points, publisher);
    }

    private void validRelations() {
        when(attributes.getById(7L, 11L)).thenReturn(Mono.just(attribute()));
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));
        when(points.getById(7L, 13L)).thenReturn(Mono.just(point()));
    }

    private PointAttributeConfigBO value() {
        PointAttributeConfigBO value = new PointAttributeConfigBO();
        value.setTenantId(7L);
        value.setAttributeId(11L);
        value.setDeviceId(12L);
        value.setPointId(13L);
        value.setEnableFlag(EnableFlagEnum.ENABLE);
        value.setConfigValue("x");
        return value;
    }

    private PointAttributeBO attribute() {
        PointAttributeBO value = new PointAttributeBO();
        value.setTenantId(7L);
        value.setId(11L);
        value.setDriverId(21L);
        return value;
    }

    private DeviceBO device() {
        DeviceBO value = new DeviceBO();
        value.setTenantId(7L);
        value.setId(12L);
        value.setDriverId(21L);
        value.setProfileId(31L);
        return value;
    }

    private PointBO point() {
        PointBO value = new PointBO();
        value.setTenantId(7L);
        value.setId(13L);
        value.setProfileId(31L);
        return value;
    }
}

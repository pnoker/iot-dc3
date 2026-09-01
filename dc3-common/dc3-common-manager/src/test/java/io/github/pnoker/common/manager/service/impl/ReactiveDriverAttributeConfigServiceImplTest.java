package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.ReactiveDriverAttributeConfigStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveDriverAttributeConfigServiceImplTest {
    @Mock
    ReactiveDriverAttributeConfigStore store;
    @Mock
    ReactiveDriverAttributeService attributes;
    @Mock
    ReactiveDeviceService devices;
    @Mock
    MetadataEventPublisher publisher;

    @Test
    void addRejectsDuplicateWithinTenantBeforeInsert() {
        DriverAttributeConfigBO value = value();
        when(attributes.getById(7L, 11L)).thenReturn(Mono.just(attribute()));
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));
        when(store.getByAttributeAndDevice(7L, 11L, 12L)).thenReturn(Mono.just(value));

        StepVerifier.create(service().add(value))
                .expectError(DuplicateException.class)
                .verify();
        verify(store, never()).insert(any());
    }

    @Test
    void addRejectsAttributeFromDifferentDriver() {
        DriverAttributeConfigBO value = value();
        DriverAttributeBO attribute = attribute();
        attribute.setDriverId(99L);
        when(attributes.getById(7L, 11L)).thenReturn(Mono.just(attribute));
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));

        StepVerifier.create(service().add(value))
                .expectError(NotFoundException.class)
                .verify();
        verify(store, never()).getByAttributeAndDevice(any(), any(), any());
        verify(store, never()).insert(any());
    }

    @Test
    void updatePassesExpectedVersionAndPublishesMetadata() {
        DriverAttributeConfigBO value = value();
        value.setId(99L);
        value.setVersion(2);
        DriverAttributeConfigBO saved = value();
        saved.setId(99L);
        saved.setVersion(3);
        when(attributes.getById(7L, 11L)).thenReturn(Mono.just(attribute()));
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));
        when(store.get(7L, 99L)).thenReturn(Mono.just(value));
        when(store.getByAttributeAndDevice(7L, 11L, 12L)).thenReturn(Mono.just(value));
        when(store.update(any(), eq(2))).thenReturn(Mono.just(saved));

        StepVerifier.create(service().update(value))
                .expectNext(saved)
                .verifyComplete();
        verify(store).update(value, 2);
        verify(publisher).publishEvent(any());
    }

    @Test
    void addRequiresTenantScopedRelations() {
        DriverAttributeConfigBO value = value();
        when(attributes.getById(7L, 11L)).thenReturn(Mono.empty());
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));

        StepVerifier.create(service().add(value))
                .expectError(NotFoundException.class)
                .verify();
        verify(store, never()).insert(any());
    }

    private ReactiveDriverAttributeConfigServiceImpl service() {
        return new ReactiveDriverAttributeConfigServiceImpl(store, attributes, devices, publisher);
    }

    private DriverAttributeConfigBO value() {
        DriverAttributeConfigBO value = new DriverAttributeConfigBO();
        value.setTenantId(7L);
        value.setAttributeId(11L);
        value.setDeviceId(12L);
        value.setConfigValue("x");
        value.setEnableFlag(EnableFlagEnum.ENABLE);
        return value;
    }

    private DriverAttributeBO attribute() {
        DriverAttributeBO value = new DriverAttributeBO();
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
        return value;
    }
}

package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.ReactiveEventAttributeConfigStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeService;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveEventAttributeConfigServiceImplTest {
    @Mock ReactiveEventAttributeConfigStore store;
    @Mock ReactiveEventAttributeService attributes;
    @Mock ReactiveDeviceService devices;
    @Mock ReactiveEventService events;
    @Mock MetadataEventPublisher publisher;

    @Test
    void addRejectsDuplicateWithinTenant() {
        EventAttributeConfigBO value = value();
        validRelations();
        when(store.getByAttributeDeviceEvent(7L, 11L, 12L, 13L)).thenReturn(Mono.just(value));

        StepVerifier.create(service().add(value)).expectError(DuplicateException.class).verify();
        verify(store, never()).insert(any());
    }

    @Test
    void addRejectsEventFromDifferentProfile() {
        EventAttributeConfigBO value = value();
        validRelations();
        EventBO event = event();
        event.setProfileId(99L);
        when(events.getById(7L, 13L)).thenReturn(Mono.just(event));

        StepVerifier.create(service().add(value)).expectError(NotFoundException.class).verify();
        verify(store, never()).getByAttributeDeviceEvent(any(), any(), any(), any());
        verify(store, never()).insert(any());
    }

    @Test
    void updateUsesExpectedVersionAndPublishesMetadata() {
        EventAttributeConfigBO value = value();
        value.setId(99L);
        value.setVersion(2);
        EventAttributeConfigBO saved = value();
        saved.setId(99L);
        saved.setVersion(3);
        validRelations();
        when(store.get(7L, 99L)).thenReturn(Mono.just(value));
        when(store.getByAttributeDeviceEvent(7L, 11L, 12L, 13L)).thenReturn(Mono.just(value));
        when(store.update(any(), eq(2))).thenReturn(Mono.just(saved));

        StepVerifier.create(service().update(value)).expectNext(saved).verifyComplete();
        verify(store).update(value, 2);
        verify(publisher).publishEvent(any());
    }

    @Test
    void addFailsWhenRelationLookupIsEmpty() {
        EventAttributeConfigBO value = value();
        when(attributes.getById(7L, 11L)).thenReturn(Mono.empty());
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));
        when(events.getById(7L, 13L)).thenReturn(Mono.just(event()));

        StepVerifier.create(service().add(value)).expectError(NotFoundException.class).verify();
        verify(store, never()).insert(any());
    }

    private ReactiveEventAttributeConfigServiceImpl service() { return new ReactiveEventAttributeConfigServiceImpl(store, attributes, devices, events, publisher); }
    private void validRelations() { when(attributes.getById(7L, 11L)).thenReturn(Mono.just(attribute())); when(devices.getById(7L, 12L)).thenReturn(Mono.just(device())); when(events.getById(7L, 13L)).thenReturn(Mono.just(event())); }
    private EventAttributeConfigBO value() { EventAttributeConfigBO value = new EventAttributeConfigBO(); value.setTenantId(7L); value.setAttributeId(11L); value.setDeviceId(12L); value.setEventId(13L); value.setEnableFlag(EnableFlagEnum.ENABLE); value.setConfigValue("x"); return value; }
    private EventAttributeBO attribute() { EventAttributeBO value = new EventAttributeBO(); value.setTenantId(7L); value.setId(11L); value.setDriverId(21L); return value; }
    private DeviceBO device() { DeviceBO value = new DeviceBO(); value.setTenantId(7L); value.setId(12L); value.setDriverId(21L); value.setProfileId(31L); return value; }
    private EventBO event() { EventBO value = new EventBO(); value.setTenantId(7L); value.setId(13L); value.setProfileId(31L); return value; }
}

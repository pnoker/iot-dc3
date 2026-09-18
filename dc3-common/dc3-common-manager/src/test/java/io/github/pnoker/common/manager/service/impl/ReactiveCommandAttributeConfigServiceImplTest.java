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
import static org.mockito.Mockito.*;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.ReactiveCommandAttributeConfigStore;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeService;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveCommandAttributeConfigServiceImplTest {
    @Mock
    ReactiveCommandAttributeConfigStore store;

    @Mock
    ReactiveCommandAttributeService attributes;

    @Mock
    ReactiveDeviceService devices;

    @Mock
    ReactiveCommandService commands;

    @Mock
    MetadataEventPublisher publisher;

    @Test
    void addRejectsDuplicateWithinTenantBeforeInsert() {
        CommandAttributeConfigBO value = value();
        when(attributes.getById(7L, 11L)).thenReturn(Mono.just(attribute()));
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));
        when(commands.getById(7L, 13L)).thenReturn(Mono.just(command()));
        CommandAttributeConfigBO existing = value();
        existing.setId(99L);
        when(store.getByAttributeDeviceCommand(7L, 11L, 12L, 13L)).thenReturn(Mono.just(existing));

        StepVerifier.create(service().add(value))
                .expectError(DuplicateException.class)
                .verify();
        verify(store, never()).insert(any());
    }

    @Test
    void updateUsesExpectedVersionAndPublishesDeviceMetadata() {
        CommandAttributeConfigBO value = value();
        value.setId(99L);
        value.setVersion(2);
        CommandAttributeConfigBO saved = value();
        saved.setId(99L);
        saved.setVersion(3);
        when(attributes.getById(7L, 11L)).thenReturn(Mono.just(attribute()));
        when(devices.getById(7L, 12L)).thenReturn(Mono.just(device()));
        when(commands.getById(7L, 13L)).thenReturn(Mono.just(command()));
        when(store.get(7L, 99L)).thenReturn(Mono.just(value()));
        CommandAttributeConfigBO existing = value();
        existing.setId(99L);
        when(store.getByAttributeDeviceCommand(7L, 11L, 12L, 13L)).thenReturn(Mono.just(existing));
        when(store.update(any(), eq(2))).thenReturn(Mono.just(saved));

        StepVerifier.create(service().update(value)).expectNext(saved).verifyComplete();
        verify(store).update(value, 2);
        verify(publisher).publishEvent(any());
    }

    private ReactiveCommandAttributeConfigServiceImpl service() {
        return new ReactiveCommandAttributeConfigServiceImpl(store, attributes, devices, commands, publisher);
    }

    private CommandAttributeConfigBO value() {
        CommandAttributeConfigBO value = new CommandAttributeConfigBO();
        value.setTenantId(7L);
        value.setAttributeId(11L);
        value.setDeviceId(12L);
        value.setCommandId(13L);
        value.setConfigValue("x");
        value.setEnableFlag(EnableFlagEnum.ENABLE);
        return value;
    }

    private CommandAttributeBO attribute() {
        CommandAttributeBO value = new CommandAttributeBO();
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

    private CommandBO command() {
        CommandBO value = new CommandBO();
        value.setTenantId(7L);
        value.setId(13L);
        value.setProfileId(31L);
        return value;
    }
}

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

import static org.mockito.Mockito.*;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.EventParamFilter;
import io.github.pnoker.common.manager.repository.ReactiveEventParamStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactiveEventService;
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
class ReactiveEventParamServiceImplTest {
    @Mock
    ReactiveEventParamStore store;

    @Mock
    ReactiveEventService events;

    @Mock
    ReactiveDeviceService devices;

    @Mock
    ReactiveDriverService drivers;

    @Mock
    MetadataEventPublisher publisher;

    @Test
    void addKeepsTenantScopeAndPublishesOnlyAfterPersistence() {
        EventParamBO value = value();
        EventBO event = new EventBO();
        event.setId(10L);
        event.setTenantId(7L);
        event.setProfileId(20L);
        when(events.getById(7L, 10L)).thenReturn(Mono.just(event));
        when(store.existsByNameOrCode(7L, 10L, "temperature", "temperature", null))
                .thenReturn(Mono.just(false));
        when(store.insert(value)).thenReturn(Mono.just(value));
        when(devices.listByProfileId(7L, 20L)).thenReturn(Flux.empty());

        StepVerifier.create(service().add(value)).expectNext(value).verifyComplete();

        verify(store).existsByNameOrCode(7L, 10L, "temperature", "temperature", null);
        verify(store).insert(value);
        verify(publisher)
                .publishEvent(argThat(metadata -> metadata.getId().equals(10L)
                        && metadata.getTargetServices().isEmpty()));
    }

    @Test
    void listDelegatesImmutableOffsetFilter() {
        EventParamFilter filter = new EventParamFilter(7L, null, null, null, null, null, null, 20, 10, List.of());
        OffsetPage<EventParamBO> page = OffsetPage.of(List.of(), 20, 10, 20);
        when(store.list(filter)).thenReturn(Mono.just(page));
        StepVerifier.create(service().list(filter)).expectNext(page).verifyComplete();
    }

    @Test
    void updateRejectsBusinessCodeMutation() {
        EventParamBO current = value();
        EventParamBO requested = value();
        requested.setParamCode("changed");
        when(store.get(7L, 1L)).thenReturn(Mono.just(current));
        StepVerifier.create(service().update(requested))
                .expectErrorMessage("Event param code cannot be changed")
                .verify();
        verifyNoInteractions(events, devices, drivers, publisher);
    }

    private ReactiveEventParamServiceImpl service() {
        return new ReactiveEventParamServiceImpl(store, events, devices, drivers, publisher);
    }

    private EventParamBO value() {
        EventParamBO value = new EventParamBO();
        value.setId(1L);
        value.setTenantId(7L);
        value.setEventId(10L);
        value.setParamName("temperature");
        value.setParamCode("temperature");
        value.setParamTypeFlag(PointTypeEnum.DOUBLE);
        value.setEnableFlag(EnableFlagEnum.ENABLE);
        value.setVersion(0);
        return value;
    }
}

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
import static org.mockito.Mockito.*;

import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.DriverAttributeFilter;
import io.github.pnoker.common.manager.repository.ReactiveDriverAttributeStore;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveDriverAttributeServiceImplTest {
    @Mock
    ReactiveDriverAttributeStore store;

    @Mock
    ReactiveDriverService drivers;

    @Mock
    MetadataEventPublisher publisher;

    @Test
    void addValidatesDriverAndReturnsSavedAttribute() {
        DriverAttributeBO value = value(null);
        DriverAttributeBO saved = value(10L);
        when(drivers.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.manager.entity.bo.DriverBO()));
        when(store.getByCodeAndDriver(7L, "FUNC", 11L)).thenReturn(Mono.empty());
        when(store.insert(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(new ReactiveDriverAttributeServiceImpl(store, drivers, publisher).add(value))
                .assertNext(result -> assertThat(result.getId()).isEqualTo(10L))
                .verifyComplete();
        verify(store).insert(value);
        verify(publisher).publishEvent(any());
    }

    @Test
    void duplicateCodeFailsBeforeInsert() {
        DriverAttributeBO value = value(null);
        when(drivers.getById(7L, 11L)).thenReturn(Mono.just(new io.github.pnoker.common.manager.entity.bo.DriverBO()));
        when(store.getByCodeAndDriver(7L, "FUNC", 11L)).thenReturn(Mono.just(value(12L)));

        StepVerifier.create(new ReactiveDriverAttributeServiceImpl(store, drivers, publisher).add(value))
                .expectErrorMessage("Command attribute has been duplicated")
                .verify();
        verify(store, never()).insert(any());
    }

    @Test
    void listDelegatesCanonicalOffsetFilter() {
        var filter = new DriverAttributeFilter(7L, null, null, null, 11L, null, null, 20, 10, List.of());
        var page = OffsetPage.of(List.<DriverAttributeBO>of(), 20, 10, 20);
        when(store.list(filter)).thenReturn(Mono.just(page));
        StepVerifier.create(new ReactiveDriverAttributeServiceImpl(store, drivers, publisher).list(filter))
                .expectNext(page)
                .verifyComplete();
        verify(store).list(filter);
    }

    private DriverAttributeBO value(Long id) {
        DriverAttributeBO value = new DriverAttributeBO();
        value.setId(id);
        value.setTenantId(7L);
        value.setDriverId(11L);
        value.setAttributeName("Function");
        value.setAttributeCode("FUNC");
        value.setAttributeTypeFlag(AttributeTypeEnum.STRING);
        value.setVersion(0);
        return value;
    }
}

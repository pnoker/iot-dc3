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
package io.github.pnoker.common.facade.local;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDeviceBuilder;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DeviceLocalFacadeTest {
    @Mock
    ReactiveDeviceService service;

    @Mock
    FacadeDeviceBuilder builder;

    @Mock
    ObjectProvider<io.github.pnoker.common.manager.repository.ReactiveDriverLeaseStore> leaseStore;

    @InjectMocks
    DeviceLocalFacade facade;

    @Test
    void listReactiveMapsCanonicalOffsetPage() {
        DeviceBO source = new DeviceBO();
        FacadeDeviceBO mapped = new FacadeDeviceBO();
        when(service.list(any(DeviceFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(source), 0, 20, 1)));
        when(builder.toFacadeBO(source)).thenReturn(mapped);
        StepVerifier.create(facade.listReactive(new FacadeDeviceOffsetQuery(
                        7L, null, null, null, null, null, null, null, null, 0, 20, List.of())))
                .assertNext(page ->
                        org.assertj.core.api.Assertions.assertThat(page.items()).containsExactly(mapped))
                .verifyComplete();
    }
}

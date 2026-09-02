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
package io.github.pnoker.common.data.biz.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DeviceStatusServiceImplTest {
    @Mock
    DeviceFacade deviceFacade;

    @Mock
    ReactiveEntityStateStore stateStore;

    @Test
    void listUsesReactiveFacadeAndStateProjection() {
        DeviceStatusServiceImpl service = new DeviceStatusServiceImpl(deviceFacade, stateStore);
        FacadeDeviceBO device = device(10L);
        when(deviceFacade.listReactive(any(FacadeDeviceOffsetQuery.class)))
                .thenReturn(Mono.just(io.github.pnoker.db.r2dbc.core.page.OffsetPage.of(List.of(device), 0, 50, 1)));
        when(stateStore.listStateFlags(100L, EntityTypeEnum.DEVICE, List.of(10L)))
                .thenReturn(Mono.just(Map.of(10L, (byte) EntityStatusEnum.ONLINE.getIndex())));

        StepVerifier.create(service.list(query(100L)))
                .assertNext(result -> assertThat(result).containsEntry("10", EntityStatusEnum.ONLINE.getCode()))
                .verifyComplete();
        verify(deviceFacade).listReactive(any(FacadeDeviceOffsetQuery.class));
    }

    @Test
    void missingProjectionDefaultsOffline() {
        DeviceStatusServiceImpl service = new DeviceStatusServiceImpl(deviceFacade, stateStore);
        when(deviceFacade.listReactive(any(FacadeDeviceOffsetQuery.class)))
                .thenReturn(
                        Mono.just(io.github.pnoker.db.r2dbc.core.page.OffsetPage.of(List.of(device(10L)), 0, 50, 1)));
        when(stateStore.listStateFlags(any(), any(), any())).thenReturn(Mono.just(Map.of()));

        StepVerifier.create(service.list(query(100L)))
                .assertNext(result -> assertThat(result).containsEntry("10", EntityStatusEnum.OFFLINE.getCode()))
                .verifyComplete();
    }

    private FacadeDeviceOffsetQuery query(Long tenantId) {
        return new FacadeDeviceOffsetQuery(tenantId, null, null, null, null, null, null, null, null, 0, 50, List.of());
    }

    private FacadeDeviceBO device(Long id) {
        FacadeDeviceBO value = new FacadeDeviceBO();
        value.setId(id);
        return value;
    }
}

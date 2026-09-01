package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceStatusServiceImplTest {
    @Mock DeviceFacade deviceFacade;
    @Mock ReactiveEntityStateStore stateStore;

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
                .thenReturn(Mono.just(io.github.pnoker.db.r2dbc.core.page.OffsetPage.of(List.of(device(10L)), 0, 50, 1)));
        when(stateStore.listStateFlags(any(), any(), any())).thenReturn(Mono.just(Map.of()));

        StepVerifier.create(service.list(query(100L)))
                .assertNext(result -> assertThat(result).containsEntry("10", EntityStatusEnum.OFFLINE.getCode()))
                .verifyComplete();
    }

    private FacadeDeviceOffsetQuery query(Long tenantId) {
        return new FacadeDeviceOffsetQuery(tenantId, null, null, null, null, null, null, null, null, 0, 50, List.of());
    }

    private FacadeDeviceBO device(Long id) {
        FacadeDeviceBO value = new FacadeDeviceBO(); value.setId(id); return value;
    }
}

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.repository.ReactiveEntityStateStore;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverStatusServiceImplTest {
    @Mock DriverFacade driverFacade;
    @Mock DeviceFacade deviceFacade;
    @Mock ReactiveEntityStateStore stateStore;

    @Test
    void listUsesReactiveDriverFacade() {
        DriverStatusServiceImpl service = new DriverStatusServiceImpl(driverFacade, deviceFacade, stateStore);
        FacadeDriverBO driver = new FacadeDriverBO(); driver.setId(7L);
        when(driverFacade.listReactive(any(FacadeDriverOffsetQuery.class)))
                .thenReturn(Mono.just(io.github.pnoker.db.r2dbc.core.page.OffsetPage.of(List.of(driver), 0, 50, 1)));
        when(stateStore.listStateFlags(100L, EntityTypeEnum.DRIVER, List.of(7L)))
                .thenReturn(Mono.just(Map.of(7L, (byte) EntityStatusEnum.ONLINE.getIndex())));

        StepVerifier.create(service.list(query(100L)))
                .assertNext(result -> assertThat(result).containsEntry("7", EntityStatusEnum.ONLINE.getCode()))
                .verifyComplete();
    }

    @Test
    void countOnlineDevicesIsTenantScoped() {
        DriverStatusServiceImpl service = new DriverStatusServiceImpl(driverFacade, deviceFacade, stateStore);
        when(driverFacade.getByIdReactive(100L, 7L)).thenReturn(Mono.just(new FacadeDriverBO()));
        when(deviceFacade.listByDriverIdReactive(100L, 7L)).thenReturn(Flux.just(device(10L), device(11L)));
        when(stateStore.listStateFlags(100L, EntityTypeEnum.DEVICE, List.of(10L, 11L)))
                .thenReturn(Mono.just(Map.of(10L, (byte) EntityStatusEnum.ONLINE.getIndex(), 11L, (byte) EntityStatusEnum.OFFLINE.getIndex())));

        StepVerifier.create(service.countOnlineDevices(100L, 7L)).expectNext(1L).verifyComplete();
    }

    @Test
    void missingDriverIsNotFound() {
        DriverStatusServiceImpl service = new DriverStatusServiceImpl(driverFacade, deviceFacade, stateStore);
        when(driverFacade.getByIdReactive(100L, 7L)).thenReturn(Mono.empty());

        StepVerifier.create(service.countOnlineDevices(100L, 7L))
                .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(io.github.pnoker.common.exception.NotFoundException.class))
                .verify();
    }

    private FacadeDriverOffsetQuery query(Long tenantId) {
        return new FacadeDriverOffsetQuery(tenantId, null, null, null, null, null, null, null, null, null, 0, 50, List.of());
    }

    private FacadeDeviceBO device(Long id) {
        FacadeDeviceBO value = new FacadeDeviceBO(); value.setId(id); return value;
    }
}

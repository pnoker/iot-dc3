package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReactiveDriverRegisterServiceImplTest {

    @Test
    void registerCreatesDriverAndReconcilesEmptyAttributeDeclarations() {
        GrpcDriverBuilder driverBuilder = mock(GrpcDriverBuilder.class);
        GrpcDriverRegisterDTO request = GrpcDriverRegisterDTO.newBuilder().setTenant("tenant-a")
                .setDriver(io.github.pnoker.api.common.GrpcDriverDTO.getDefaultInstance()).build();
        DriverBO driver = new DriverBO();
        driver.setId(7L);
        driver.setServiceName("driver-service");
        ReactiveDriverService driverService = mock(ReactiveDriverService.class);
        when(driverBuilder.buildBOByGrpcDTO(request.getDriver())).thenReturn(driver);
        when(driverService.getByServiceName(100L, "driver-service")).thenReturn(Mono.empty());
        when(driverService.add(driver)).thenReturn(Mono.just(driver));

        ReactiveDriverAttributeService driverAttributes = mock(ReactiveDriverAttributeService.class);
        ReactivePointAttributeService pointAttributes = mock(ReactivePointAttributeService.class);
        ReactiveCommandAttributeService commandAttributes = mock(ReactiveCommandAttributeService.class);
        ReactiveEventAttributeService eventAttributes = mock(ReactiveEventAttributeService.class);
        when(driverAttributes.listByDriverId(100L, 7L)).thenReturn(Flux.empty());
        when(pointAttributes.listByDriverId(100L, 7L)).thenReturn(Flux.empty());
        when(commandAttributes.listByDriverId(100L, 7L)).thenReturn(Flux.empty());
        when(eventAttributes.listByDriverId(100L, 7L)).thenReturn(Flux.empty());
        when(driverAttributes.saveBatch(any())).thenReturn(Mono.just(List.of()));
        when(driverAttributes.updateBatch(any())).thenReturn(Mono.just(List.of()));
        when(driverAttributes.deleteByIds(any(), any(), any(), any())).thenReturn(Mono.just(false));
        when(pointAttributes.saveBatch(any())).thenReturn(Mono.just(List.of()));
        when(pointAttributes.updateBatch(any())).thenReturn(Mono.just(List.of()));
        when(pointAttributes.deleteByIds(any(), any(), any(), any())).thenReturn(Mono.just(false));
        when(commandAttributes.saveBatch(any())).thenReturn(Mono.just(List.of()));
        when(commandAttributes.updateBatch(any())).thenReturn(Mono.just(List.of()));
        when(commandAttributes.deleteByIds(any(), any(), any(), any())).thenReturn(Mono.just(false));
        when(eventAttributes.saveBatch(any())).thenReturn(Mono.just(List.of()));
        when(eventAttributes.updateBatch(any())).thenReturn(Mono.just(List.of()));
        when(eventAttributes.deleteByIds(any(), any(), any(), any())).thenReturn(Mono.just(false));

        TenantFacade tenantFacade = mock(TenantFacade.class);
        FacadeTenantBO tenant = new FacadeTenantBO();
        tenant.setId(100L);
        when(tenantFacade.getByCode("tenant-a")).thenReturn(Mono.just(tenant));
        TransactionalOperator transactions = mock(TransactionalOperator.class);
        when(transactions.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReactiveDriverRegisterServiceImpl service = new ReactiveDriverRegisterServiceImpl(driverBuilder,
                mock(GrpcDriverAttributeBuilder.class), mock(GrpcPointAttributeBuilder.class),
                mock(GrpcCommandAttributeBuilder.class), mock(GrpcEventAttributeBuilder.class), driverService,
                driverAttributes, pointAttributes, commandAttributes, eventAttributes, tenantFacade, transactions);

        var registration = service.register(request).block();

        assertThat(registration).isNotNull();
        assertThat(registration.driver().getTenantId()).isEqualTo(100L);
    }
}

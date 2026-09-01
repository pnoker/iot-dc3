package io.github.pnoker.common.manager.grpc.server;

import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDriverIdsQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetDriverQuery;
import io.github.pnoker.api.center.manager.GrpcOffsetPageDriverDTO;
import io.github.pnoker.api.center.manager.GrpcDriverListDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.grpc.server.manager.ManagerDriverServer;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerDriverServerTest {

    @Mock private ReactiveDriverService reactiveDriverService;
    @Mock private GrpcDriverBuilder grpcDriverBuilder;
    private Server server; private ManagedChannel channel; private DriverApiGrpc.DriverApiBlockingStub stub;

    @BeforeEach void setUp() throws Exception {
        String name = "dc3-manager-driver-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(new ManagerDriverServer(grpcDriverBuilder, reactiveDriverService)).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build(); stub = DriverApiGrpc.newBlockingStub(channel);
    }
    @AfterEach void tearDown() { if (channel != null) channel.shutdownNow(); if (server != null) server.shutdownNow(); }

    @Test void getByDriverIdReturnsMappedResource() {
        DriverBO bo = driver(1L); when(reactiveDriverService.getById(7L, 1L)).thenReturn(Mono.just(bo));
        when(grpcDriverBuilder.buildGrpcDTOByBO(bo)).thenReturn(GrpcDriverDTO.newBuilder().build());
        GrpcDriverDTO response = stub.getByDriverId(GrpcDriverQuery.newBuilder().setTenantId(7L).setDriverId(1L).build());
        assertThat(response).isNotNull();
    }

    @Test void getByDriverIdReturnsNotFoundStatus() {
        when(reactiveDriverService.getById(7L, 99L)).thenReturn(Mono.error(new io.github.pnoker.common.exception.NotFoundException("missing")));
        assertThatThrownBy(() -> stub.getByDriverId(GrpcDriverQuery.newBuilder().setTenantId(7L).setDriverId(99L).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    @Test void listByDriverIdsIsTenantScoped() {
        DriverBO bo = driver(1L); when(reactiveDriverService.listByIds(7L, List.of(1L))).thenReturn(reactor.core.publisher.Flux.just(bo));
        when(grpcDriverBuilder.buildGrpcDTOByBO(bo)).thenReturn(GrpcDriverDTO.newBuilder().build());
        GrpcDriverListDTO response = stub.listByDriverIds(GrpcDriverIdsQuery.newBuilder().setTenantId(7L).addDriverIds(1L).build());
        assertThat(response.getItemsCount()).isEqualTo(1);
    }

    @Test void listReturnsCanonicalOffsetPage() {
        when(reactiveDriverService.list(any(DriverFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(driver(1L)), 10, 5, 11)));
        when(grpcDriverBuilder.buildGrpcDTOByBO(any())).thenReturn(GrpcDriverDTO.newBuilder().build());
        GrpcOffsetPageDriverDTO response = stub.list(GrpcOffsetDriverQuery.newBuilder().setTenantId(7L)
                .setPage(PageRequest.newBuilder().setOffset(0).setLimit(5)).build());
        assertThat(response.getPage().getOffset()).isEqualTo(10L);
        assertThat(response.getPage().getLimit()).isEqualTo(5L);
        assertThat(response.getPage().getTotal()).isEqualTo(11L);
    }

    private DriverBO driver(Long id) { DriverBO value = new DriverBO(); value.setId(id); value.setTenantId(7L); return value; }
}

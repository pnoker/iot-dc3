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
package io.github.pnoker.common.manager.grpc.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pnoker.api.center.manager.DeviceApiGrpc;
import io.github.pnoker.api.center.manager.GrpcOffsetDeviceQuery;
import io.github.pnoker.common.manager.biz.DriverLeaseService;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ReactiveManagerDeviceServerTest {

    private final GrpcDeviceBuilder deviceBuilder = mock(GrpcDeviceBuilder.class);
    private final ReactiveDeviceService deviceService = mock(ReactiveDeviceService.class);
    private final DriverLeaseService leaseService = mock(DriverLeaseService.class);
    private Server server;
    private ManagedChannel channel;
    private DeviceApiGrpc.DeviceApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String name = "dc3-manager-device-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new ManagerDeviceServer(deviceBuilder, deviceService, leaseService))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = DeviceApiGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) channel.shutdownNow();
        if (server != null) server.shutdownNow();
    }

    @Test
    void listUsesOffsetPageAndReturnsItems() {
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        when(deviceService.list(any())).thenReturn(Mono.just(OffsetPage.of(java.util.List.of(device), 20, 10, 21)));
        when(deviceBuilder.buildGrpcDTOByBO(device))
                .thenReturn(io.github.pnoker.api.common.GrpcDeviceDTO.getDefaultInstance());

        var response = stub.list(GrpcOffsetDeviceQuery.newBuilder()
                .setTenantId(100L)
                .setPage(io.github.pnoker.api.common.PageRequest.newBuilder()
                        .setOffset(20)
                        .setLimit(10))
                .build());

        assertThat(response.getPage().getOffset()).isEqualTo(20);
        assertThat(response.getPage().getLimit()).isEqualTo(10);
        assertThat(response.getPage().getTotal()).isEqualTo(21);
        assertThat(response.getItemsCount()).isEqualTo(1);
    }

    @Test
    void getByDeviceIdReturnsNotFoundStatus() {
        when(deviceService.getById(100L, 404L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> stub.getByDeviceId(io.github.pnoker.api.center.manager.GrpcDeviceQuery.newBuilder()
                        .setTenantId(100L)
                        .setDeviceId(404L)
                        .build()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }
}

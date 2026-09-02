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
package io.github.pnoker.common.manager.grpc.server.driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.driver.GrpcOffsetPagePointDTO;
import io.github.pnoker.api.common.driver.GrpcOffsetPointQuery;
import io.github.pnoker.api.common.driver.GrpcPointQuery;
import io.github.pnoker.api.common.driver.PointApiGrpc;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointBuilder;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactivePointService;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ReactiveDriverPointServerTest {

    private final GrpcPointBuilder pointBuilder = mock(GrpcPointBuilder.class);
    private final ReactivePointService pointService = mock(ReactivePointService.class);
    private final ReactiveDriverService driverService = mock(ReactiveDriverService.class);
    private final ReactiveDeviceService deviceService = mock(ReactiveDeviceService.class);
    private Server server;
    private ManagedChannel channel;
    private PointApiGrpc.PointApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        String name = "dc3-driver-point-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new DriverPointServer(pointBuilder, pointService, driverService, deviceService))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = PointApiGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) channel.shutdownNow();
        if (server != null) server.shutdownNow();
    }

    @Test
    void listScopesPointsToDriverTenantAndUsesOffsetBounds() {
        DriverBO driver = new DriverBO();
        driver.setId(7L);
        driver.setTenantId(100L);
        DeviceBO device = new DeviceBO();
        device.setId(8L);
        device.setDriverId(7L);
        device.setTenantId(100L);
        device.setProfileId(9L);
        PointBO first = point(2L, 100L, 9L);
        PointBO second = point(1L, 100L, 9L);
        PointBO foreign = point(3L, 200L, 9L);
        when(driverService.getById(100L, 7L)).thenReturn(Mono.just(driver));
        when(deviceService.listByDriverId(100L, 7L)).thenReturn(Flux.just(device));
        when(pointService.listByProfileId(100L, 9L)).thenReturn(Flux.just(first, second, foreign));
        when(pointBuilder.buildGrpcDTOByBO(any(PointBO.class)))
                .thenAnswer(invocation -> GrpcPointDTO.newBuilder()
                        .setBase(io.github.pnoker.api.common.GrpcBase.newBuilder()
                                .setId(invocation.<PointBO>getArgument(0).getId())
                                .build())
                        .build());

        GrpcOffsetPagePointDTO response = stub.list(GrpcOffsetPointQuery.newBuilder()
                .setTenantId(100L)
                .setDriverId(7L)
                .setPage(PageRequest.newBuilder().setOffset(0).setLimit(1))
                .build());

        assertThat(response.getPage().getTotal()).isEqualTo(2);
        assertThat(response.getItems(0).getBase().getId()).isEqualTo(1L);
    }

    @Test
    void getByIdRejectsPointFromAnotherTenant() {
        DriverBO driver = new DriverBO();
        driver.setId(7L);
        driver.setTenantId(100L);
        PointBO point = point(1L, 200L, 9L);
        when(driverService.getById(100L, 7L)).thenReturn(Mono.just(driver));
        when(pointService.getById(100L, 1L)).thenReturn(Mono.just(point));

        assertThatThrownBy(() -> stub.getById(GrpcPointQuery.newBuilder()
                        .setTenantId(100L)
                        .setDriverId(7L)
                        .setPointId(1L)
                        .build()))
                .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        error -> assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND));
    }

    private PointBO point(Long id, Long tenantId, Long profileId) {
        PointBO point = new PointBO();
        point.setId(id);
        point.setTenantId(tenantId);
        point.setProfileId(profileId);
        return point;
    }
}

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

package io.github.pnoker.common.manager.grpc.server;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDriverIdsQuery;
import io.github.pnoker.api.center.manager.GrpcRDriverDTO;
import io.github.pnoker.api.center.manager.GrpcRDriverListDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.grpc.server.manager.ManagerDriverServer;
import io.github.pnoker.common.manager.service.DriverService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerDriverServerTest {

    @Mock
    private DriverService driverService;

    @Mock
    private GrpcDriverBuilder grpcDriverBuilder;

    private Server server;
    private ManagedChannel channel;
    private DriverApiGrpc.DriverApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        ManagerDriverServer driverServer = new ManagerDriverServer(grpcDriverBuilder, driverService);

        String name = "dc3-manager-driver-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(driverServer).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = DriverApiGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void listByDriverIdReturnsOkEnvelopeWithMappedData() {
        DriverBO bo = new DriverBO();
        bo.setId(1L);
        GrpcDriverDTO dto = GrpcDriverDTO.newBuilder().build();
        when(driverService.getById(1L)).thenReturn(bo);
        when(grpcDriverBuilder.buildGrpcDTOByBO(bo)).thenReturn(dto);

        GrpcRDriverDTO response = stub.getByDriverId(GrpcDriverQuery.newBuilder().setDriverId(1L).build());
        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getResult().getCode()).isEqualTo(SuccessCode.OK.getCode());
    }

    @Test
    void listByDriverIdReturnsNoResourceWhenMissing() {
        when(driverService.getById(99L)).thenReturn(null);
        GrpcRDriverDTO response = stub.getByDriverId(GrpcDriverQuery.newBuilder().setDriverId(99L).build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void listByDriverIdsReturnsOkForNonEmptyList() {
        DriverBO bo = new DriverBO();
        bo.setId(1L);
        when(driverService.listByIds(Set.of(1L))).thenReturn(List.of(bo));
        when(grpcDriverBuilder.buildGrpcDTOByBO(bo)).thenReturn(GrpcDriverDTO.newBuilder().build());

        GrpcRDriverListDTO response = stub.listByDriverIds(
                GrpcDriverIdsQuery.newBuilder().addDriverIds(1L).build());
        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getDataCount()).isEqualTo(1);
    }

    @Test
    void listByDriverIdsReturnsNoResourceForEmptyList() {
        when(driverService.listByIds(eq(Set.of(99L)))).thenReturn(List.of());
        GrpcRDriverListDTO response = stub.listByDriverIds(
                GrpcDriverIdsQuery.newBuilder().addDriverIds(99L).build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void listByDriverIdsReturnsNoResourceForNullSelection() {
        when(driverService.listByIds(Set.of(99L))).thenReturn(null);
        GrpcRDriverListDTO response = stub.listByDriverIds(
                GrpcDriverIdsQuery.newBuilder().addDriverIds(99L).build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void listByPageReturnsNoResourceWhenServiceReturnsNull() {
        when(driverService.list(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(grpcDriverBuilder.buildQueryByGrpcQuery(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        var response = stub.listByPage(io.github.pnoker.api.center.manager.GrpcPageDriverQuery.newBuilder().build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void listByPageReturnsOkWithPageMetadata() {
        Page<DriverBO> page = new Page<>(2, 10);
        page.setTotal(25);
        page.setRecords(List.of(new DriverBO()));
        when(driverService.list(org.mockito.ArgumentMatchers.any())).thenReturn(page);
        when(grpcDriverBuilder.buildQueryByGrpcQuery(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(grpcDriverBuilder.buildGrpcDTOByBO(org.mockito.ArgumentMatchers.any()))
                .thenReturn(GrpcDriverDTO.newBuilder().build());

        var response = stub.listByPage(io.github.pnoker.api.center.manager.GrpcPageDriverQuery.newBuilder().build());
        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getData().getPage().getCurrent()).isEqualTo(2L);
        assertThat(response.getData().getPage().getSize()).isEqualTo(10L);
        assertThat(response.getData().getPage().getTotal()).isEqualTo(25L);
    }
}

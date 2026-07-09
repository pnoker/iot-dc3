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

import io.github.pnoker.api.common.GrpcCommandAttributeDTO;
import io.github.pnoker.api.common.GrpcDriverAttributeDTO;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.api.common.GrpcEventAttributeDTO;
import io.github.pnoker.api.common.GrpcPointAttributeDTO;
import io.github.pnoker.api.common.driver.DriverApiGrpc;
import io.github.pnoker.api.common.driver.GrpcRDriverRegisterDTO;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.manager.biz.DriverRegisterService;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.manager.service.CommandAttributeService;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.EventAttributeService;
import io.github.pnoker.common.manager.service.PointAttributeService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverDriverServerTest {

    @Mock
    private GrpcDriverBuilder grpcDriverBuilder;

    @Mock
    private GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;

    @Mock
    private GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    @Mock
    private GrpcCommandAttributeBuilder grpcCommandAttributeBuilder;

    @Mock
    private GrpcEventAttributeBuilder grpcEventAttributeBuilder;

    @Mock
    private DriverRegisterService driverRegisterService;

    @Mock
    private DriverService driverService;

    @Mock
    private DriverAttributeService driverAttributeService;

    @Mock
    private PointAttributeService pointAttributeService;

    @Mock
    private CommandAttributeService commandAttributeService;

    @Mock
    private EventAttributeService eventAttributeService;

    @Mock
    private DeviceService deviceService;

    private Server server;
    private ManagedChannel channel;
    private DriverApiGrpc.DriverApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        DriverDriverServer driverServer = new DriverDriverServer(grpcDriverBuilder, grpcDriverAttributeBuilder,
                grpcPointAttributeBuilder, grpcCommandAttributeBuilder, grpcEventAttributeBuilder,
                driverRegisterService, driverService, driverAttributeService, pointAttributeService,
                commandAttributeService, eventAttributeService, deviceService);

        String name = "dc3-driver-metadata-" + UUID.randomUUID();
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
    void getByIdReturnsRegisteredMetadataSnapshot() {
        DriverBO driver = new DriverBO();
        driver.setId(7L);
        driver.setTenantId(100L);

        DriverAttributeBO driverAttribute = new DriverAttributeBO();
        driverAttribute.setTenantId(100L);
        PointAttributeBO pointAttribute = new PointAttributeBO();
        pointAttribute.setTenantId(100L);
        CommandAttributeBO commandAttribute = new CommandAttributeBO();
        commandAttribute.setTenantId(100L);
        EventAttributeBO eventAttribute = new EventAttributeBO();
        eventAttribute.setTenantId(100L);

        when(driverService.getById(7L)).thenReturn(driver);
        when(grpcDriverBuilder.buildGrpcDTOByBO(driver)).thenReturn(GrpcDriverDTO.newBuilder().build());
        when(driverAttributeService.listByDriverId(7L)).thenReturn(List.of(driverAttribute));
        when(pointAttributeService.listByDriverId(7L)).thenReturn(List.of(pointAttribute));
        when(commandAttributeService.listByDriverId(7L)).thenReturn(List.of(commandAttribute));
        when(eventAttributeService.listByDriverId(7L)).thenReturn(List.of(eventAttribute));
        when(grpcDriverAttributeBuilder.buildGrpcDTOByBO(driverAttribute))
                .thenReturn(GrpcDriverAttributeDTO.newBuilder().build());
        when(grpcPointAttributeBuilder.buildGrpcDTOByBO(pointAttribute))
                .thenReturn(GrpcPointAttributeDTO.newBuilder().build());
        when(grpcCommandAttributeBuilder.buildGrpcDTOByBO(commandAttribute))
                .thenReturn(GrpcCommandAttributeDTO.newBuilder().build());
        when(grpcEventAttributeBuilder.buildGrpcDTOByBO(eventAttribute))
                .thenReturn(GrpcEventAttributeDTO.newBuilder().build());
        when(deviceService.listIdsByDriverId(7L, 100L)).thenReturn(List.of(1L, 2L));

        GrpcRDriverRegisterDTO response = stub.getById(GrpcDriverQuery.newBuilder().setDriverId(7L).build());

        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getResult().getCode()).isEqualTo(SuccessCode.OK.getCode());
        assertThat(response.getDriverAttributesCount()).isEqualTo(1);
        assertThat(response.getPointAttributesCount()).isEqualTo(1);
        assertThat(response.getCommandAttributesCount()).isEqualTo(1);
        assertThat(response.getEventAttributesCount()).isEqualTo(1);
        assertThat(response.getDeviceIdsList()).containsExactly(1L, 2L);
    }

    @Test
    void getByIdReturnsNoResourceWhenDriverMissing() {
        when(driverService.getById(404L)).thenReturn(null);

        GrpcRDriverRegisterDTO response = stub.getById(GrpcDriverQuery.newBuilder().setDriverId(404L).build());

        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    void getByIdReturnsFailureWhenLookupThrows() {
        when(driverService.getById(7L)).thenThrow(new IllegalStateException("metadata unavailable"));

        GrpcRDriverRegisterDTO response = stub.getById(GrpcDriverQuery.newBuilder().setDriverId(7L).build());

        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ErrorCode.FAILURE.getCode());
        assertThat(response.getResult().getMessage()).isEqualTo("metadata unavailable");
    }
}

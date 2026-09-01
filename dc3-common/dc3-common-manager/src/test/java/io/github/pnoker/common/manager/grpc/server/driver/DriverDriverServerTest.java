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
import io.github.pnoker.api.common.driver.GrpcDriverLeaseRequest;
import io.github.pnoker.api.common.driver.GrpcDriverLeaseDTO;
import io.github.pnoker.api.common.driver.GrpcDriverRegistrationDTO;
import io.github.pnoker.common.manager.biz.DriverLeaseService;
import io.github.pnoker.common.manager.biz.ReactiveDriverRegisterService;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DeviceLeaseBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.DriverLeaseGrantBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ReactiveDriverRegisterService driverRegisterService;

    @Mock
    private ReactiveDriverService driverService;

    @Mock
    private ReactiveDriverAttributeService driverAttributeService;

    @Mock
    private ReactivePointAttributeService pointAttributeService;

    @Mock
    private ReactiveCommandAttributeService commandAttributeService;

    @Mock
    private ReactiveEventAttributeService eventAttributeService;

    @Mock
    private DriverLeaseService driverLeaseService;

    private Server server;
    private ManagedChannel channel;
    private DriverApiGrpc.DriverApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        DriverDriverServer driverServer = new DriverDriverServer(grpcDriverBuilder, grpcDriverAttributeBuilder,
                grpcPointAttributeBuilder, grpcCommandAttributeBuilder, grpcEventAttributeBuilder,
                driverRegisterService, driverService, driverAttributeService, pointAttributeService,
                commandAttributeService, eventAttributeService, driverLeaseService);

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

        when(driverService.getById(100L, 7L)).thenReturn(Mono.just(driver));
        when(grpcDriverBuilder.buildGrpcDTOByBO(driver)).thenReturn(GrpcDriverDTO.newBuilder().build());
        when(driverAttributeService.listByDriverId(100L, 7L)).thenReturn(Flux.just(driverAttribute));
        when(pointAttributeService.listByDriverId(100L, 7L)).thenReturn(Flux.just(pointAttribute));
        when(commandAttributeService.listByDriverId(100L, 7L)).thenReturn(Flux.just(commandAttribute));
        when(eventAttributeService.listByDriverId(100L, 7L)).thenReturn(Flux.just(eventAttribute));
        when(grpcDriverAttributeBuilder.buildGrpcDTOByBO(driverAttribute))
                .thenReturn(GrpcDriverAttributeDTO.newBuilder().build());
        when(grpcPointAttributeBuilder.buildGrpcDTOByBO(pointAttribute))
                .thenReturn(GrpcPointAttributeDTO.newBuilder().build());
        when(grpcCommandAttributeBuilder.buildGrpcDTOByBO(commandAttribute))
                .thenReturn(GrpcCommandAttributeDTO.newBuilder().build());
        when(grpcEventAttributeBuilder.buildGrpcDTOByBO(eventAttribute))
                .thenReturn(GrpcEventAttributeDTO.newBuilder().build());

        GrpcDriverRegistrationDTO response = stub.getById(GrpcDriverQuery.newBuilder().setTenantId(100L).setDriverId(7L).build());

        assertThat(response.getDriverAttributesCount()).isEqualTo(1);
        assertThat(response.getPointAttributesCount()).isEqualTo(1);
        assertThat(response.getCommandAttributesCount()).isEqualTo(1);
        assertThat(response.getEventAttributesCount()).isEqualTo(1);
    }

    @Test
    void renewLeaseStreamsBoundedAssignmentPages() {
        GrpcDriverLeaseRequest request = GrpcDriverLeaseRequest.newBuilder()
                .setTenantId(100L).setDriverId(7L).setNode("node-a")
                .setClient("client-a").setHost("host-a").setLeaseSeconds(30).build();
        when(driverLeaseService.renew(100L, 7L, "node-a", "client-a", "host-a", 30, 0))
                .thenReturn(Mono.just(new DriverLeaseGrantBO(123_456L, 9L, true)));
        List<DeviceLeaseBO> first = java.util.stream.LongStream.rangeClosed(1, 1001)
                .mapToObj(id -> new DeviceLeaseBO(7L, id, "node-a", id + 1000)).toList();
        when(driverLeaseService.getAssignmentVersion(100L, 7L)).thenReturn(Mono.just(9L));
        when(driverLeaseService.listOwnedLeases(100L, 7L, "node-a", 0L, 1001)).thenReturn(Flux.fromIterable(first));
        when(driverLeaseService.listOwnedLeases(100L, 7L, "node-a", 1000L, 1001))
                .thenReturn(Flux.just(first.getLast()));

        Iterator<GrpcDriverLeaseDTO> responses = stub.renewLease(request);
        GrpcDriverLeaseDTO pageOne = responses.next();
        GrpcDriverLeaseDTO pageTwo = responses.next();

        assertThat(pageOne.getDeviceLeasesCount()).isEqualTo(1000);
        assertThat(pageOne.getSnapshotComplete()).isFalse();
        assertThat(pageTwo.getDeviceLeasesCount()).isEqualTo(1);
        assertThat(pageTwo.getSnapshotComplete()).isTrue();
        assertThat(responses.hasNext()).isFalse();
    }

    @Test
    void getByIdReturnsNoResourceWhenDriverMissing() {
        when(driverService.getById(100L, 404L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> stub.getById(GrpcDriverQuery.newBuilder().setTenantId(100L).setDriverId(404L).build()))
                .hasMessageContaining("NOT_FOUND");
    }

    @Test
    void getByIdReturnsFailureWhenLookupThrows() {
        when(driverService.getById(100L, 7L)).thenReturn(Mono.error(new IllegalStateException("metadata unavailable")));

        assertThatThrownBy(() -> stub.getById(GrpcDriverQuery.newBuilder().setTenantId(100L).setDriverId(7L).build()))
                .hasMessageContaining("metadata unavailable");
    }
}

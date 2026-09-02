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
package io.github.pnoker.common.data.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.api.center.data.GrpcPointCommandAccepted;
import io.github.pnoker.api.center.data.GrpcPointValueCommandQuery;
import io.github.pnoker.api.center.data.GrpcPointValueDTO;
import io.github.pnoker.api.center.data.GrpcPointValueHistoryQuery;
import io.github.pnoker.api.center.data.GrpcPointValueQuery;
import io.github.pnoker.api.center.data.GrpcPointValueWriteCommand;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.bo.PointCommandReadBO;
import io.github.pnoker.common.data.entity.bo.PointCommandWriteBO;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PointValueServerTest {

    @Mock
    private PointValueService pointValueService;

    @Mock
    private PointCommandService pointCommandService;

    private Server server;
    private ManagedChannel channel;
    private PointValueApiGrpc.PointValueApiBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        PointValueServer pointValueServer = new PointValueServer(pointValueService, pointCommandService);
        String name = "dc3-data-pointvalue-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(pointValueServer)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = PointValueApiGrpc.newBlockingStub(channel);
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
    void lastValueReturnsDataForKnownPoint() {
        OffsetPage<PointValueBO> page = OffsetPage.of(
                List.of(PointValueBO.builder()
                        .deviceId(10L)
                        .pointId(20L)
                        .rawValue("42")
                        .calValue("42.0")
                        .build()),
                0,
                1,
                1);
        when(pointValueService.latest(any())).thenReturn(Mono.just(page));

        GrpcPointValueDTO response = stub.getLastValue(GrpcPointValueQuery.newBuilder()
                .setTenantId(1L)
                .setDeviceId(10L)
                .setPointId(20L)
                .build());
        assertThat(response.getValue()).isEqualTo("42.0");
        assertThat(response.getRawValue()).isEqualTo("42");
    }

    @Test
    void lastValueReturnsNotFoundStatusForEmptyPage() {
        OffsetPage<PointValueBO> empty = OffsetPage.of(List.of(), 0, 1, 0);
        when(pointValueService.latest(any())).thenReturn(Mono.just(empty));

        assertThatThrownBy(() -> stub.getLastValue(GrpcPointValueQuery.newBuilder()
                        .setTenantId(1L)
                        .setDeviceId(10L)
                        .setPointId(20L)
                        .build()))
                .hasMessageContaining("NOT_FOUND");
    }

    @Test
    void lastValueReturnsGrpcStatusOnException() {
        when(pointValueService.latest(any())).thenReturn(Mono.error(new NotFoundException("Device does not exist")));

        assertThatThrownBy(() -> stub.getLastValue(GrpcPointValueQuery.newBuilder()
                        .setTenantId(1L)
                        .setDeviceId(99L)
                        .setPointId(20L)
                        .build()))
                .hasMessageContaining("NOT_FOUND");
    }

    @Test
    void historyValueReturnsListFromService() {
        when(pointValueService.history(eq(1L), eq(10L), eq(20L), eq(""), eq(50)))
                .thenReturn(Mono.just(CursorPage.of(
                        List.of(
                                PointValueBO.builder().calValue("v1").build(),
                                PointValueBO.builder().calValue("v2").build(),
                                PointValueBO.builder().calValue("v3").build()),
                        null)));

        io.github.pnoker.api.center.data.GrpcPointValueCursorPage response =
                stub.listHistoryValues(GrpcPointValueHistoryQuery.newBuilder()
                        .setTenantId(1L)
                        .setDeviceId(10L)
                        .setPointId(20L)
                        .setLimit(50)
                        .build());
        assertThat(response.getDataList()).map(GrpcPointValueDTO::getValue).containsExactly("v1", "v2", "v3");
    }

    @Test
    void historyValueReturnsGrpcStatusOnException() {
        when(pointValueService.history(any(), any(), any(), any(), eq(50)))
                .thenReturn(Mono.error(new NotFoundException("Point does not exist")));

        assertThatThrownBy(() -> stub.listHistoryValues(GrpcPointValueHistoryQuery.newBuilder()
                        .setTenantId(1L)
                        .setDeviceId(10L)
                        .setPointId(99L)
                        .setLimit(50)
                        .build()))
                .hasMessageContaining("NOT_FOUND");
    }

    @Test
    void readCommandDispatchesToCommandService() {
        when(pointCommandService.read(any(), any())).thenReturn(reactor.core.publisher.Mono.just("cmd-1"));
        GrpcPointCommandAccepted response = stub.readCommand(GrpcPointValueCommandQuery.newBuilder()
                .setTenantId(1L)
                .setDeviceId(10L)
                .setPointId(20L)
                .build());
        assertThat(response.getCommandId()).isEqualTo("cmd-1");
        verify(pointCommandService).read(eq(1L), any(PointCommandReadBO.class));
    }

    @Test
    void readCommandReturnsFailureOnAuthorizationError() {
        when(pointCommandService.read(any(), any()))
                .thenReturn(reactor.core.publisher.Mono.error(
                        new io.github.pnoker.common.exception.UnAuthorizedException("nope")));
        org.junit.jupiter.api.Assertions.assertThrows(
                io.grpc.StatusRuntimeException.class,
                () -> stub.readCommand(GrpcPointValueCommandQuery.newBuilder()
                        .setTenantId(1L)
                        .setDeviceId(10L)
                        .setPointId(20L)
                        .build()));
    }

    @Test
    void writeCommandPropagatesValuePayload() {
        when(pointCommandService.write(any(), any())).thenReturn(reactor.core.publisher.Mono.just("cmd-2"));
        GrpcPointCommandAccepted response = stub.writeCommand(GrpcPointValueWriteCommand.newBuilder()
                .setTenantId(1L)
                .setDeviceId(10L)
                .setPointId(20L)
                .setValue("99")
                .build());
        assertThat(response.getCommandId()).isEqualTo("cmd-2");
        verify(pointCommandService).write(eq(1L), any(PointCommandWriteBO.class));
    }

    @Test
    void writeCommandReturnsFailureOnException() {
        when(pointCommandService.write(any(), any()))
                .thenReturn(reactor.core.publisher.Mono.error(new NotFoundException("Device does not exist")));
        org.junit.jupiter.api.Assertions.assertThrows(
                io.grpc.StatusRuntimeException.class,
                () -> stub.writeCommand(GrpcPointValueWriteCommand.newBuilder()
                        .setTenantId(1L)
                        .setDeviceId(99L)
                        .setPointId(20L)
                        .setValue("v")
                        .build()));
    }
}

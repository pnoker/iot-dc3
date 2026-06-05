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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.data.GrpcPointValueCommandQuery;
import io.github.pnoker.api.center.data.GrpcPointValueHistoryQuery;
import io.github.pnoker.api.center.data.GrpcPointValueQuery;
import io.github.pnoker.api.center.data.GrpcPointValueWriteCommand;
import io.github.pnoker.api.center.data.GrpcRBoolean;
import io.github.pnoker.api.center.data.GrpcRPointValueDTO;
import io.github.pnoker.api.center.data.GrpcRPointValueStringList;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        server = InProcessServerBuilder.forName(name).directExecutor().addService(pointValueServer).build().start();
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
    void lastValueReturnsOkEnvelopeWithDataForKnownPoint() {
        Page<PointValueBO> page = new Page<>();
        page.setRecords(List.of(PointValueBO.builder()
                .deviceId(10L)
                .pointId(20L)
                .rawValue("42")
                .calValue("42.0")
                .build()));
        when(pointValueService.latest(any())).thenReturn(page);

        GrpcRPointValueDTO response = stub.getLastValue(GrpcPointValueQuery.newBuilder()
                .setTenantId(1L)
                .setDeviceId(10L)
                .setPointId(20L)
                .build());
        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getData().getValue()).isEqualTo("42.0");
        assertThat(response.getData().getRawValue()).isEqualTo("42");
    }

    @Test
    void lastValueReturnsNoResourceForEmptyPage() {
        Page<PointValueBO> empty = new Page<>();
        empty.setRecords(List.of());
        when(pointValueService.latest(any())).thenReturn(empty);

        GrpcRPointValueDTO response = stub.getLastValue(GrpcPointValueQuery.newBuilder()
                .setTenantId(1L).setDeviceId(10L).setPointId(20L).build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ResponseEnum.NO_RESOURCE.getCode());
    }

    @Test
    void lastValueReturnsFailureEnvelopeOnException() {
        when(pointValueService.latest(any())).thenThrow(new NotFoundException("Device does not exist"));

        GrpcRPointValueDTO response = stub.getLastValue(GrpcPointValueQuery.newBuilder()
                .setTenantId(1L).setDeviceId(99L).setPointId(20L).build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ResponseEnum.FAILURE.getCode());
        assertThat(response.getResult().getMessage()).contains("Device");
    }

    @Test
    void historyValueReturnsListFromService() {
        when(pointValueService.history(eq(1L), eq(10L), eq(20L), eq(50)))
                .thenReturn(List.of("v1", "v2", "v3"));

        GrpcRPointValueStringList response = stub.listHistoryValues(GrpcPointValueHistoryQuery.newBuilder()
                .setTenantId(1L).setDeviceId(10L).setPointId(20L).setCount(50)
                .build());
        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getDataList()).containsExactly("v1", "v2", "v3");
    }

    @Test
    void historyValueReturnsFailureEnvelopeOnException() {
        when(pointValueService.history(any(), any(), any(), eq(50)))
                .thenThrow(new NotFoundException("Point does not exist"));

        GrpcRPointValueStringList response = stub.listHistoryValues(GrpcPointValueHistoryQuery.newBuilder()
                .setTenantId(1L).setDeviceId(10L).setPointId(99L).setCount(50)
                .build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getResult().getCode()).isEqualTo(ResponseEnum.FAILURE.getCode());
    }

    @Test
    void readCommandDispatchesToCommandService() {
        GrpcRBoolean response = stub.readCommand(GrpcPointValueCommandQuery.newBuilder()
                .setTenantId(1L).setDeviceId(10L).setPointId(20L).build());
        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getData()).isTrue();
        verify(pointCommandService).read(eq(1L), any(PointCommandReadVO.class));
    }

    @Test
    void readCommandReturnsFailureOnAuthorizationError() {
        doThrow(new io.github.pnoker.common.exception.UnAuthorizedException("nope"))
                .when(pointCommandService).read(any(), any());
        GrpcRBoolean response = stub.readCommand(GrpcPointValueCommandQuery.newBuilder()
                .setTenantId(1L).setDeviceId(10L).setPointId(20L).build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getData()).isFalse();
    }

    @Test
    void writeCommandPropagatesValuePayload() {
        GrpcRBoolean response = stub.writeCommand(GrpcPointValueWriteCommand.newBuilder()
                .setTenantId(1L).setDeviceId(10L).setPointId(20L).setValue("99")
                .build());
        assertThat(response.getResult().getOk()).isTrue();
        assertThat(response.getData()).isTrue();
        verify(pointCommandService).write(eq(1L), any(PointCommandWriteVO.class));
    }

    @Test
    void writeCommandReturnsFailureOnException() {
        doThrow(new NotFoundException("Device does not exist"))
                .when(pointCommandService).write(any(), any());
        GrpcRBoolean response = stub.writeCommand(GrpcPointValueWriteCommand.newBuilder()
                .setTenantId(1L).setDeviceId(99L).setPointId(20L).setValue("v")
                .build());
        assertThat(response.getResult().getOk()).isFalse();
        assertThat(response.getData()).isFalse();
    }
}

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
package io.github.pnoker.common.driver.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pnoker.api.common.GrpcCommandRuntimeDTO;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcEventRuntimeDTO;
import io.github.pnoker.api.common.OffsetPage;
import io.github.pnoker.api.common.driver.DeviceApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDeviceAttachDTO;
import io.github.pnoker.api.common.driver.GrpcOffsetPageDeviceDTO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.builder.DeviceBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcCommandAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcEventAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.entity.ext.CommandExt;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class DeviceClientTest {

    @Test
    void listUsesOffsetPaginationAndEmitsAttachedDevices() {
        DeviceApiGrpc.DeviceApiStub stub = mock(DeviceApiGrpc.DeviceApiStub.class);
        DeviceBuilder deviceBuilder = mock(DeviceBuilder.class);
        DeviceBO device = new DeviceBO();
        when(deviceBuilder.buildDTOByGrpcDTO(org.mockito.ArgumentMatchers.any()))
                .thenReturn(device);
        DriverMetadata metadata = new DriverMetadata();
        io.github.pnoker.common.driver.entity.bo.DriverBO driver =
                new io.github.pnoker.common.driver.entity.bo.DriverBO();
        driver.setId(2L);
        driver.setTenantId(1L);
        metadata.setDriver(driver);
        DeviceClient client = new DeviceClient(
                stub,
                metadata,
                deviceBuilder,
                mock(GrpcDriverAttributeConfigBuilder.class),
                mock(GrpcPointAttributeConfigBuilder.class),
                mock(GrpcCommandAttributeConfigBuilder.class),
                mock(GrpcEventAttributeConfigBuilder.class));
        GrpcOffsetPageDeviceDTO response = GrpcOffsetPageDeviceDTO.newBuilder()
                .setPage(OffsetPage.newBuilder()
                        .setOffset(0)
                        .setLimit(200)
                        .setTotal(1)
                        .setHasNext(false))
                .addItems(GrpcDeviceAttachDTO.newBuilder()
                        .setDevice(GrpcDeviceDTO.getDefaultInstance())
                        .build())
                .build();
        doAnswer(invocation -> {
                    StreamObserver<GrpcOffsetPageDeviceDTO> observer = invocation.getArgument(1);
                    observer.onNext(response);
                    observer.onCompleted();
                    return null;
                })
                .when(stub)
                .list(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        StepVerifier.create(client.list()).expectNext(device).verifyComplete();
    }

    @Test
    void mapsAttachedCommandAndEventRuntimeMetadata() {
        DeviceBuilder deviceBuilder = mock(DeviceBuilder.class);
        DeviceBO device = new DeviceBO();
        when(deviceBuilder.buildDTOByGrpcDTO(org.mockito.ArgumentMatchers.any()))
                .thenReturn(device);
        DeviceClient client = new DeviceClient(
                null,
                new DriverMetadata(),
                deviceBuilder,
                mock(GrpcDriverAttributeConfigBuilder.class),
                mock(GrpcPointAttributeConfigBuilder.class),
                mock(GrpcCommandAttributeConfigBuilder.class),
                mock(GrpcEventAttributeConfigBuilder.class));
        GrpcEventRuntimeDTO event = GrpcEventRuntimeDTO.newBuilder()
                .setEventId("20")
                .setEventName("Alarm")
                .setEventCode("alarm")
                .setEventTypeFlag(EventTypeFlagEnum.ALERT.getIndex())
                .setEventLevelFlag(EventLevelEnum.HIGH.getIndex())
                .setEnableFlag(EnableFlagEnum.ENABLE.getIndex())
                .setVersion(3)
                .build();
        GrpcCommandRuntimeDTO command = GrpcCommandRuntimeDTO.newBuilder()
                .setCommandId("30")
                .setCommandName("Setpoint")
                .setCommandCode("setpoint")
                .setCommandTypeFlag(CommandTypeEnum.CUSTOM.getIndex())
                .setCallTypeFlag(CallTypeEnum.ASYNC.getIndex())
                .setTimeout(15)
                .setCommandExt("{\"content\":{\"keep\":\"reserved\"}}")
                .setEnableFlag(EnableFlagEnum.ENABLE.getIndex())
                .setVersion(4)
                .build();
        GrpcDeviceAttachDTO attach = GrpcDeviceAttachDTO.newBuilder()
                .setDevice(GrpcDeviceDTO.getDefaultInstance())
                .addCommands(command)
                .addEvents(event)
                .build();

        DeviceBO mapped = client.buildDTOByGrpcAttachDTO(attach);

        assertThat(mapped.getCommandRuntimeIdMap()).containsOnlyKeys(30L);
        assertThat(mapped.getCommandRuntimeIdMap().get(30L)).satisfies(runtime -> {
            assertThat(runtime.commandName()).isEqualTo("Setpoint");
            assertThat(runtime.commandCode()).isEqualTo("setpoint");
            assertThat(runtime.commandTypeFlag()).isEqualTo(CommandTypeEnum.CUSTOM);
            assertThat(runtime.callTypeFlag()).isEqualTo(CallTypeEnum.ASYNC);
            assertThat(runtime.timeout()).isEqualTo(15);
            assertThat(runtime.commandExt())
                    .extracting(CommandExt::getContent)
                    .extracting(CommandExt.Content::getKeep)
                    .isEqualTo("reserved");
            assertThat(runtime.enableFlag()).isEqualTo(EnableFlagEnum.ENABLE);
            assertThat(runtime.version()).isEqualTo(4);
        });
        assertThat(mapped.getEventRuntimeIdMap()).containsOnlyKeys(20L);
        assertThat(mapped.getEventRuntimeIdMap().get(20L)).satisfies(runtime -> {
            assertThat(runtime.eventCode()).isEqualTo("alarm");
            assertThat(runtime.eventTypeFlag()).isEqualTo(EventTypeFlagEnum.ALERT);
            assertThat(runtime.eventLevelFlag()).isEqualTo(EventLevelEnum.HIGH);
            assertThat(runtime.enableFlag()).isEqualTo(EnableFlagEnum.ENABLE);
            assertThat(runtime.version()).isEqualTo(3);
        });
    }
}

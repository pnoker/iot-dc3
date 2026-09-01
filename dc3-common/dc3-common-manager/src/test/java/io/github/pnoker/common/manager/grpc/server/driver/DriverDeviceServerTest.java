/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.driver.GrpcDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcOffsetDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcOffsetPageDeviceDTO;
import io.github.pnoker.api.common.driver.GrpcDeviceAttachDTO;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverDeviceServerTest {

    @Test
    void listReturnsOffsetPageWithAttachedMetadata() {
        GrpcDeviceBuilder deviceBuilder = mock(GrpcDeviceBuilder.class);
        ReactiveDeviceService deviceService = mock(ReactiveDeviceService.class);
        ReactiveDriverService driverService = mock(ReactiveDriverService.class);
        ReactivePointService pointService = mock(ReactivePointService.class);
        ReactiveDriverAttributeConfigService driverConfigService = mock(ReactiveDriverAttributeConfigService.class);
        ReactivePointAttributeConfigService pointConfigService = mock(ReactivePointAttributeConfigService.class);
        ReactiveCommandAttributeConfigService commandConfigService = mock(ReactiveCommandAttributeConfigService.class);
        ReactiveCommandService commandService = mock(ReactiveCommandService.class);
        ReactiveEventAttributeConfigService eventConfigService = mock(ReactiveEventAttributeConfigService.class);
        ReactiveEventService eventService = mock(ReactiveEventService.class);
        DriverDeviceServer server = new DriverDeviceServer(deviceBuilder,
                mock(GrpcDriverAttributeConfigBuilder.class), mock(GrpcPointAttributeConfigBuilder.class),
                mock(GrpcCommandAttributeConfigBuilder.class), mock(GrpcEventAttributeConfigBuilder.class),
                deviceService, driverService, pointService, driverConfigService, pointConfigService,
                commandConfigService, commandService, eventConfigService, eventService);
        DriverBO driver = new DriverBO();
        driver.setId(2L);
        driver.setTenantId(1L);
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        device.setDriverId(2L);
        device.setTenantId(1L);
        when(driverService.getById(1L, 2L)).thenReturn(Mono.just(driver));
        when(deviceService.list(org.mockito.ArgumentMatchers.any())).thenReturn(
                Mono.just(new OffsetPage<>(List.of(device), 0, 50, 1, false)));
        when(pointService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(driverConfigService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(pointConfigService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(commandConfigService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(commandService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(eventConfigService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(eventService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(deviceBuilder.buildGrpcDTOByBO(device)).thenReturn(GrpcDeviceDTO.getDefaultInstance());
        CapturingObserver<GrpcOffsetPageDeviceDTO> observer = new CapturingObserver<>();

        server.list(GrpcOffsetDeviceQuery.newBuilder().setTenantId(1L).setDriverId(2L)
                .setPage(io.github.pnoker.api.common.PageRequest.newBuilder().setOffset(0).setLimit(50)).build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.completed).isTrue();
        assertThat(observer.value.getPage().getOffset()).isZero();
        assertThat(observer.value.getPage().getLimit()).isEqualTo(50);
        assertThat(observer.value.getItemsCount()).isEqualTo(1);
    }

    @Test
    void attachContainsOnlyEnabledDeviceCommandsAndEvents() {
        GrpcDeviceBuilder deviceBuilder = mock(GrpcDeviceBuilder.class);
        ReactiveDeviceService deviceService = mock(ReactiveDeviceService.class);
        ReactiveDriverService driverService = mock(ReactiveDriverService.class);
        ReactivePointService pointService = mock(ReactivePointService.class);
        ReactiveDriverAttributeConfigService driverConfigService = mock(ReactiveDriverAttributeConfigService.class);
        ReactivePointAttributeConfigService pointConfigService = mock(ReactivePointAttributeConfigService.class);
        ReactiveCommandAttributeConfigService commandConfigService = mock(ReactiveCommandAttributeConfigService.class);
        ReactiveCommandService commandService = mock(ReactiveCommandService.class);
        ReactiveEventAttributeConfigService eventConfigService = mock(ReactiveEventAttributeConfigService.class);
        ReactiveEventService eventService = mock(ReactiveEventService.class);
        DriverDeviceServer server = new DriverDeviceServer(deviceBuilder,
                mock(GrpcDriverAttributeConfigBuilder.class), mock(GrpcPointAttributeConfigBuilder.class),
                mock(GrpcCommandAttributeConfigBuilder.class), mock(GrpcEventAttributeConfigBuilder.class),
                deviceService, driverService, pointService, driverConfigService, pointConfigService,
                commandConfigService, commandService, eventConfigService, eventService);
        DriverBO driver = new DriverBO();
        driver.setId(2L);
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        device.setDriverId(2L);
        device.setTenantId(1L);
        when(driverService.getById(1L, 2L)).thenReturn(Mono.just(driver));
        when(deviceService.getById(1L, 10L)).thenReturn(Mono.just(device));
        when(pointService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(driverConfigService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(pointConfigService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(commandConfigService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(commandService.listByDeviceId(1L, 10L)).thenReturn(Flux.just(
                command(30L, EnableFlagEnum.ENABLE), command(31L, EnableFlagEnum.DISABLE)));
        when(eventConfigService.listByDeviceId(1L, 10L)).thenReturn(Flux.empty());
        when(eventService.listByDeviceId(1L, 10L)).thenReturn(Flux.just(
                event(20L, EnableFlagEnum.ENABLE), event(21L, EnableFlagEnum.DISABLE)));
        when(deviceBuilder.buildGrpcDTOByBO(device)).thenReturn(GrpcDeviceDTO.getDefaultInstance());
        CapturingObserver<GrpcDeviceAttachDTO> observer = new CapturingObserver<>();

        server.getById(GrpcDeviceQuery.newBuilder().setTenantId(1L).setDriverId(2L).setDeviceId(10L).build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.completed).isTrue();
        assertThat(observer.value.getEventsList())
                .extracting(event -> event.getEventId())
                .containsExactly("20");
        assertThat(observer.value.getCommandsList())
                .extracting(command -> command.getCommandId())
                .containsExactly("30");
        verify(commandService).listByDeviceId(1L, 10L);
        verify(eventService).listByDeviceId(1L, 10L);
    }

    private CommandBO command(Long id, EnableFlagEnum enableFlag) {
        CommandBO command = new CommandBO();
        command.setId(id);
        command.setCommandName("Command " + id);
        command.setCommandCode("command-" + id);
        command.setCommandTypeFlag(CommandTypeEnum.CUSTOM);
        command.setCallTypeFlag(CallTypeEnum.ASYNC);
        command.setTimeout(5);
        command.setEnableFlag(enableFlag);
        command.setVersion(1);
        return command;
    }

    private EventBO event(Long id, EnableFlagEnum enableFlag) {
        EventBO event = new EventBO();
        event.setId(id);
        event.setEventName("Event " + id);
        event.setEventCode("event-" + id);
        event.setEventTypeFlag(EventTypeFlagEnum.INFO);
        event.setEventLevelFlag(EventLevelEnum.LOW);
        event.setEnableFlag(enableFlag);
        event.setVersion(1);
        return event;
    }

    private static final class CapturingObserver<T> implements StreamObserver<T> {
        private T value;
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }
    }
}

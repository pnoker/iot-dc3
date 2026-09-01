package io.github.pnoker.common.manager.grpc.server.driver;

import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcCommandRuntimeDTO;
import io.github.pnoker.api.common.GrpcEventRuntimeDTO;
import io.github.pnoker.api.common.driver.DeviceApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcOffsetDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcOffsetPageDeviceDTO;
import io.github.pnoker.api.common.driver.GrpcDeviceAttachDTO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/** Reactive gRPC server handling driver-to-manager device requests. */
@Service
@RequiredArgsConstructor
public class DriverDeviceServer extends DeviceApiGrpc.DeviceApiImplBase {
    private final GrpcDeviceBuilder grpcDeviceBuilder;
    private final GrpcDriverAttributeConfigBuilder grpcDriverAttributeConfigBuilder;
    private final GrpcPointAttributeConfigBuilder grpcPointAttributeConfigBuilder;
    private final GrpcCommandAttributeConfigBuilder grpcCommandAttributeConfigBuilder;
    private final GrpcEventAttributeConfigBuilder grpcEventAttributeConfigBuilder;
    private final ReactiveDeviceService deviceService;
    private final ReactiveDriverService driverService;
    private final ReactivePointService pointService;
    private final ReactiveDriverAttributeConfigService driverAttributeConfigService;
    private final ReactivePointAttributeConfigService pointAttributeConfigService;
    private final ReactiveCommandAttributeConfigService commandAttributeConfigService;
    private final ReactiveCommandService commandService;
    private final ReactiveEventAttributeConfigService eventAttributeConfigService;
    private final ReactiveEventService eventService;

    @Override
    public void list(GrpcOffsetDeviceQuery request, StreamObserver<GrpcOffsetPageDeviceDTO> responseObserver) {
        Mono<GrpcOffsetPageDeviceDTO> response = Mono.defer(() -> {
            var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
            long offset = page.offset();
            int limit = page.limit();
            DeviceFilter filter = new DeviceFilter(request.getTenantId(), null, null,
                    request.getDriverId(), null, null, null, null, null, offset, limit, page.sort());
            return driverService.getById(request.getTenantId(), request.getDriverId())
                    .switchIfEmpty(Mono.error(new IllegalStateException("driver does not exist")))
                    .then(deviceService.list(filter))
                    .flatMap(result -> Flux.fromIterable(result.items()).concatMap(this::getDeviceAttachDTO, 8)
                            .collectList()
                            .map(items -> GrpcOffsetPageDeviceDTO.newBuilder()
                                    .setPage(io.github.pnoker.api.common.OffsetPage.newBuilder()
                                            .setOffset(result.offset()).setLimit(result.limit())
                                            .setTotal(result.total()).setHasNext(result.hasNext()))
                                    .addAllItems(items)
                                    .build()));
        });
        io.github.pnoker.common.manager.grpc.server.manager.ReactiveGrpcServerSupport.subscribe(response, responseObserver);
    }

    @Override
    public void getById(GrpcDeviceQuery request, StreamObserver<GrpcDeviceAttachDTO> responseObserver) {
        Mono<GrpcDeviceAttachDTO> response = driverService.getById(request.getTenantId(), request.getDriverId())
                .zipWith(deviceService.getById(request.getTenantId(), request.getDeviceId()))
                .filter(tuple -> java.util.Objects.equals(tuple.getT2().getDriverId(), tuple.getT1().getId()))
                .flatMap(tuple -> getDeviceAttachDTO(tuple.getT2()))
                .switchIfEmpty(Mono.error(new io.github.pnoker.common.exception.NotFoundException("device does not exist")));
        io.github.pnoker.common.manager.grpc.server.manager.ReactiveGrpcServerSupport.subscribe(response, responseObserver);
    }

    private Mono<GrpcDeviceAttachDTO> getDeviceAttachDTO(DeviceBO device) {
        Mono<List<Long>> pointIds = pointService.listByDeviceId(device.getTenantId(), device.getId()).map(value -> value.getId()).collectList();
        Mono<List<DriverAttributeConfigBO>> driverConfigs = driverAttributeConfigService.listByDeviceId(device.getTenantId(), device.getId()).collectList();
        Mono<List<PointAttributeConfigBO>> pointConfigs = pointAttributeConfigService.listByDeviceId(device.getTenantId(), device.getId()).collectList();
        Mono<List<CommandAttributeConfigBO>> commandConfigs = commandAttributeConfigService.listByDeviceId(device.getTenantId(), device.getId()).collectList();
        Mono<List<EventAttributeConfigBO>> eventConfigs = eventAttributeConfigService.listByDeviceId(device.getTenantId(), device.getId()).collectList();
        Mono<List<CommandBO>> commands = commandService.listByDeviceId(device.getTenantId(), device.getId())
                .filter(command -> io.github.pnoker.common.enums.EnableFlagEnum.ENABLE.equals(command.getEnableFlag()))
                .collectList();
        Mono<List<EventBO>> events = eventService.listByDeviceId(device.getTenantId(), device.getId())
                .filter(event -> io.github.pnoker.common.enums.EnableFlagEnum.ENABLE.equals(event.getEnableFlag()))
                .collectList();
        return Mono.zip(pointIds, driverConfigs, pointConfigs, commandConfigs, eventConfigs, events, commands)
                .map(tuple -> GrpcDeviceAttachDTO.newBuilder()
                        .setDevice(grpcDeviceBuilder.buildGrpcDTOByBO(device))
                        .addAllPointIds(tuple.getT1())
                        .addAllDriverConfigs(tuple.getT2().stream().map(grpcDriverAttributeConfigBuilder::buildGrpcDTOByBO).toList())
                        .addAllPointConfigs(tuple.getT3().stream().map(grpcPointAttributeConfigBuilder::buildGrpcDTOByBO).toList())
                        .addAllCommandConfigs(tuple.getT4().stream().map(grpcCommandAttributeConfigBuilder::buildGrpcDTOByBO).toList())
                        .addAllEventConfigs(tuple.getT5().stream().map(grpcEventAttributeConfigBuilder::buildGrpcDTOByBO).toList())
                        .addAllEvents(tuple.getT6().stream().map(this::eventRuntime).toList())
                        .addAllCommands(tuple.getT7().stream().map(this::commandRuntime).toList())
                        .build());
    }

    private GrpcCommandRuntimeDTO commandRuntime(CommandBO command) {
        GrpcCommandRuntimeDTO.Builder builder = GrpcCommandRuntimeDTO.newBuilder()
                .setCommandId(String.valueOf(command.getId()))
                .setCommandName(command.getCommandName())
                .setCommandCode(command.getCommandCode())
                .setCommandTypeFlag(command.getCommandTypeFlag().getIndex())
                .setCallTypeFlag(command.getCallTypeFlag().getIndex())
                .setEnableFlag(command.getEnableFlag().getIndex());
        if (command.getTimeout() != null) {
            builder.setTimeout(command.getTimeout());
        }
        if (command.getCommandExt() != null) {
            builder.setCommandExt(io.github.pnoker.common.utils.JsonUtil.toJsonString(command.getCommandExt()));
        }
        if (command.getVersion() != null) {
            builder.setVersion(command.getVersion());
        }
        return builder.build();
    }

    private GrpcEventRuntimeDTO eventRuntime(EventBO event) {
        GrpcEventRuntimeDTO.Builder builder = GrpcEventRuntimeDTO.newBuilder()
                .setEventId(String.valueOf(event.getId()))
                .setEventName(event.getEventName())
                .setEventCode(event.getEventCode())
                .setEventTypeFlag(event.getEventTypeFlag().getIndex())
                .setEventLevelFlag(event.getEventLevelFlag().getIndex())
                .setEnableFlag(event.getEnableFlag().getIndex());
        if (event.getVersion() != null) {
            builder.setVersion(event.getVersion());
        }
        return builder.build();
    }

}

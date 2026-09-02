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

import io.github.pnoker.api.common.GrpcCommandAttributeConfigDTO;
import io.github.pnoker.api.common.GrpcCommandRuntimeDTO;
import io.github.pnoker.api.common.GrpcDriverAttributeConfigDTO;
import io.github.pnoker.api.common.GrpcEventAttributeConfigDTO;
import io.github.pnoker.api.common.GrpcEventRuntimeDTO;
import io.github.pnoker.api.common.GrpcPointAttributeConfigDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.driver.DeviceApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDeviceAttachDTO;
import io.github.pnoker.api.common.driver.GrpcDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcOffsetDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcOffsetPageDeviceDTO;
import io.github.pnoker.common.driver.entity.bo.CommandRuntimeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.bo.EventRuntimeBO;
import io.github.pnoker.common.driver.entity.builder.DeviceBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcCommandAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcEventAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.dto.CommandAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeConfigDTO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.entity.ext.CommandExt;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.optional.CollectionOptional;
import io.github.pnoker.common.utils.JsonUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * gRPC client used to query device metadata and device-specific attribute configuration
 * from the manager center.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceClient {

    private final DeviceApiGrpc.DeviceApiStub deviceApiStub;

    private final DriverMetadata driverMetadata;

    private final DeviceBuilder deviceBuilder;

    private final GrpcDriverAttributeConfigBuilder grpcDriverAttributeConfigBuilder;

    private final GrpcPointAttributeConfigBuilder grpcPointAttributeConfigBuilder;

    private final GrpcCommandAttributeConfigBuilder grpcCommandAttributeConfigBuilder;

    private final GrpcEventAttributeConfigBuilder grpcEventAttributeConfigBuilder;

    /**
     * Fetch all devices via paginated gRPC calls, looping through every page and
     * accumulating the results.
     *
     * @return all devices
     */
    public Flux<DeviceBO> list() {
        return loadPage(0, 200)
                .expand(page -> page.hasNext()
                        ? loadPage(page.offset() + Math.max(page.limit(), 1), page.limit())
                        : Mono.empty())
                .concatMapIterable(DevicePage::data)
                .map(this::buildDTOByGrpcAttachDTO);
    }

    /**
     * Performs a gRPC getById lookup scoped to the current tenant and driver, returning
     * null (after logging) when the response result is not OK, otherwise a DeviceBO
     * populated with point ids and the driver, point, command and event attribute
     * configuration maps.
     *
     * @param id Device ID
     * @return DeviceBO
     */
    public Mono<DeviceBO> getById(Long id) {
        return Mono.defer(() -> {
            GrpcDeviceQuery query = GrpcDeviceQuery.newBuilder()
                    .setTenantId(driverMetadata.getDriver().getTenantId())
                    .setDriverId(driverMetadata.getDriver().getId())
                    .setDeviceId(id)
                    .build();
            return ReactiveGrpcClientSupport.<GrpcDeviceQuery, GrpcDeviceAttachDTO>unary(
                            "get device metadata", observer -> deviceApiStub.getById(query, observer))
                    .map(this::buildDTOByGrpcAttachDTO);
        });
    }

    private Mono<DevicePage> loadPage(long offset, int limit) {
        GrpcOffsetDeviceQuery query = GrpcOffsetDeviceQuery.newBuilder()
                .setTenantId(driverMetadata.getDriver().getTenantId())
                .setDriverId(driverMetadata.getDriver().getId())
                .setPage(PageRequest.newBuilder()
                        .setOffset(offset)
                        .setLimit(limit)
                        .build())
                .build();
        return ReactiveGrpcClientSupport.<GrpcOffsetDeviceQuery, GrpcOffsetPageDeviceDTO>unary(
                        "list device metadata", observer -> deviceApiStub.list(query, observer))
                .map(response -> new DevicePage(
                        response.getPage().getOffset(),
                        response.getPage().getLimit(),
                        response.getPage().getHasNext(),
                        response.getItemsList()));
    }

    private record DevicePage(long offset, int limit, boolean hasNext, List<GrpcDeviceAttachDTO> data) {}

    DeviceBO buildDTOByGrpcAttachDTO(GrpcDeviceAttachDTO rDeviceAttachDTO) {
        DeviceBO deviceBO = deviceBuilder.buildDTOByGrpcDTO(rDeviceAttachDTO.getDevice());
        deviceBO.setPointIds(new HashSet<>(rDeviceAttachDTO.getPointIdsList()));

        CollectionOptional.ofNullable(rDeviceAttachDTO.getDriverConfigsList()).ifPresent(value -> {
            Map<Long, DriverAttributeConfigDTO> driverAttributeConfigMap = value.stream()
                    .collect(Collectors.toMap(
                            GrpcDriverAttributeConfigDTO::getAttributeId,
                            grpcDriverAttributeConfigBuilder::buildDTOByGrpcDTO));
            deviceBO.setDriverAttributeConfigIdMap(driverAttributeConfigMap);
        });

        CollectionOptional.ofNullable(rDeviceAttachDTO.getPointConfigsList()).ifPresent(value -> {
            Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = value.stream()
                    .collect(Collectors.groupingBy(
                            GrpcPointAttributeConfigDTO::getPointId,
                            Collectors.toMap(
                                    GrpcPointAttributeConfigDTO::getAttributeId,
                                    grpcPointAttributeConfigBuilder::buildDTOByGrpcDTO)));
            deviceBO.setPointAttributeConfigIdMap(pointAttributeConfigMap);
        });

        CollectionOptional.ofNullable(rDeviceAttachDTO.getCommandConfigsList()).ifPresent(value -> {
            Map<Long, Map<Long, CommandAttributeConfigDTO>> commandAttributeConfigMap = value.stream()
                    .collect(Collectors.groupingBy(
                            GrpcCommandAttributeConfigDTO::getCommandId,
                            Collectors.toMap(
                                    GrpcCommandAttributeConfigDTO::getAttributeId,
                                    grpcCommandAttributeConfigBuilder::buildDTOByGrpcDTO)));
            deviceBO.setCommandAttributeConfigIdMap(commandAttributeConfigMap);
        });

        deviceBO.setCommandRuntimeIdMap(rDeviceAttachDTO.getCommandsList().stream()
                .map(this::commandRuntime)
                .collect(Collectors.toUnmodifiableMap(CommandRuntimeBO::id, command -> command)));

        CollectionOptional.ofNullable(rDeviceAttachDTO.getEventConfigsList()).ifPresent(value -> {
            Map<Long, Map<Long, EventAttributeConfigDTO>> eventAttributeConfigMap = value.stream()
                    .collect(Collectors.groupingBy(
                            GrpcEventAttributeConfigDTO::getEventId,
                            Collectors.toMap(
                                    GrpcEventAttributeConfigDTO::getAttributeId,
                                    grpcEventAttributeConfigBuilder::buildDTOByGrpcDTO)));
            deviceBO.setEventAttributeConfigIdMap(eventAttributeConfigMap);
        });

        deviceBO.setEventRuntimeIdMap(rDeviceAttachDTO.getEventsList().stream()
                .map(this::eventRuntime)
                .collect(Collectors.toUnmodifiableMap(EventRuntimeBO::id, event -> event)));

        return deviceBO;
    }

    private EventRuntimeBO eventRuntime(GrpcEventRuntimeDTO event) {
        return new EventRuntimeBO(
                Long.valueOf(event.getEventId()),
                event.getEventName(),
                event.getEventCode(),
                EventTypeFlagEnum.ofIndex((byte) event.getEventTypeFlag()),
                EventLevelEnum.ofIndex((byte) event.getEventLevelFlag()),
                EnableFlagEnum.ofIndex((byte) event.getEnableFlag()),
                event.getVersion());
    }

    private CommandRuntimeBO commandRuntime(GrpcCommandRuntimeDTO command) {
        return new CommandRuntimeBO(
                Long.valueOf(command.getCommandId()),
                command.getCommandName(),
                command.getCommandCode(),
                CommandTypeEnum.ofIndex((byte) command.getCommandTypeFlag()),
                CallTypeEnum.ofIndex((byte) command.getCallTypeFlag()),
                command.getTimeout(),
                command.getCommandExt().isBlank()
                        ? null
                        : JsonUtil.parseObject(command.getCommandExt(), CommandExt.class),
                EnableFlagEnum.ofIndex((byte) command.getEnableFlag()),
                command.getVersion());
    }
}

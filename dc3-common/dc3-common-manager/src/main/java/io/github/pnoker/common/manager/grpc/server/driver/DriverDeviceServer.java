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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.api.common.driver.DeviceApiGrpc;
import io.github.pnoker.api.common.driver.GrpcDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcPageDeviceDTO;
import io.github.pnoker.api.common.driver.GrpcPageDeviceQuery;
import io.github.pnoker.api.common.driver.GrpcRDeviceAttachDTO;
import io.github.pnoker.api.common.driver.GrpcRDeviceDTO;
import io.github.pnoker.api.common.driver.GrpcRPageDeviceDTO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDeviceBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventAttributeConfigBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.common.manager.service.CommandAttributeConfigService;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverAttributeConfigService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.EventAttributeConfigService;
import io.github.pnoker.common.manager.service.PointAttributeConfigService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.optional.CollectionOptional;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * gRPC server handling driver-to-manager device requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverDeviceServer extends DeviceApiGrpc.DeviceApiImplBase {

    private final GrpcDeviceBuilder grpcDeviceBuilder;

    private final GrpcDriverAttributeConfigBuilder grpcDriverAttributeConfigBuilder;

    private final GrpcPointAttributeConfigBuilder grpcPointAttributeConfigBuilder;

    private final GrpcCommandAttributeConfigBuilder grpcCommandAttributeConfigBuilder;

    private final GrpcEventAttributeConfigBuilder grpcEventAttributeConfigBuilder;

    private final DeviceService deviceService;

    private final DriverService driverService;

    private final PointService pointService;

    private final DriverAttributeConfigService driverAttributeConfigService;

    private final PointAttributeConfigService pointAttributeConfigService;

    private final CommandAttributeConfigService commandAttributeConfigService;

    private final EventAttributeConfigService eventAttributeConfigService;

    @Override
    public void listByPage(GrpcPageDeviceQuery request, StreamObserver<GrpcRPageDeviceDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRPageDeviceDTO.Builder builder = GrpcRPageDeviceDTO.newBuilder();
            GrpcR result;

            DeviceQuery query = grpcDeviceBuilder.buildQueryByGrpcQuery(request);

            Page<DeviceBO> entityPage = driverInTenant(query.getTenantId(), query.getDriverId())
                    ? deviceService.list(query) : null;
            if (Objects.isNull(entityPage)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                GrpcPageDeviceDTO.Builder pageBuilder = GrpcPageDeviceDTO.newBuilder();
                GrpcPage.Builder page = GrpcPage.newBuilder();
                page.setCurrent(entityPage.getCurrent());
                page.setSize(entityPage.getSize());
                page.setPages(entityPage.getPages());
                page.setTotal(entityPage.getTotal());
                pageBuilder.setPage(page);

                List<GrpcRDeviceAttachDTO> entityGrpcDTOList = entityPage.getRecords()
                        .stream()
                        .map(entityBO -> getDeviceAttachDTO(entityBO).build())
                        .toList();
                pageBuilder.addAllData(entityGrpcDTOList);

                builder.setData(pageBuilder);
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getById(GrpcDeviceQuery request, StreamObserver<GrpcRDeviceDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRDeviceDTO.Builder builder = GrpcRDeviceDTO.newBuilder();
            GrpcR result;

            DriverBO driverBO = selectDriver(request.getDriverId());
            DeviceBO entityBO = selectDevice(request.getDeviceId());
            if (Objects.isNull(entityBO) || Objects.isNull(driverBO)
                    || !Objects.equals(entityBO.getDriverId(), driverBO.getId())
                    || !Objects.equals(entityBO.getTenantId(), driverBO.getTenantId())) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                builder.setData(getDeviceAttachDTO(entityBO));
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private boolean driverInTenant(Long tenantId, Long driverId) {
        DriverBO driverBO = selectDriver(driverId);
        return Objects.nonNull(driverBO) && Objects.equals(tenantId, driverBO.getTenantId());
    }

    private DriverBO selectDriver(Long driverId) {
        try {
            return driverService.getById(driverId);
        } catch (Exception e) {
            return null;
        }
    }

    private DeviceBO selectDevice(Long deviceId) {
        try {
            return deviceService.getById(deviceId);
        } catch (Exception e) {
            return null;
        }
    }

    private GrpcRDeviceAttachDTO.Builder getDeviceAttachDTO(DeviceBO entityBO) {
        GrpcRDeviceAttachDTO.Builder builder = GrpcRDeviceAttachDTO.newBuilder();
        GrpcDeviceDTO entityGrpcDTO = grpcDeviceBuilder.buildGrpcDTOByBO(entityBO);
        builder.setDevice(entityGrpcDTO);

        // Attach the device's point ids
        List<PointBO> pointBOList = pointService.listByDeviceId(entityBO.getId(), entityBO.getTenantId());
        CollectionOptional.ofNullable(pointBOList)
                .ifPresent(value -> builder.addAllPointIds(value.stream().map(PointBO::getId).toList()));

        List<DriverAttributeConfigBO> driverAttributeConfigBOList = driverAttributeConfigService
                .listByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(driverAttributeConfigBOList)
                .ifPresent(value -> builder
                        .addAllDriverConfigs(value.stream().map(grpcDriverAttributeConfigBuilder::buildGrpcDTOByBO).toList()));

        List<PointAttributeConfigBO> pointAttributeConfigBOList = pointAttributeConfigService
                .listByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(pointAttributeConfigBOList)
                .ifPresent(value -> builder
                        .addAllPointConfigs(value.stream().map(grpcPointAttributeConfigBuilder::buildGrpcDTOByBO).toList()));

        List<CommandAttributeConfigBO> commandAttributeConfigBOList = commandAttributeConfigService
                .listByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(commandAttributeConfigBOList)
                .ifPresent(value -> builder
                        .addAllCommandConfigs(value.stream().map(grpcCommandAttributeConfigBuilder::buildGrpcDTOByBO).toList()));

        List<EventAttributeConfigBO> eventAttributeConfigBOList = eventAttributeConfigService
                .listByDeviceId(entityBO.getId());
        CollectionOptional.ofNullable(eventAttributeConfigBOList)
                .ifPresent(value -> builder
                        .addAllEventConfigs(value.stream().map(grpcEventAttributeConfigBuilder::buildGrpcDTOByBO).toList()));
        return builder;
    }

}

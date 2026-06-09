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

import io.github.pnoker.api.center.data.GrpcDriverStatusQuery;
import io.github.pnoker.api.center.data.GrpcFleetSummaryDTO;
import io.github.pnoker.api.center.data.GrpcIdsStatusQuery;
import io.github.pnoker.api.center.data.GrpcProfileStatusQuery;
import io.github.pnoker.api.center.data.GrpcRStatusMap;
import io.github.pnoker.api.center.data.GrpcRStringMap;
import io.github.pnoker.api.center.data.GrpcRSystemHealthDTO;
import io.github.pnoker.api.center.data.GrpcSystemHealthDTO;
import io.github.pnoker.api.center.data.GrpcTenantHealthQuery;
import io.github.pnoker.api.center.data.StatusHealthApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.data.biz.SystemHealthService;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.entity.vo.dashboard.SystemHealthVO;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * gRPC server implementation for status and health queries.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatusHealthServer extends StatusHealthApiGrpc.StatusHealthApiImplBase {

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final EntityStateManager entityStateManager;

    private final SystemHealthService systemHealthService;

    @Override
    public void deviceStatusesByIds(GrpcIdsStatusQuery request, StreamObserver<GrpcRStatusMap> responseObserver) {
        List<FacadeDeviceBO> devices = deviceFacade.listByIds(request.getTenantId(), request.getIdsList());
        Map<Long, String> statuses = new LinkedHashMap<>();
        devices.forEach(device -> statuses.put(device.getId(), deviceStatus(request.getTenantId(), device.getId())));
        responseObserver.onNext(GrpcRStatusMap.newBuilder().setResult(ok()).putAllData(statuses).build());
        responseObserver.onCompleted();
    }

    @Override
    public void deviceStatusesByProfileId(GrpcProfileStatusQuery request,
                                          StreamObserver<GrpcRStatusMap> responseObserver) {
        List<FacadeDeviceBO> devices = deviceFacade.listByProfileId(request.getTenantId(), request.getProfileId());
        Map<Long, String> statuses = new LinkedHashMap<>();
        devices.forEach(device -> statuses.put(device.getId(), deviceStatus(request.getTenantId(), device.getId())));
        responseObserver.onNext(GrpcRStatusMap.newBuilder().setResult(ok()).putAllData(statuses).build());
        responseObserver.onCompleted();
    }

    @Override
    public void driverStatusesByIds(GrpcIdsStatusQuery request, StreamObserver<GrpcRStatusMap> responseObserver) {
        List<FacadeDriverBO> drivers = driverFacade.listByIds(request.getTenantId(), request.getIdsList());
        Map<Long, String> statuses = new LinkedHashMap<>();
        drivers.forEach(driver -> statuses.put(driver.getId(), driverStatus(request.getTenantId(), driver.getId())));
        responseObserver.onNext(GrpcRStatusMap.newBuilder().setResult(ok()).putAllData(statuses).build());
        responseObserver.onCompleted();
    }

    @Override
    public void driverDeviceStatusSummary(GrpcDriverStatusQuery request,
                                          StreamObserver<GrpcRStringMap> responseObserver) {
        FacadeDriverBO driver = driverFacade.getById(request.getTenantId(), request.getDriverId());
        if (Objects.isNull(driver)) {
            responseObserver.onNext(GrpcRStringMap.newBuilder().setResult(noResource()).build());
            responseObserver.onCompleted();
            return;
        }
        List<FacadeDeviceBO> devices = deviceFacade.listByDriverId(request.getTenantId(), request.getDriverId());
        long online = devices.stream()
                .filter(device -> Objects.equals(EntityStatusEnum.ONLINE.getCode(),
                        deviceStatus(request.getTenantId(), device.getId())))
                .count();
        FacadeDriverDeviceStatusSummaryBO summary = new FacadeDriverDeviceStatusSummaryBO(
                request.getDriverId(),
                devices.size(),
                (int) online,
                (int) Math.max(0, devices.size() - online));
        responseObserver.onNext(GrpcRStringMap.newBuilder().setResult(ok()).putAllData(summary.toMap()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void systemHealth(GrpcTenantHealthQuery request, StreamObserver<GrpcRSystemHealthDTO> responseObserver) {
        SystemHealthVO health = systemHealthService.snapshot(request.getTenantId());
        if (Objects.isNull(health)) {
            responseObserver.onNext(GrpcRSystemHealthDTO.newBuilder().setResult(noResource()).build());
            responseObserver.onCompleted();
            return;
        }
        GrpcSystemHealthDTO dto = GrpcSystemHealthDTO.newBuilder()
                .putAllCenter(nullToEmpty(health.getCenter()))
                .putAllInfra(nullToEmpty(health.getInfra()))
                .setDrivers(toGrpcSummary(health.getDrivers()))
                .setDevices(toGrpcSummary(health.getDevices()))
                .build();
        responseObserver.onNext(GrpcRSystemHealthDTO.newBuilder().setResult(ok()).setData(dto).build());
        responseObserver.onCompleted();
    }

    private Map<String, String> nullToEmpty(Map<String, String> source) {
        return Objects.nonNull(source) ? source : Map.of();
    }

    private GrpcFleetSummaryDTO toGrpcSummary(SystemHealthVO.FleetSummary summary) {
        if (Objects.isNull(summary)) {
            return GrpcFleetSummaryDTO.newBuilder().build();
        }
        return GrpcFleetSummaryDTO.newBuilder().setTotal(summary.getTotal()).setOnline(summary.getOnline()).build();
    }

    private String deviceStatus(Long tenantId, Long deviceId) {
        EntityStateDO state = entityStateManager.lambdaQuery()
                .eq(EntityStateDO::getTenantId, tenantId)
                .eq(EntityStateDO::getEntityTypeFlag, EntityTypeEnum.DEVICE.getIndex())
                .eq(EntityStateDO::getEntityId, deviceId)
                .one();
        if (Objects.isNull(state) || state.getExpireTime().isBefore(LocalDateTime.now())) {
            return EntityStatusEnum.OFFLINE.getCode();
        }
        EntityStatusEnum e = EntityStatusEnum.ofIndex(state.getStateFlag());
        return Objects.nonNull(e) ? e.getCode() : EntityStatusEnum.OFFLINE.getCode();
    }

    private String driverStatus(Long tenantId, Long driverId) {
        EntityStateDO state = entityStateManager.lambdaQuery()
                .eq(EntityStateDO::getTenantId, tenantId)
                .eq(EntityStateDO::getEntityTypeFlag, EntityTypeEnum.DRIVER.getIndex())
                .eq(EntityStateDO::getEntityId, driverId)
                .one();
        if (Objects.isNull(state) || state.getExpireTime().isBefore(LocalDateTime.now())) {
            return EntityStatusEnum.OFFLINE.getCode();
        }
        EntityStatusEnum e = EntityStatusEnum.ofIndex(state.getStateFlag());
        return Objects.nonNull(e) ? e.getCode() : EntityStatusEnum.OFFLINE.getCode();
    }

    private GrpcR ok() {
        return GrpcR.newBuilder()
                .setOk(true)
                .setCode(ResponseEnum.OK.getCode())
                .setMessage(ResponseEnum.OK.getRemark())
                .build();
    }

    private GrpcR noResource() {
        return GrpcR.newBuilder()
                .setOk(false)
                .setCode(ResponseEnum.NO_RESOURCE.getCode())
                .setMessage(ResponseEnum.NO_RESOURCE.getRemark())
                .build();
    }

}

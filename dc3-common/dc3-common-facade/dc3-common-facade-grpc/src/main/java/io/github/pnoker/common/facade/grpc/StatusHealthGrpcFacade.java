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

package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.data.GrpcDriverStatusQuery;
import io.github.pnoker.api.center.data.GrpcIdsStatusQuery;
import io.github.pnoker.api.center.data.GrpcProfileStatusQuery;
import io.github.pnoker.api.center.data.GrpcRStatusMap;
import io.github.pnoker.api.center.data.GrpcRStringMap;
import io.github.pnoker.api.center.data.GrpcRSystemHealthDTO;
import io.github.pnoker.api.center.data.GrpcSystemHealthDTO;
import io.github.pnoker.api.center.data.GrpcTenantHealthQuery;
import io.github.pnoker.api.center.data.StatusHealthApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.github.pnoker.common.facade.entity.bo.FacadeSystemHealthBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * gRPC StatusHealthFacade: forwards to Data Center via {@link StatusHealthApiGrpc}.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusHealthGrpcFacade implements StatusHealthFacade {

    private final StatusHealthApiGrpc.StatusHealthApiBlockingStub statusHealthApiBlockingStub;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Map<Long, String> listDeviceStatusesByIds(Long tenantId, Collection<Long> deviceIds) {
        GrpcIdsStatusQuery request = idsQuery(tenantId, deviceIds);
        GrpcRStatusMap response = grpcFacadeSupport.call("StatusHealthFacade.listDeviceStatusesByIds",
                statusHealthApiBlockingStub, stub -> stub.deviceStatusesByIds(request));
        return statusMap(response.getResult(), response.getDataMap(), "listDeviceStatusesByIds");
    }

    @Override
    public Map<Long, String> listDeviceStatusesByProfileId(Long tenantId, Long profileId) {
        GrpcProfileStatusQuery request = GrpcProfileStatusQuery.newBuilder()
                .setTenantId(Objects.requireNonNullElse(tenantId, 0L))
                .setProfileId(Objects.requireNonNullElse(profileId, 0L))
                .build();
        GrpcRStatusMap response = grpcFacadeSupport.call("StatusHealthFacade.listDeviceStatusesByProfileId",
                statusHealthApiBlockingStub, stub -> stub.deviceStatusesByProfileId(request));
        return statusMap(response.getResult(), response.getDataMap(), "listDeviceStatusesByProfileId");
    }

    @Override
    public Map<Long, String> listDriverStatusesByIds(Long tenantId, Collection<Long> driverIds) {
        GrpcIdsStatusQuery request = idsQuery(tenantId, driverIds);
        GrpcRStatusMap response = grpcFacadeSupport.call("StatusHealthFacade.listDriverStatusesByIds",
                statusHealthApiBlockingStub, stub -> stub.driverStatusesByIds(request));
        return statusMap(response.getResult(), response.getDataMap(), "listDriverStatusesByIds");
    }

    @Override
    public FacadeDriverDeviceStatusSummaryBO getDriverDeviceStatusSummary(Long tenantId, Long driverId) {
        GrpcDriverStatusQuery request = GrpcDriverStatusQuery.newBuilder()
                .setTenantId(Objects.requireNonNullElse(tenantId, 0L))
                .setDriverId(Objects.requireNonNullElse(driverId, 0L))
                .build();
        GrpcRStringMap response = grpcFacadeSupport.call("StatusHealthFacade.getDriverDeviceStatusSummary",
                statusHealthApiBlockingStub, stub -> stub.driverDeviceStatusSummary(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getDriverDeviceStatusSummary");
            return null;
        }
        return FacadeDriverDeviceStatusSummaryBO.fromMap(response.getDataMap());
    }

    @Override
    public FacadeSystemHealthBO systemHealth(Long tenantId) {
        GrpcTenantHealthQuery request = GrpcTenantHealthQuery.newBuilder()
                .setTenantId(Objects.requireNonNullElse(tenantId, 0L))
                .build();
        GrpcRSystemHealthDTO response = grpcFacadeSupport.call("StatusHealthFacade.systemHealth",
                statusHealthApiBlockingStub, stub -> stub.systemHealth(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "systemHealth");
            return null;
        }
        return toFacadeHealth(response.getData());
    }

    private GrpcIdsStatusQuery idsQuery(Long tenantId, Collection<Long> ids) {
        GrpcIdsStatusQuery.Builder builder = GrpcIdsStatusQuery.newBuilder()
                .setTenantId(Objects.requireNonNullElse(tenantId, 0L));
        if (Objects.nonNull(ids)) {
            builder.addAllIds(ids.stream().filter(Objects::nonNull).distinct().toList());
        }
        return builder.build();
    }

    private Map<Long, String> statusMap(GrpcR result, Map<Long, String> data, String op) {
        if (!result.getOk()) {
            guardOrThrow(result, op);
            return Collections.emptyMap();
        }
        return data;
    }

    private FacadeSystemHealthBO toFacadeHealth(GrpcSystemHealthDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }
        FacadeSystemHealthBO health = new FacadeSystemHealthBO();
        health.setCenter(dto.getCenterMap());
        health.setInfra(dto.getInfraMap());
        health.setDrivers(new FacadeSystemHealthBO.FleetSummary(dto.getDrivers().getTotal(),
                dto.getDrivers().getOnline()));
        health.setDevices(new FacadeSystemHealthBO.FleetSummary(dto.getDevices().getTotal(),
                dto.getDevices().getOnline()));
        return health;
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ResponseEnum.NO_RESOURCE.getCode().equals(code)) {
            log.debug("StatusHealthGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("StatusHealthFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

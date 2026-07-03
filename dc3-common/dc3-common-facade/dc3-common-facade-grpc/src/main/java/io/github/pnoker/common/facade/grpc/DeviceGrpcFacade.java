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

import io.github.pnoker.api.center.manager.DeviceApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDeviceIdsQuery;
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcDriverQuery;
import io.github.pnoker.api.center.manager.GrpcPageDeviceDTO;
import io.github.pnoker.api.center.manager.GrpcPageDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.center.manager.GrpcRDeviceDTO;
import io.github.pnoker.api.center.manager.GrpcRDeviceListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageDeviceDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcDeviceBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * gRPC implementation: forwards each call to Manager Center via
 * {@link DeviceApiGrpc.DeviceApiBlockingStub}.
 * <p>
 * Selected when {@code dc3.facade.mode=grpc} (or unset — grpc is the default in the
 * auto-configuration declaration).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceGrpcFacade implements DeviceFacade {

    private final DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    private final FacadeGrpcDeviceBuilder facadeGrpcDeviceBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeDeviceBO getById(Long tenantId, Long id) {
        GrpcDeviceQuery request = GrpcDeviceQuery.newBuilder().setDeviceId(id).setTenantId(tenantId).build();
        GrpcRDeviceDTO response = grpcFacadeSupport.call("DeviceFacade.getById", deviceApiBlockingStub,
                stub -> stub.getByDeviceId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getById");
            return null;
        }
        return facadeGrpcDeviceBuilder.toFacadeBO(response.getData());
    }

    @Override
    public List<FacadeDeviceBO> listByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> deviceIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        GrpcDeviceIdsQuery request = GrpcDeviceIdsQuery.newBuilder().addAllDeviceIds(deviceIds).setTenantId(tenantId).build();
        GrpcRDeviceListDTO response = grpcFacadeSupport.call("DeviceFacade.listByIds", deviceApiBlockingStub,
                stub -> stub.listByDeviceIds(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByIds");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcDeviceBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeDeviceBO> listByPage(FacadeDeviceQuery query) {
        GrpcPageDeviceQuery request = facadeGrpcDeviceBuilder.toGrpcPageQuery(query);
        GrpcRPageDeviceDTO response = grpcFacadeSupport.call("DeviceFacade.listByPage", deviceApiBlockingStub,
                stub -> stub.listByPage(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByPage");
            return FacadePage.empty();
        }

        GrpcPageDeviceDTO pageDTO = response.getData();
        List<FacadeDeviceBO> records = pageDTO.getDataList().stream().map(facadeGrpcDeviceBuilder::toFacadeBO).toList();

        return new FacadePage<>(pageDTO.getPage().getCurrent(), pageDTO.getPage().getSize(),
                pageDTO.getPage().getTotal(), pageDTO.getPage().getPages(), records);
    }

    @Override
    public List<FacadeDeviceBO> listByProfileId(Long tenantId, Long profileId) {
        GrpcProfileQuery request = GrpcProfileQuery.newBuilder().setProfileId(profileId).setTenantId(tenantId).build();
        GrpcRDeviceListDTO response = grpcFacadeSupport.call("DeviceFacade.listByProfileId", deviceApiBlockingStub,
                stub -> stub.listByProfileId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByProfileId");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcDeviceBuilder::toFacadeBO).toList();
    }

    @Override
    public List<FacadeDeviceBO> listByDriverId(Long tenantId, Long driverId) {
        GrpcDriverQuery request = GrpcDriverQuery.newBuilder().setDriverId(driverId).setTenantId(tenantId).build();
        GrpcRDeviceListDTO response = grpcFacadeSupport.call("DeviceFacade.listByDriverId", deviceApiBlockingStub,
                stub -> stub.listByDriverId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByDriverId");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcDeviceBuilder::toFacadeBO).toList();
    }

    /**
     * NO_RESOURCE is a normal "not found" signal — swallow and let the caller see null /
     * empty. Any other non-OK code (server error, param error, etc.) escalates to an
     * exception.
     */
    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ErrorCode.NOT_FOUND.getCode().equals(code)) {
            log.debug("DeviceGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("DeviceFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

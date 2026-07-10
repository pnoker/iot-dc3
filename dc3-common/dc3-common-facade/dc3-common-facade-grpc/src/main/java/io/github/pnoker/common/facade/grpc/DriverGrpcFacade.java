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

import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcDriverIdsQuery;
import io.github.pnoker.api.center.manager.GrpcPageDriverDTO;
import io.github.pnoker.api.center.manager.GrpcPageDriverQuery;
import io.github.pnoker.api.center.manager.GrpcRDriverDTO;
import io.github.pnoker.api.center.manager.GrpcRDriverListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageDriverDTO;
import io.github.pnoker.api.common.GrpcDriverQuery;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcDriverBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * gRPC DriverFacade: forwards to Manager Center via
 * {@link DriverApiGrpc.DriverApiBlockingStub}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverGrpcFacade implements DriverFacade {

    private final DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    private final FacadeGrpcDriverBuilder facadeGrpcDriverBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeDriverBO getById(Long tenantId, Long id) {
        GrpcDriverQuery request = GrpcDriverQuery.newBuilder().setDriverId(id).setTenantId(tenantId).build();
        GrpcRDriverDTO response = grpcFacadeSupport.call("DriverFacade.getById", driverApiBlockingStub,
                stub -> stub.getByDriverId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getById");
            return null;
        }
        return facadeGrpcDriverBuilder.toFacadeBO(response.getData());
    }

    @Override
    public List<FacadeDriverBO> listByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> driverIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (driverIds.isEmpty()) {
            return Collections.emptyList();
        }

        GrpcDriverIdsQuery request = GrpcDriverIdsQuery.newBuilder().addAllDriverIds(driverIds).setTenantId(tenantId).build();
        GrpcRDriverListDTO response = grpcFacadeSupport.call("DriverFacade.listByIds", driverApiBlockingStub,
                stub -> stub.listByDriverIds(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByIds");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcDriverBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeDriverBO> listByPage(FacadeDriverQuery query) {
        GrpcPageDriverQuery request = facadeGrpcDriverBuilder.toGrpcPageQuery(query);
        GrpcRPageDriverDTO response = grpcFacadeSupport.call("DriverFacade.listByPage", driverApiBlockingStub,
                stub -> stub.listByPage(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByPage");
            return FacadePage.empty();
        }

        GrpcPageDriverDTO pageDTO = response.getData();
        List<FacadeDriverBO> records = pageDTO.getDataList().stream().map(facadeGrpcDriverBuilder::toFacadeBO).toList();

        return new FacadePage<>(pageDTO.getPage().getCurrent(), pageDTO.getPage().getSize(),
                pageDTO.getPage().getTotal(), pageDTO.getPage().getPages(), records);
    }

    @Override
    public FacadeDriverBO getByDeviceId(Long tenantId, Long deviceId) {
        GrpcDeviceQuery request = GrpcDeviceQuery.newBuilder().setDeviceId(deviceId).setTenantId(tenantId).build();
        GrpcRDriverDTO response = grpcFacadeSupport.call("DriverFacade.getByDeviceId", driverApiBlockingStub,
                stub -> stub.getByDeviceId(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getByDeviceId");
            return null;
        }
        return facadeGrpcDriverBuilder.toFacadeBO(response.getData());
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ErrorCode.NOT_FOUND.getCode().equals(code)) {
            log.debug("DriverGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("DriverFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

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

import io.github.pnoker.api.center.manager.GrpcPagePointDTO;
import io.github.pnoker.api.center.manager.GrpcPagePointQuery;
import io.github.pnoker.api.center.manager.GrpcPointIdsQuery;
import io.github.pnoker.api.center.manager.GrpcPointQuery;
import io.github.pnoker.api.center.manager.GrpcRPagePointDTO;
import io.github.pnoker.api.center.manager.GrpcRPointDTO;
import io.github.pnoker.api.center.manager.GrpcRPointListDTO;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcPointBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * gRPC PointFacade: forwards to Manager Center via
 * {@link PointApiGrpc.PointApiBlockingStub}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointGrpcFacade implements PointFacade {

    private final PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;

    private final FacadeGrpcPointBuilder facadeGrpcPointBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadePointBO getById(Long tenantId, Long id) {
        GrpcPointQuery request = GrpcPointQuery.newBuilder().setPointId(id).setTenantId(tenantId).build();
        GrpcRPointDTO response = grpcFacadeSupport.call("PointFacade.getById", pointApiBlockingStub,
                stub -> stub.getById(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getById");
            return null;
        }
        return facadeGrpcPointBuilder.toFacadeBO(response.getData());
    }

    @Override
    public List<FacadePointBO> listByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> pointIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (pointIds.isEmpty()) {
            return Collections.emptyList();
        }

        GrpcPointIdsQuery request = GrpcPointIdsQuery.newBuilder().addAllPointIds(pointIds).setTenantId(tenantId).build();
        GrpcRPointListDTO response = grpcFacadeSupport.call("PointFacade.listByIds", pointApiBlockingStub,
                stub -> stub.listByIds(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByIds");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcPointBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadePointBO> listByPage(FacadePointQuery query) {
        GrpcPagePointQuery request = facadeGrpcPointBuilder.toGrpcPageQuery(query);
        GrpcRPagePointDTO response = grpcFacadeSupport.call("PointFacade.listByPage", pointApiBlockingStub,
                stub -> stub.listByPage(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByPage");
            return FacadePage.empty();
        }

        GrpcPagePointDTO pageDTO = response.getData();
        List<FacadePointBO> records = pageDTO.getDataList().stream().map(facadeGrpcPointBuilder::toFacadeBO).toList();

        return new FacadePage<>(pageDTO.getPage().getCurrent(), pageDTO.getPage().getSize(),
                pageDTO.getPage().getTotal(), pageDTO.getPage().getPages(), records);
    }

    /**
     * Guard a gRPC result: NOT_FOUND is treated as a normal empty outcome, any other
     * error code throws a service exception.
     *
     * @param result the gRPC result envelope
     * @param op     the operation name, for error messages
     */
    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ErrorCode.NOT_FOUND.getCode().equals(code)) {
            log.debug("PointGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("PointFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

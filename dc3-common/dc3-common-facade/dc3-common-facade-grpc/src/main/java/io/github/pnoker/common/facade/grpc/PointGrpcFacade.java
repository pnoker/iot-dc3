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

import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcPointBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * gRPC PointFacade: forwards to Manager Center via
 * {@link PointApiGrpc.PointApiBlockingStub}.
 *
 * @author pnoker
 * @since 2026.4.30
 */
@Slf4j
@Component
public class PointGrpcFacade implements PointFacade {

    @Resource
    private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;

    @Resource
    private FacadeGrpcPointBuilder facadeGrpcPointBuilder;

    @Override
    public FacadePointBO selectById(Long id) {
        GrpcPointQuery request = GrpcPointQuery.newBuilder().setPointId(id).build();
        GrpcRPointDTO response = pointApiBlockingStub.selectById(request);
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "selectById");
            return null;
        }
        return facadeGrpcPointBuilder.toFacadeBO(response.getData());
    }

    @Override
    public FacadePage<FacadePointBO> selectByPage(FacadePointQuery query) {
        GrpcPagePointQuery request = facadeGrpcPointBuilder.toGrpcPageQuery(query);
        GrpcRPagePointDTO response = pointApiBlockingStub.selectByPage(request);
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "selectByPage");
            return FacadePage.empty();
        }

        GrpcPagePointDTO pageDTO = response.getData();
        List<FacadePointBO> records = pageDTO.getDataList().stream()
                .map(facadeGrpcPointBuilder::toFacadeBO)
                .toList();

        return new FacadePage<>(
                pageDTO.getPage().getCurrent(),
                pageDTO.getPage().getSize(),
                pageDTO.getPage().getTotal(),
                pageDTO.getPage().getPages(),
                records);
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ResponseEnum.NO_RESOURCE.getCode().equals(code)) {
            log.debug("PointGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("PointFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }
}

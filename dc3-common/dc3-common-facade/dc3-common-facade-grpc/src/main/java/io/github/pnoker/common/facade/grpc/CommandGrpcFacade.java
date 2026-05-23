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

import io.github.pnoker.api.center.manager.CommandApiGrpc;
import io.github.pnoker.api.center.manager.GrpcCommandIdsQuery;
import io.github.pnoker.api.center.manager.GrpcCommandQuery;
import io.github.pnoker.api.center.manager.GrpcPageCommandDTO;
import io.github.pnoker.api.center.manager.GrpcPageCommandQuery;
import io.github.pnoker.api.center.manager.GrpcRCommandDTO;
import io.github.pnoker.api.center.manager.GrpcRCommandListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageCommandDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeCommandQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcCommandBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * gRPC CommandFacade: forwards to Manager Center via
 * {@link CommandApiGrpc.CommandApiBlockingStub}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandGrpcFacade implements CommandFacade {

    private final CommandApiGrpc.CommandApiBlockingStub commandApiBlockingStub;

    private final FacadeGrpcCommandBuilder facadeGrpcCommandBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeCommandBO getById(Long id) {
        GrpcCommandQuery request = GrpcCommandQuery.newBuilder().setCommandId(id).build();
        GrpcRCommandDTO response = grpcFacadeSupport.call("CommandFacade.getById", commandApiBlockingStub,
                stub -> stub.getById(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getById");
            return null;
        }
        return facadeGrpcCommandBuilder.toFacadeBO(response.getData());
    }

    @Override
    public List<FacadeCommandBO> listByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> commandIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (commandIds.isEmpty()) {
            return Collections.emptyList();
        }

        GrpcCommandIdsQuery request = GrpcCommandIdsQuery.newBuilder().addAllCommandIds(commandIds).build();
        GrpcRCommandListDTO response = grpcFacadeSupport.call("CommandFacade.listByIds", commandApiBlockingStub,
                stub -> stub.listByIds(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByIds");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcCommandBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeCommandBO> listByPage(FacadeCommandQuery query) {
        GrpcPageCommandQuery request = facadeGrpcCommandBuilder.toGrpcPageQuery(query);
        GrpcRPageCommandDTO response = grpcFacadeSupport.call("CommandFacade.listByPage", commandApiBlockingStub,
                stub -> stub.listByPage(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByPage");
            return FacadePage.empty();
        }

        GrpcPageCommandDTO pageDTO = response.getData();
        List<FacadeCommandBO> records = pageDTO.getDataList().stream().map(facadeGrpcCommandBuilder::toFacadeBO).toList();

        return new FacadePage<>(pageDTO.getPage().getCurrent(), pageDTO.getPage().getSize(),
                pageDTO.getPage().getTotal(), pageDTO.getPage().getPages(), records);
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ResponseEnum.NO_RESOURCE.getCode().equals(code)) {
            log.debug("CommandGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("CommandFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

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

import io.github.pnoker.api.center.manager.EventApiGrpc;
import io.github.pnoker.api.center.manager.GrpcEventIdsQuery;
import io.github.pnoker.api.center.manager.GrpcEventQuery;
import io.github.pnoker.api.center.manager.GrpcPageEventDTO;
import io.github.pnoker.api.center.manager.GrpcPageEventQuery;
import io.github.pnoker.api.center.manager.GrpcREventDTO;
import io.github.pnoker.api.center.manager.GrpcREventListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageEventDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeEventQuery;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcEventBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * gRPC EventFacade: forwards to Manager Center via
 * {@link EventApiGrpc.EventApiBlockingStub}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventGrpcFacade implements EventFacade {

    private final EventApiGrpc.EventApiBlockingStub eventApiBlockingStub;

    private final FacadeGrpcEventBuilder facadeGrpcEventBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeEventBO getById(Long tenantId, Long id) {
        GrpcEventQuery request = GrpcEventQuery.newBuilder().setEventId(id).setTenantId(tenantId).build();
        GrpcREventDTO response = grpcFacadeSupport.call("EventFacade.getById", eventApiBlockingStub,
                stub -> stub.getById(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getById");
            return null;
        }
        return facadeGrpcEventBuilder.toFacadeBO(response.getData());
    }

    @Override
    public List<FacadeEventBO> listByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> eventIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        GrpcEventIdsQuery request = GrpcEventIdsQuery.newBuilder().addAllEventIds(eventIds).setTenantId(tenantId).build();
        GrpcREventListDTO response = grpcFacadeSupport.call("EventFacade.listByIds", eventApiBlockingStub,
                stub -> stub.listByIds(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByIds");
            return Collections.emptyList();
        }
        return response.getDataList().stream().map(facadeGrpcEventBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeEventBO> listByPage(FacadeEventQuery query) {
        GrpcPageEventQuery request = facadeGrpcEventBuilder.toGrpcPageQuery(query);
        GrpcRPageEventDTO response = grpcFacadeSupport.call("EventFacade.listByPage", eventApiBlockingStub,
                stub -> stub.listByPage(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "listByPage");
            return FacadePage.empty();
        }

        GrpcPageEventDTO pageDTO = response.getData();
        List<FacadeEventBO> records = pageDTO.getDataList().stream().map(facadeGrpcEventBuilder::toFacadeBO).toList();

        return new FacadePage<>(pageDTO.getPage().getCurrent(), pageDTO.getPage().getSize(),
                pageDTO.getPage().getTotal(), pageDTO.getPage().getPages(), records);
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ErrorCode.NOT_FOUND.getCode().equals(code)) {
            log.debug("EventGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("EventFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

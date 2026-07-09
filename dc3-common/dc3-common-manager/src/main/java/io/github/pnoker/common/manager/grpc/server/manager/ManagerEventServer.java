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

package io.github.pnoker.common.manager.grpc.server.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.EventApiGrpc;
import io.github.pnoker.api.center.manager.GrpcEventIdsQuery;
import io.github.pnoker.api.center.manager.GrpcEventQuery;
import io.github.pnoker.api.center.manager.GrpcPageEventDTO;
import io.github.pnoker.api.center.manager.GrpcPageEventQuery;
import io.github.pnoker.api.center.manager.GrpcREventDTO;
import io.github.pnoker.api.center.manager.GrpcREventListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageEventDTO;
import io.github.pnoker.api.common.GrpcEventDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.query.EventQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcEventBuilder;
import io.github.pnoker.common.manager.service.EventService;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * gRPC server handling manager event facade requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerEventServer extends EventApiGrpc.EventApiImplBase {

    private final GrpcEventBuilder grpcEventBuilder;

    private final EventService eventService;

    @Override
    public void listByPage(GrpcPageEventQuery request, StreamObserver<GrpcRPageEventDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRPageEventDTO.Builder builder = GrpcRPageEventDTO.newBuilder();
            GrpcR result;

            EventQuery query = grpcEventBuilder.buildQueryByGrpcQuery(request);

            Page<EventBO> entityPage = eventService.list(query);
            if (Objects.isNull(entityPage)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                GrpcPageEventDTO.Builder pageEventBuilder = GrpcPageEventDTO.newBuilder();
                GrpcPage.Builder page = GrpcPage.newBuilder();
                page.setCurrent(entityPage.getCurrent());
                page.setSize(entityPage.getSize());
                page.setPages(entityPage.getPages());
                page.setTotal(entityPage.getTotal());
                pageEventBuilder.setPage(page);

                List<GrpcEventDTO> entityGrpcDTOList = entityPage.getRecords()
                        .stream()
                        .map(grpcEventBuilder::buildGrpcDTOByBO)
                        .toList();
                pageEventBuilder.addAllData(entityGrpcDTOList);

                builder.setData(pageEventBuilder);
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void listByIds(GrpcEventIdsQuery request, StreamObserver<GrpcREventListDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcREventListDTO.Builder builder = GrpcREventListDTO.newBuilder();
            GrpcR result;

            List<EventBO> entityBOList = eventService.listByIds(new HashSet<>(request.getEventIdsList()));
            if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                List<GrpcEventDTO> entityGrpcDTOList = entityBOList.stream()
                        .map(grpcEventBuilder::buildGrpcDTOByBO)
                        .toList();

                builder.addAllData(entityGrpcDTOList);
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getById(GrpcEventQuery request, StreamObserver<GrpcREventDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcREventDTO.Builder builder = GrpcREventDTO.newBuilder();
            GrpcR result;

            EventBO entityBO;
            try {
                entityBO = eventService.getById(request.getEventId());
            } catch (NotFoundException ignored) {
                entityBO = null;
            }
            if (Objects.isNull(entityBO)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                builder.setData(grpcEventBuilder.buildGrpcDTOByBO(entityBO));
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

}

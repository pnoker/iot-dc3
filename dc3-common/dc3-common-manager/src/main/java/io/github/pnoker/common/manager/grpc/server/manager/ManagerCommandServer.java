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
import io.github.pnoker.api.center.manager.CommandApiGrpc;
import io.github.pnoker.api.center.manager.GrpcCommandIdsQuery;
import io.github.pnoker.api.center.manager.GrpcCommandQuery;
import io.github.pnoker.api.center.manager.GrpcPageCommandDTO;
import io.github.pnoker.api.center.manager.GrpcPageCommandQuery;
import io.github.pnoker.api.center.manager.GrpcRCommandDTO;
import io.github.pnoker.api.center.manager.GrpcRCommandListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageCommandDTO;
import io.github.pnoker.api.common.GrpcCommandDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.query.CommandQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcCommandBuilder;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * gRPC server handling manager command facade requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerCommandServer extends CommandApiGrpc.CommandApiImplBase {

    private final GrpcCommandBuilder grpcCommandBuilder;

    private final CommandService commandService;

    @Override
    public void listByPage(GrpcPageCommandQuery request, StreamObserver<GrpcRPageCommandDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRPageCommandDTO.Builder builder = GrpcRPageCommandDTO.newBuilder();
            GrpcR result;

            CommandQuery query = grpcCommandBuilder.buildQueryByGrpcQuery(request);

            Page<CommandBO> entityPage = commandService.list(query);
            if (Objects.isNull(entityPage)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                GrpcPageCommandDTO.Builder pageCommandBuilder = GrpcPageCommandDTO.newBuilder();
                GrpcPage.Builder page = GrpcPage.newBuilder();
                page.setCurrent(entityPage.getCurrent());
                page.setSize(entityPage.getSize());
                page.setPages(entityPage.getPages());
                page.setTotal(entityPage.getTotal());
                pageCommandBuilder.setPage(page);

                List<GrpcCommandDTO> entityGrpcDTOList = entityPage.getRecords()
                        .stream()
                        .map(grpcCommandBuilder::buildGrpcDTOByBO)
                        .toList();
                pageCommandBuilder.addAllData(entityGrpcDTOList);

                builder.setData(pageCommandBuilder);
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void listByIds(GrpcCommandIdsQuery request, StreamObserver<GrpcRCommandListDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRCommandListDTO.Builder builder = GrpcRCommandListDTO.newBuilder();
            GrpcR result;

            List<CommandBO> entityBOList = commandService.listByIds(new HashSet<>(request.getCommandIdsList()));
            if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                List<GrpcCommandDTO> entityGrpcDTOList = entityBOList.stream()
                        .map(grpcCommandBuilder::buildGrpcDTOByBO)
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
    public void getById(GrpcCommandQuery request, StreamObserver<GrpcRCommandDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRCommandDTO.Builder builder = GrpcRCommandDTO.newBuilder();
            GrpcR result;

            CommandBO entityBO;
            try {
                entityBO = commandService.getById(request.getCommandId());
            } catch (NotFoundException ignored) {
                entityBO = null;
            }
            if (Objects.isNull(entityBO)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                builder.setData(grpcCommandBuilder.buildGrpcDTOByBO(entityBO));
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

}

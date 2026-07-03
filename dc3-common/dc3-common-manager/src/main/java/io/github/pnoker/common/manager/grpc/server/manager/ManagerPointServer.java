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
import io.github.pnoker.api.center.manager.GrpcPagePointDTO;
import io.github.pnoker.api.center.manager.GrpcPagePointQuery;
import io.github.pnoker.api.center.manager.GrpcPointIdsQuery;
import io.github.pnoker.api.center.manager.GrpcPointQuery;
import io.github.pnoker.api.center.manager.GrpcRPagePointDTO;
import io.github.pnoker.api.center.manager.GrpcRPointDTO;
import io.github.pnoker.api.center.manager.GrpcRPointListDTO;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointBuilder;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * gRPC server handling manager point facade requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerPointServer extends PointApiGrpc.PointApiImplBase {

    private final GrpcPointBuilder grpcPointBuilder;

    private final PointService pointService;

    @Override
    public void listByPage(GrpcPagePointQuery request, StreamObserver<GrpcRPagePointDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRPagePointDTO.Builder builder = GrpcRPagePointDTO.newBuilder();
            GrpcR result;

            PointQuery query = grpcPointBuilder.buildQueryByGrpcQuery(request);

            Page<PointBO> entityPage = pointService.list(query);
            if (Objects.isNull(entityPage)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                GrpcPagePointDTO.Builder pagePointBuilder = GrpcPagePointDTO.newBuilder();
                GrpcPage.Builder page = GrpcPage.newBuilder();
                page.setCurrent(entityPage.getCurrent());
                page.setSize(entityPage.getSize());
                page.setPages(entityPage.getPages());
                page.setTotal(entityPage.getTotal());
                pagePointBuilder.setPage(page);

                List<GrpcPointDTO> entityGrpcDTOList = entityPage.getRecords()
                        .stream()
                        .map(grpcPointBuilder::buildGrpcDTOByBO)
                        .toList();
                pagePointBuilder.addAllData(entityGrpcDTOList);

                builder.setData(pagePointBuilder);
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void listByIds(GrpcPointIdsQuery request, StreamObserver<GrpcRPointListDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRPointListDTO.Builder builder = GrpcRPointListDTO.newBuilder();
            GrpcR result;

            List<PointBO> entityBOList = pointService.listByIds(new HashSet<>(request.getPointIdsList()));
            if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                List<GrpcPointDTO> entityGrpcDTOList = entityBOList.stream()
                        .map(grpcPointBuilder::buildGrpcDTOByBO)
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
    public void getById(GrpcPointQuery request, StreamObserver<GrpcRPointDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRPointDTO.Builder builder = GrpcRPointDTO.newBuilder();
            GrpcR result;

            PointBO entityBO;
            try {
                entityBO = pointService.getById(request.getPointId());
            } catch (NotFoundException e) {
                entityBO = null;
            }
            if (Objects.isNull(entityBO)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();

                builder.setData(grpcPointBuilder.buildGrpcDTOByBO(entityBO));
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

}

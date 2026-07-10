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
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcPageProfileDTO;
import io.github.pnoker.api.center.manager.GrpcPageProfileQuery;
import io.github.pnoker.api.center.manager.GrpcProfileIdsQuery;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.center.manager.GrpcRPageProfileDTO;
import io.github.pnoker.api.center.manager.GrpcRProfileDTO;
import io.github.pnoker.api.center.manager.GrpcRProfileListDTO;
import io.github.pnoker.api.center.manager.ProfileApiGrpc;
import io.github.pnoker.api.common.GrpcProfileDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.query.ProfileQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcProfileBuilder;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Profile gRPC API.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerProfileServer extends ProfileApiGrpc.ProfileApiImplBase {

    private final GrpcProfileBuilder grpcProfileBuilder;

    private final ProfileService profileService;

    @Override
    public void listByPage(GrpcPageProfileQuery request, StreamObserver<GrpcRPageProfileDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRPageProfileDTO.Builder builder = GrpcRPageProfileDTO.newBuilder();
            GrpcR result;

            ProfileQuery query = grpcProfileBuilder.buildQueryByGrpcQuery(request);
            Page<ProfileBO> entityPage = profileService.list(query);
            if (Objects.isNull(entityPage)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();
                GrpcPageProfileDTO.Builder pageBuilder = GrpcPageProfileDTO.newBuilder();
                pageBuilder.setPage(grpcProfileBuilder.buildGrpcPage(entityPage));
                List<GrpcProfileDTO> profiles = entityPage.getRecords().stream()
                        .map(grpcProfileBuilder::buildGrpcDTOByBO)
                        .toList();
                pageBuilder.addAllData(profiles);
                builder.setData(pageBuilder);
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getByProfileId(GrpcProfileQuery request, StreamObserver<GrpcRProfileDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRProfileDTO.Builder builder = GrpcRProfileDTO.newBuilder();
            GrpcR result;

            ProfileBO profile;
            try {
                profile = profileService.getById(request.getProfileId());
            } catch (NotFoundException ignored) {
                profile = null;
            }
            if (Objects.isNull(profile)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();
                builder.setData(grpcProfileBuilder.buildGrpcDTOByBO(profile));
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void listByProfileIds(GrpcProfileIdsQuery request,
                                 StreamObserver<GrpcRProfileListDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRProfileListDTO.Builder builder = GrpcRProfileListDTO.newBuilder();
            GrpcR result;

            List<ProfileBO> profiles = profileService.listByIds(new HashSet<>(request.getProfileIdsList()));
            if (CollectionUtils.isEmpty(profiles)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();
                builder.addAllData(profiles.stream().map(grpcProfileBuilder::buildGrpcDTOByBO).toList());
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void listByDeviceId(GrpcDeviceQuery request, StreamObserver<GrpcRProfileListDTO> responseObserver) {
        TenantContextHolder.setTenantId(request.getTenantId());
        try {
            GrpcRProfileListDTO.Builder builder = GrpcRProfileListDTO.newBuilder();
            GrpcR result;

            List<ProfileBO> profiles = profileService.listByDeviceId(request.getDeviceId());
            if (CollectionUtils.isEmpty(profiles)) {
                result = GrpcRFactory.notFound();
            } else {
                result = GrpcRFactory.ok();
                builder.addAllData(profiles.stream().map(grpcProfileBuilder::buildGrpcDTOByBO).toList());
            }

            builder.setResult(result);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } finally {
            TenantContextHolder.clear();
        }
    }

}

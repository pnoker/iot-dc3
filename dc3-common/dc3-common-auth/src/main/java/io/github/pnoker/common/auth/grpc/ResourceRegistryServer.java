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

package io.github.pnoker.common.auth.grpc;

import io.github.pnoker.api.center.auth.GrpcRSyncResult;
import io.github.pnoker.api.center.auth.GrpcScannedApiDTO;
import io.github.pnoker.api.center.auth.GrpcSyncRequest;
import io.github.pnoker.api.center.auth.GrpcSyncResultDTO;
import io.github.pnoker.api.center.auth.ResourceRegistryApiGrpc;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.auth.biz.ResourceRegistrySyncService;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistryScannedApi;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncResult;
import io.github.pnoker.common.enums.ErrorCode;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * gRPC server handling resource registration requests.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceRegistryServer extends ResourceRegistryApiGrpc.ResourceRegistryApiImplBase {

    private final ResourceRegistrySyncService resourceRegistrySyncService;

    private static List<ResourceRegistryScannedApi> toScannedApis(List<GrpcScannedApiDTO> dtos) {
        List<ResourceRegistryScannedApi> apis = new ArrayList<>(dtos.size());
        for (GrpcScannedApiDTO dto : dtos) {
            apis.add(ResourceRegistryScannedApi.builder()
                    .method(dto.getMethod())
                    .path(dto.getPath())
                    .apiName(dto.getApiName())
                    .title(dto.getTitle())
                    .remark(dto.getRemark())
                    .apiGroup(dto.getApiGroup())
                    .riskLevel(dto.getRiskLevel())
                    .destructiveHint(dto.getDestructiveHint())
                    .openWorldHint(dto.getOpenWorldHint())
                    .idempotentHint(dto.getIdempotentHint())
                    .aiDescription(dto.getAiDescription())
                    .hidden(dto.getHidden())
                    .build());
        }
        return apis;
    }

    @Override
    public void sync(GrpcSyncRequest request, StreamObserver<GrpcRSyncResult> responseObserver) {
        GrpcRSyncResult.Builder builder = GrpcRSyncResult.newBuilder();
        try {
            ResourceRegistrySyncCommand command = ResourceRegistrySyncCommand.builder()
                    .serviceName(request.getServiceName())
                    .deleteMissing(request.getDeleteMissing())
                    .apis(toScannedApis(request.getApisList()))
                    .build();
            ResourceRegistrySyncResult result = resourceRegistrySyncService.sync(command);

            builder.setResult(GrpcRFactory.ok());
            builder.setData(GrpcSyncResultDTO.newBuilder()
                    .setInserted(result.getInserted())
                    .setUpdated(result.getUpdated())
                    .setDeleted(result.getDeleted())
                    .setUnchanged(result.getUnchanged())
                    .build());
        } catch (Exception e) {
            log.error("Resource registry sync failed for service [{}]", request.getServiceName(), e);
            builder.setResult(Objects.nonNull(e.getMessage())
                    ? GrpcRFactory.fail(ErrorCode.FAILURE, e.getMessage())
                    : GrpcRFactory.fail(ErrorCode.FAILURE));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}

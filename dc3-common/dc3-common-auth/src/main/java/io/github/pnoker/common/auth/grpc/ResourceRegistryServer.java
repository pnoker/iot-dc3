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

import io.github.pnoker.api.center.auth.GrpcScannedApiDTO;
import io.github.pnoker.api.center.auth.GrpcSyncRequest;
import io.github.pnoker.api.center.auth.GrpcSyncResultDTO;
import io.github.pnoker.api.center.auth.ResourceRegistryApiGrpc;
import io.github.pnoker.common.auth.biz.ReactiveResourceRegistrySyncService;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistryScannedApi;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * gRPC server handling resource registration requests.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceRegistryServer extends ResourceRegistryApiGrpc.ResourceRegistryApiImplBase {

    private final ReactiveResourceRegistrySyncService resourceRegistrySyncService;

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
                    .build());
        }
        return apis;
    }

    @Override
    public void sync(GrpcSyncRequest request, StreamObserver<GrpcSyncResultDTO> responseObserver) {
        try {
            ResourceRegistrySyncCommand command = ResourceRegistrySyncCommand.builder()
                    .serviceName(request.getServiceName())
                    .deleteMissing(request.getDeleteMissing())
                    .apis(toScannedApis(request.getApisList()))
                    .build();
            resourceRegistrySyncService
                    .sync(command)
                    .subscribe(
                            result -> {
                                responseObserver.onNext(GrpcSyncResultDTO.newBuilder()
                                        .setInserted(result.getInserted())
                                        .setUpdated(result.getUpdated())
                                        .setDeleted(result.getDeleted())
                                        .setUnchanged(result.getUnchanged())
                                        .build());
                                responseObserver.onCompleted();
                            },
                            error -> {
                                log.error(
                                        "Resource registry synchronization failed, serviceName={}",
                                        request.getServiceName(),
                                        error);
                                responseObserver.onError(status(error)
                                        .withDescription(error.getMessage())
                                        .withCause(error)
                                        .asRuntimeException());
                            });
            return;
        } catch (Exception e) {
            log.error("Resource registry synchronization failed, serviceName={}", request.getServiceName(), e);
            responseObserver.onError(
                    status(e).withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    private static Status status(Throwable error) {
        if (error instanceof TimeoutException) return Status.DEADLINE_EXCEEDED;
        if (error instanceof IllegalArgumentException) return Status.INVALID_ARGUMENT;
        return Status.INTERNAL;
    }
}

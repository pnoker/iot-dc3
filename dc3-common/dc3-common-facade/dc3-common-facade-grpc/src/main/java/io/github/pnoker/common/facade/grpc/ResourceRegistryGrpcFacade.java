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

import io.github.pnoker.api.center.auth.GrpcRSyncResult;
import io.github.pnoker.api.center.auth.GrpcScannedApiDTO;
import io.github.pnoker.api.center.auth.GrpcSyncRequest;
import io.github.pnoker.api.center.auth.ResourceRegistryApiGrpc;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.ResourceRegistryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeResourceRegistrySyncResultBO;
import io.github.pnoker.common.facade.entity.bo.FacadeScannedApiBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * gRPC {@link ResourceRegistryFacade}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceRegistryGrpcFacade implements ResourceRegistryFacade {

    private final ResourceRegistryApiGrpc.ResourceRegistryApiBlockingStub resourceRegistryApiBlockingStub;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeResourceRegistrySyncResultBO sync(FacadeResourceRegistrySyncCommandBO command) {
        GrpcSyncRequest.Builder request = GrpcSyncRequest.newBuilder()
                .setServiceName(Objects.requireNonNullElse(command.getServiceName(), ""))
                .setDeleteMissing(command.isDeleteMissing());
        List<FacadeScannedApiBO> apis = command.getApis();
        if (Objects.nonNull(apis)) {
            for (FacadeScannedApiBO api : apis) {
                request.addApis(GrpcScannedApiDTO.newBuilder()
                        .setMethod(Objects.requireNonNullElse(api.getMethod(), ""))
                        .setPath(Objects.requireNonNullElse(api.getPath(), ""))
                        .setApiName(Objects.requireNonNullElse(api.getApiName(), ""))
                        .setTitle(Objects.requireNonNullElse(api.getTitle(), ""))
                        .setRemark(Objects.requireNonNullElse(api.getRemark(), ""))
                        .setApiGroup(Objects.requireNonNullElse(api.getApiGroup(), ""))
                        .build());
            }
        }
        GrpcSyncRequest syncRequest = request.build();
        GrpcRSyncResult response = grpcFacadeSupport.call("ResourceRegistryFacade.sync", resourceRegistryApiBlockingStub,
                stub -> stub.sync(syncRequest));
        if (!response.getResult().getOk()) {
            throw new ServiceException("ResourceRegistryFacade.sync failed: [" + response.getResult().getCode() + "] "
                    + response.getResult().getMessage());
        }
        return FacadeResourceRegistrySyncResultBO.builder()
                .inserted(response.getData().getInserted())
                .updated(response.getData().getUpdated())
                .deleted(response.getData().getDeleted())
                .unchanged(response.getData().getUnchanged())
                .build();
    }

}

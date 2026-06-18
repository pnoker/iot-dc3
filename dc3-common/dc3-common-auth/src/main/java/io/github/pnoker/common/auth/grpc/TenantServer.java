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

import io.github.pnoker.api.center.auth.GrpcCodeQuery;
import io.github.pnoker.api.center.auth.GrpcRTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcTenantBuilder;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.enums.ErrorCode;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * gRPC server handling tenant facade requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServer extends TenantApiGrpc.TenantApiImplBase {

    private final GrpcTenantBuilder grpcTenantBuilder;

    private final TenantService tenantService;

    @Override
    public void getByCode(GrpcCodeQuery request, StreamObserver<GrpcRTenantDTO> responseObserver) {
        GrpcRTenantDTO.Builder builder = GrpcRTenantDTO.newBuilder();

        try {
            TenantBO entityBO = tenantService.getByCode(request.getCode());
            if (Objects.isNull(entityBO)) {
                builder.setResult(GrpcRFactory.notFound());
            } else {
                builder.setResult(GrpcRFactory.ok());
                builder.setData(grpcTenantBuilder.buildGrpcDTOByBO(entityBO));
            }
        } catch (Exception e) {
            log.warn("getByCode failed", e);
            builder.setResult(GrpcRFactory.fail(ErrorCode.FAILURE));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}

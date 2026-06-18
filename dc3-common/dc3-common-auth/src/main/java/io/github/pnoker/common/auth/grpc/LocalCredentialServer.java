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

import io.github.pnoker.api.center.auth.GrpcLoginNameQuery;
import io.github.pnoker.api.center.auth.GrpcRLocalCredentialDTO;
import io.github.pnoker.api.center.auth.LocalCredentialApiGrpc;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.grpc.builder.GrpcLocalCredentialBuilder;
import io.github.pnoker.common.auth.service.LocalCredentialService;
import io.github.pnoker.common.enums.ErrorCode;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * gRPC server handling local credential facade requests.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCredentialServer extends LocalCredentialApiGrpc.LocalCredentialApiImplBase {

    private final GrpcLocalCredentialBuilder grpcLocalCredentialBuilder;

    private final LocalCredentialService localCredentialService;

    @Override
    public void getByLoginName(GrpcLoginNameQuery request, StreamObserver<GrpcRLocalCredentialDTO> responseObserver) {
        GrpcRLocalCredentialDTO.Builder builder = GrpcRLocalCredentialDTO.newBuilder();

        try {
            LocalCredentialBO entityBO = localCredentialService.getByLoginName(request.getLoginName(), false);
            if (Objects.isNull(entityBO)) {
                builder.setResult(GrpcRFactory.notFound());
            } else {
                builder.setResult(GrpcRFactory.ok());
                builder.setData(grpcLocalCredentialBuilder.buildGrpcDTOByBO(entityBO));
            }
        } catch (Exception e) {
            log.warn("getByLoginName failed", e);
            builder.setResult(GrpcRFactory.fail(ErrorCode.FAILURE));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}

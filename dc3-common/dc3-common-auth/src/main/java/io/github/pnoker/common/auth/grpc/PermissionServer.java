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

import io.github.pnoker.api.center.auth.GrpcPermissionQuery;
import io.github.pnoker.api.center.auth.GrpcRPermissionCodesDTO;
import io.github.pnoker.api.center.auth.PermissionApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * gRPC server handling permission-code lookup requests.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServer extends PermissionApiGrpc.PermissionApiImplBase {

    private final RoleResourceBindService roleResourceBindService;

    @Override
    public void listPermissionCodes(GrpcPermissionQuery request,
                                    StreamObserver<GrpcRPermissionCodesDTO> responseObserver) {
        GrpcRPermissionCodesDTO.Builder builder = GrpcRPermissionCodesDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        try {
            roleResourceBindService.listResourceByPrincipalId(request.getPrincipalId(), request.getTenantId())
                    .stream()
                    .map(ResourceBO::getResourceCode)
                    .filter(Objects::nonNull)
                    .filter(code -> !code.isBlank())
                    .forEach(builder::addCodes);

            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getRemark());
        } catch (Exception e) {
            log.warn("listPermissionCodes failed, tenant={}, principal={}",
                    request.getTenantId(), request.getPrincipalId(), e);
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.FAILURE.getCode());
            rBuilder.setMessage(ResponseEnum.FAILURE.getRemark());
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}

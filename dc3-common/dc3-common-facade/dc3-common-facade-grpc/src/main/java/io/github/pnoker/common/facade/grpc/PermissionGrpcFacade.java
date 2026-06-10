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

import io.github.pnoker.api.center.auth.GrpcPermissionQuery;
import io.github.pnoker.api.center.auth.GrpcRPermissionCodesDTO;
import io.github.pnoker.api.center.auth.PermissionApiGrpc;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.PermissionFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * gRPC {@link PermissionFacade}.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionGrpcFacade implements PermissionFacade {

    private final PermissionApiGrpc.PermissionApiBlockingStub permissionApiBlockingStub;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public Set<String> listPermissionCodes(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return Set.of();
        }
        GrpcPermissionQuery request = GrpcPermissionQuery.newBuilder()
                .setTenantId(tenantId)
                .setUserId(userId)
                .build();
        GrpcRPermissionCodesDTO response = grpcFacadeSupport.call("PermissionFacade.listPermissionCodes",
                permissionApiBlockingStub, stub -> stub.listPermissionCodes(request));
        if (!response.getResult().getOk()) {
            throw new ServiceException("PermissionFacade.listPermissionCodes failed: ["
                    + response.getResult().getCode() + "] " + response.getResult().getMessage());
        }
        return response.getCodesList()
                .stream()
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
    }

}

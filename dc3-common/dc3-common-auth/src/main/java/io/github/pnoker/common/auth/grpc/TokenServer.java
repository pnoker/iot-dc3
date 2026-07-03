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

import io.github.pnoker.api.center.auth.GrpcLoginQuery;
import io.github.pnoker.api.center.auth.GrpcRTokenDTO;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.api.common.GrpcRFactory;
import io.github.pnoker.common.auth.biz.TokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.tenant.TenantContextHolder;
import io.github.pnoker.common.utils.TimeUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * gRPC server handling token facade requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServer extends TokenApiGrpc.TokenApiImplBase {

    private final TokenService tokenService;

    @Override
    public void checkValid(GrpcLoginQuery request, StreamObserver<GrpcRTokenDTO> responseObserver) {
        GrpcRTokenDTO.Builder builder = GrpcRTokenDTO.newBuilder();

        try {
            // Login path: validate the principal before a tenant context is bound to this
            // thread. checkValid resolves tenant, credential, and tenant-membership; the
            // membership lookup reads dc3_tenant_membership (tenant_id-bearing, not
            // whitelisted), so run it with tenant filtering disabled.
            TokenValid entity = TenantContextHolder.runIgnore(() -> tokenService.checkValid(request.getName(),
                    request.getSalt(), request.getToken(), request.getTenant()));
            if (Objects.isNull(entity)) {
                builder.setResult(GrpcRFactory.notFound());
            } else if (!entity.isValid()) {
                builder.setResult(GrpcRFactory.fail(ErrorCode.TOKEN_INVALID));
            } else {
                builder.setResult(GrpcRFactory.ok());
                builder.setData(TimeUtil.completeFormat(entity.getExpireTime()));
            }
        } catch (Exception e) {
            log.warn("checkValid failed", e);
            builder.setResult(GrpcRFactory.fail(ErrorCode.FAILURE));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}

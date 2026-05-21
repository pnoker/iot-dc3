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

import io.github.pnoker.api.center.auth.GrpcNameQuery;
import io.github.pnoker.api.center.auth.GrpcRUserLoginDTO;
import io.github.pnoker.api.center.auth.UserLoginApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.UserLoginFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeUserLoginBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcUserLoginBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC {@link UserLoginFacade}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLoginGrpcFacade implements UserLoginFacade {

    private final UserLoginApiGrpc.UserLoginApiBlockingStub userLoginApiBlockingStub;

    private final FacadeGrpcUserLoginBuilder facadeGrpcUserLoginBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeUserLoginBO getByName(String name) {
        GrpcNameQuery request = GrpcNameQuery.newBuilder().setName(name).build();
        GrpcRUserLoginDTO response = grpcFacadeSupport.call("UserLoginFacade.getByName", userLoginApiBlockingStub,
                stub -> stub.getByName(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getByName");
            return null;
        }
        return facadeGrpcUserLoginBuilder.toFacadeBO(response.getData());
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ResponseEnum.NO_RESOURCE.getCode().equals(code)) {
            log.debug("UserLoginGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("UserLoginFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

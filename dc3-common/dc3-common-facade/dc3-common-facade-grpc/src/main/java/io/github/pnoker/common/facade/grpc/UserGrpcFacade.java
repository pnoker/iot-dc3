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

import io.github.pnoker.api.center.auth.GrpcIdQuery;
import io.github.pnoker.api.center.auth.GrpcRUserDTO;
import io.github.pnoker.api.center.auth.UserApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcUserBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC {@link UserFacade}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserGrpcFacade implements UserFacade {

    private final UserApiGrpc.UserApiBlockingStub userApiBlockingStub;

    private final FacadeGrpcUserBuilder facadeGrpcUserBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeUserBO getById(Long id) {
        GrpcIdQuery request = GrpcIdQuery.newBuilder().setId(id).build();
        GrpcRUserDTO response = grpcFacadeSupport.call("UserFacade.getById", userApiBlockingStub,
                stub -> stub.getById(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getById");
            return null;
        }
        return facadeGrpcUserBuilder.toFacadeBO(response.getData());
    }

    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ResponseEnum.NO_RESOURCE.getCode().equals(code)) {
            log.debug("UserGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("UserFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

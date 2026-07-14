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

import io.github.pnoker.api.center.auth.GrpcLoginNameQuery;
import io.github.pnoker.api.center.auth.GrpcRLocalCredentialDTO;
import io.github.pnoker.api.center.auth.LocalCredentialApiGrpc;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.LocalCredentialFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcLocalCredentialBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC {@link LocalCredentialFacade}.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalCredentialGrpcFacade implements LocalCredentialFacade {

    private final LocalCredentialApiGrpc.LocalCredentialApiBlockingStub localCredentialApiBlockingStub;

    private final FacadeGrpcLocalCredentialBuilder facadeGrpcLocalCredentialBuilder;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public FacadeLocalCredentialBO getByLoginName(String loginName) {
        GrpcLoginNameQuery request = GrpcLoginNameQuery.newBuilder().setLoginName(loginName).build();
        GrpcRLocalCredentialDTO response = grpcFacadeSupport.call("LocalCredentialFacade.getByLoginName",
                localCredentialApiBlockingStub, stub -> stub.getByLoginName(request));
        if (!response.getResult().getOk()) {
            guardOrThrow(response.getResult(), "getByLoginName");
            return null;
        }
        return facadeGrpcLocalCredentialBuilder.toFacadeBO(response.getData());
    }

    /**
     * Guard a gRPC result: NOT_FOUND is treated as a normal empty outcome, any other
     * error code throws a service exception.
     *
     * @param result the gRPC result envelope
     * @param op     the operation name, for error messages
     */
    private void guardOrThrow(GrpcR result, String op) {
        String code = result.getCode();
        if (ErrorCode.NOT_FOUND.getCode().equals(code)) {
            log.debug("LocalCredentialGrpcFacade.{} => no resource", op);
            return;
        }
        throw new ServiceException("LocalCredentialFacade." + op + " failed: [" + code + "] " + result.getMessage());
    }

}

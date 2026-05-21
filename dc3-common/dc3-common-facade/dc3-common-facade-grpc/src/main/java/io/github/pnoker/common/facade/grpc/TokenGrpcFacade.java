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

import io.github.pnoker.api.center.auth.GrpcLoginQuery;
import io.github.pnoker.api.center.auth.GrpcRTokenDTO;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.common.facade.api.TokenFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC {@link TokenFacade}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenGrpcFacade implements TokenFacade {

    private final TokenApiGrpc.TokenApiBlockingStub tokenApiBlockingStub;

    private final GrpcFacadeSupport grpcFacadeSupport;

    @Override
    public boolean checkValid(String tenant, String name, String salt, String token) {
        GrpcLoginQuery login = GrpcLoginQuery.newBuilder()
                .setTenant(tenant)
                .setName(name)
                .setSalt(salt)
                .setToken(token)
                .build();
        GrpcRTokenDTO response = grpcFacadeSupport.call("TokenFacade.checkValid", tokenApiBlockingStub,
                stub -> stub.checkValid(login));
        return response.getResult().getOk();
    }

}

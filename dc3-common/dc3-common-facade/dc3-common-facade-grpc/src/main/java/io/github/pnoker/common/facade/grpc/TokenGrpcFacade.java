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
import io.github.pnoker.api.center.auth.GrpcTokenValidationDTO;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.common.facade.api.TokenFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** gRPC facade exposing token operations. */
@Component
@RequiredArgsConstructor
public class TokenGrpcFacade implements TokenFacade {
    private final TokenApiGrpc.TokenApiStub tokenApiStub;
    private final GrpcFacadeSupport support;

    @Override
    public Mono<Boolean> checkValid(String tenant, String name, String token) {
        GrpcLoginQuery request = GrpcLoginQuery.newBuilder()
                .setTenant(tenant)
                .setName(name)
                .setToken(token)
                .build();
        TokenApiGrpc.TokenApiStub stub = support.withDeadline(tokenApiStub);
        return ReactiveGrpcClientSupport.<GrpcLoginQuery, GrpcTokenValidationDTO>unary(
                        "TokenFacade.checkValid", observer -> stub.checkValid(request, observer))
                .map(GrpcTokenValidationDTO::getValid);
    }
}

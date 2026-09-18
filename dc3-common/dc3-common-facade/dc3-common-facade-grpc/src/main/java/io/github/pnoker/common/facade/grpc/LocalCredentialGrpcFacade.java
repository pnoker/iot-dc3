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

import io.github.pnoker.api.center.auth.GrpcLocalCredentialDTO;
import io.github.pnoker.api.center.auth.GrpcLoginNameQuery;
import io.github.pnoker.api.center.auth.LocalCredentialApiGrpc;
import io.github.pnoker.common.facade.api.LocalCredentialFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcLocalCredentialBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** gRPC facade exposing local credential operations. */
@Component
@RequiredArgsConstructor
public class LocalCredentialGrpcFacade implements LocalCredentialFacade {
    private final LocalCredentialApiGrpc.LocalCredentialApiStub credentialApiStub;
    private final FacadeGrpcLocalCredentialBuilder builder;
    private final GrpcFacadeSupport support;

    @Override
    public Mono<FacadeLocalCredentialBO> getByLoginName(Long tenantId, String loginName) {
        GrpcLoginNameQuery request = GrpcLoginNameQuery.newBuilder()
                .setTenantId(tenantId)
                .setLoginName(loginName)
                .build();
        LocalCredentialApiGrpc.LocalCredentialApiStub stub = support.withDeadline(credentialApiStub);
        return ReactiveGrpcClientSupport.<GrpcLoginNameQuery, GrpcLocalCredentialDTO>unary(
                        "LocalCredentialFacade.getByLoginName", observer -> stub.getByLoginName(request, observer))
                .map(builder::toFacadeBO);
    }
}

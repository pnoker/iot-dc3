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
import io.github.pnoker.api.center.auth.GrpcUserDTO;
import io.github.pnoker.api.center.auth.UserApiGrpc;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcUserBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserGrpcFacade implements UserFacade {
    private final UserApiGrpc.UserApiStub userApiStub;
    private final FacadeGrpcUserBuilder builder;
    private final GrpcFacadeSupport support;

    @Override
    public Mono<FacadeUserBO> getById(Long tenantId, Long id) {
        return call("getById", tenantId, id, false);
    }

    @Override
    public Mono<FacadeUserBO> getByPrincipalId(Long tenantId, Long principalId) {
        return call("getByPrincipalId", tenantId, principalId, true);
    }

    private Mono<FacadeUserBO> call(String operation, Long tenantId, Long id, boolean principal) {
        GrpcIdQuery request =
                GrpcIdQuery.newBuilder().setTenantId(tenantId).setId(id).build();
        UserApiGrpc.UserApiStub stub = support.withDeadline(userApiStub);
        return ReactiveGrpcClientSupport.<GrpcIdQuery, GrpcUserDTO>unary("UserFacade." + operation, observer -> {
                    if (principal) stub.getByPrincipalId(request, observer);
                    else stub.getById(request, observer);
                })
                .map(builder::toFacadeBO);
    }
}

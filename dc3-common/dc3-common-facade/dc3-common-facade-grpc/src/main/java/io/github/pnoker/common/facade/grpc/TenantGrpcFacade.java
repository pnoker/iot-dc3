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

import io.github.pnoker.api.center.auth.GrpcCodeQuery;
import io.github.pnoker.api.center.auth.GrpcTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcTenantBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** gRPC facade exposing tenant operations. */
@Component
@RequiredArgsConstructor
public class TenantGrpcFacade implements TenantFacade {
    private final TenantApiGrpc.TenantApiStub tenantApiStub;
    private final FacadeGrpcTenantBuilder builder;
    private final GrpcFacadeSupport support;

    @Override
    public Mono<FacadeTenantBO> getByCode(String code) {
        GrpcCodeQuery request = GrpcCodeQuery.newBuilder().setCode(code).build();
        TenantApiGrpc.TenantApiStub stub = support.withDeadline(tenantApiStub);
        return ReactiveGrpcClientSupport.<GrpcCodeQuery, GrpcTenantDTO>unary(
                        "TenantFacade.getByCode", observer -> stub.getByCode(request, observer))
                .map(builder::toFacadeBO);
    }
}

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

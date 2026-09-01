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

    @Override public Mono<FacadeUserBO> getById(Long tenantId, Long id) { return call("getById", tenantId, id, false); }
    @Override public Mono<FacadeUserBO> getByPrincipalId(Long tenantId, Long principalId) { return call("getByPrincipalId", tenantId, principalId, true); }

    private Mono<FacadeUserBO> call(String operation, Long tenantId, Long id, boolean principal) {
        GrpcIdQuery request = GrpcIdQuery.newBuilder().setTenantId(tenantId).setId(id).build();
        UserApiGrpc.UserApiStub stub = support.withDeadline(userApiStub);
        return ReactiveGrpcClientSupport.<GrpcIdQuery, GrpcUserDTO>unary("UserFacade." + operation,
                        observer -> { if (principal) stub.getByPrincipalId(request, observer); else stub.getById(request, observer); })
                .map(builder::toFacadeBO);
    }
}

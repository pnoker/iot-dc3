package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.auth.GrpcLoginNameQuery;
import io.github.pnoker.api.center.auth.GrpcLocalCredentialDTO;
import io.github.pnoker.api.center.auth.LocalCredentialApiGrpc;
import io.github.pnoker.common.facade.api.LocalCredentialFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.grpc.builder.FacadeGrpcLocalCredentialBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LocalCredentialGrpcFacade implements LocalCredentialFacade {
    private final LocalCredentialApiGrpc.LocalCredentialApiStub credentialApiStub;
    private final FacadeGrpcLocalCredentialBuilder builder;
    private final GrpcFacadeSupport support;

    @Override
    public Mono<FacadeLocalCredentialBO> getByLoginName(Long tenantId, String loginName) {
        GrpcLoginNameQuery request = GrpcLoginNameQuery.newBuilder().setTenantId(tenantId).setLoginName(loginName).build();
        LocalCredentialApiGrpc.LocalCredentialApiStub stub = support.withDeadline(credentialApiStub);
        return ReactiveGrpcClientSupport.<GrpcLoginNameQuery, GrpcLocalCredentialDTO>unary(
                        "LocalCredentialFacade.getByLoginName", observer -> stub.getByLoginName(request, observer))
                .map(builder::toFacadeBO);
    }
}

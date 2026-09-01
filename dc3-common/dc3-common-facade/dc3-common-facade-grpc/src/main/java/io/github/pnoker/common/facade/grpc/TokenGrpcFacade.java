package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.auth.GrpcLoginQuery;
import io.github.pnoker.api.center.auth.GrpcTokenValidationDTO;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.common.facade.api.TokenFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TokenGrpcFacade implements TokenFacade {
    private final TokenApiGrpc.TokenApiStub tokenApiStub;
    private final GrpcFacadeSupport support;

    @Override
    public Mono<Boolean> checkValid(String tenant, String name, String token) {
        GrpcLoginQuery request = GrpcLoginQuery.newBuilder().setTenant(tenant).setName(name).setToken(token).build();
        TokenApiGrpc.TokenApiStub stub = support.withDeadline(tokenApiStub);
        return ReactiveGrpcClientSupport.<GrpcLoginQuery, GrpcTokenValidationDTO>unary(
                        "TokenFacade.checkValid", observer -> stub.checkValid(request, observer))
                .map(GrpcTokenValidationDTO::getValid);
    }
}

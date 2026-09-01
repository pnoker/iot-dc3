package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.biz.ReactiveTokenService;
import io.github.pnoker.common.facade.api.TokenFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TokenLocalFacade implements TokenFacade {
    private final ReactiveTokenService tokenService;
    @Override public Mono<Boolean> checkValid(String tenant, String name, String token) {
        return tokenService.checkValid(name, token, tenant).map(value -> value != null && value.isValid());
    }
}

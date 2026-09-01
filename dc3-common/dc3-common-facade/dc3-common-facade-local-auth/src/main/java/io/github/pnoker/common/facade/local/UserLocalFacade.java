package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.local.builder.FacadeUserBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserLocalFacade implements UserFacade {
    private final ReactiveUserService userService;
    private final FacadeUserBuilder builder;
    @Override public Mono<FacadeUserBO> getById(Long tenantId, Long id) {
        return userService.getById(tenantId, id).map(builder::toFacadeBO).onErrorResume(NotFoundException.class, error -> Mono.empty());
    }
    @Override public Mono<FacadeUserBO> getByPrincipalId(Long tenantId, Long principalId) {
        return userService.getByPrincipalId(tenantId, principalId).map(builder::toFacadeBO).onErrorResume(NotFoundException.class, error -> Mono.empty());
    }
}

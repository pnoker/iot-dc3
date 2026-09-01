package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.common.facade.api.LocalCredentialFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.local.builder.FacadeLocalCredentialBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LocalCredentialLocalFacade implements LocalCredentialFacade {
    private final ReactiveLocalCredentialService credentialService;
    private final FacadeLocalCredentialBuilder builder;
    @Override public Mono<FacadeLocalCredentialBO> getByLoginName(Long tenantId, String loginName) {
        return credentialService.getByLoginName(tenantId, loginName).map(builder::toFacadeBO);
    }
}

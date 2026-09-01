package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.service.ReactiveTenantService;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.local.builder.FacadeTenantBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TenantLocalFacade implements TenantFacade {
    private final ReactiveTenantService tenantService;
    private final FacadeTenantBuilder builder;
    @Override public Mono<FacadeTenantBO> getByCode(String code) { return tenantService.getByCode(code).map(builder::toFacadeBO); }
}

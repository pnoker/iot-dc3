package io.github.pnoker.common.auth.security;

import io.github.pnoker.common.auth.service.ReactiveTenantService;
import io.github.pnoker.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Non-blocking authorization guard for system-global metadata mutations. */
@Component
@RequiredArgsConstructor
public class ReactiveAdminChecker {

    private final ReactiveTenantService tenantService;

    public Mono<Void> assertSystemAdmin(Long tenantId) {
        return tenantService.getById(tenantId)
                .flatMap(tenant -> "default".equals(tenant.getTenantCode())
                        ? Mono.<Void>empty()
                        : Mono.error(new ServiceException(
                                "Only system administrators can manage system-global entities (resources, menus, APIs)")));
    }
}

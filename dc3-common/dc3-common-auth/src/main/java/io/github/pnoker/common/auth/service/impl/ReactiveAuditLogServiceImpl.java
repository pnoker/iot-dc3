package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import io.github.pnoker.common.auth.entity.builder.IdentityAuditLogBuilder;
import io.github.pnoker.common.auth.entity.vo.IdentityAuditLogVO;
import io.github.pnoker.common.auth.repository.IdentityAuditLogFilter;
import io.github.pnoker.common.auth.repository.ReactiveAuditLogStore;
import io.github.pnoker.common.auth.repository.ReactiveAuditLogQueryStore;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import io.github.pnoker.common.auth.service.ReactiveAuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking audit service with best-effort failure isolation. */
@Slf4j
@Service
public class ReactiveAuditLogServiceImpl implements ReactiveAuditLogService {

    private final ReactiveAuditLogStore store;
    private final ReactiveAuditLogQueryStore queryStore;
    private final IdentityAuditLogBuilder builder;

    public ReactiveAuditLogServiceImpl(ReactiveAuditLogStore store,
                                       ReactiveAuditLogQueryStore queryStore,
                                       IdentityAuditLogBuilder builder) {
        this.store = store;
        this.queryStore = queryStore;
        this.builder = builder;
    }

    @Override
    public Mono<Void> log(Long tenantId, Long principalId, String principalType, String action,
                          String resourceType, Long resourceId, String resourceName, String status,
                          String errorCode) {
        return Mono.defer(() -> {
                    if (tenantId == null || tenantId <= 0) {
                        return Mono.error(new IllegalArgumentException("audit tenantId is required"));
                    }
                    IdentityAuditLogBO event = ReactiveAuditLogService.event(tenantId, principalId,
                            principalType, action, resourceType, resourceId, resourceName, status, errorCode);
                    return store.append(event);
                })
                .onErrorResume(error -> {
                    log.warn("Failed to record identity audit log (action={}, resourceType={}, resourceId={})",
                            action, resourceType, resourceId, error);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<CursorPage<IdentityAuditLogVO>> list(IdentityAuditLogFilter filter) {
        return Mono.defer(() -> queryStore.list(filter))
                .map(page -> new CursorPage<>(page.items().stream()
                                .map(builder::buildBOByDO)
                                .map(builder::buildVOByBO)
                                .toList(),
                        page.nextCursor(), page.hasNext()));
    }
}

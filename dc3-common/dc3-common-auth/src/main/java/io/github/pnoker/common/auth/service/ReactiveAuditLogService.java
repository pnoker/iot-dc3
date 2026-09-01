package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import io.github.pnoker.common.auth.entity.vo.IdentityAuditLogVO;
import io.github.pnoker.common.auth.repository.IdentityAuditLogFilter;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import io.github.pnoker.common.entity.common.RequestHeader;
import reactor.core.publisher.Mono;

/** Non-blocking identity and authorization audit service. */
public interface ReactiveAuditLogService {

    /**
     * Append one audit event without allowing audit persistence to fail the business operation.
     *
     * @param tenantId tenant scope
     * @param principalId actor principal
     * @param principalType actor type
     * @param action action performed
     * @param resourceType affected resource type
     * @param resourceId affected resource id
     * @param resourceName affected resource name
     * @param status outcome status
     * @param errorCode stable failure code
     * @return completion after the append attempt
     */
    Mono<Void> log(Long tenantId, Long principalId, String principalType, String action,
                   String resourceType, Long resourceId, String resourceName, String status,
                   String errorCode);

    Mono<CursorPage<IdentityAuditLogVO>> list(IdentityAuditLogFilter filter);

    default Mono<Void> log(RequestHeader.PrincipalHeader actor, String action, String resourceType,
                           Long resourceId, String resourceName, String status, String errorCode) {
        return log(actor == null ? 0L : actor.getTenantId(),
                actor == null ? 0L : actor.getPrincipalId(),
                actor == null || actor.getPrincipalType() == null ? "USER" : actor.getPrincipalType(),
                action, resourceType, resourceId, resourceName, status, errorCode);
    }

    /** Package-level helper for implementations and focused tests. */
    static IdentityAuditLogBO event(Long tenantId, Long principalId, String principalType, String action,
                                    String resourceType, Long resourceId, String resourceName,
                                    String status, String errorCode) {
        IdentityAuditLogBO event = new IdentityAuditLogBO();
        event.setTenantId(tenantId);
        event.setPrincipalId(principalId);
        event.setPrincipalType(principalType);
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setResourceName(resourceName);
        event.setStatus(status);
        event.setErrorCode(errorCode);
        return event;
    }
}

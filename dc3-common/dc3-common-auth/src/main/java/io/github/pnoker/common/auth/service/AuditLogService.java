/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import io.github.pnoker.common.entity.common.RequestHeader;

import java.util.List;

/**
 * Identity and authorization change audit log service. Callers record events via {@link #log};
 * the audit view reads them via {@link #list}.
 *
 * @author pnoker
 * @version 2026.6.14
 * @since 2026.6.14
 */
public interface AuditLogService {

    /**
     * Persist one audit event for an identity or authorization change.
     *
     * @param tenantId      tenant scope
     * @param principalId   the actor principal id
     * @param principalType the actor principal type (USER/SERVICE_ACCOUNT)
     * @param action        the action performed (e.g. create/update/delete/login)
     * @param resourceType  the affected resource type
     * @param resourceId    the affected resource id, may be null
     * @param resourceName  the affected resource name, may be null
     * @param status        the outcome status (success/failed)
     * @param errorCode     an error code on failure, may be null
     */
    void log(Long tenantId, Long principalId, String principalType, String action,
             String resourceType, Long resourceId, String resourceName, String status, String errorCode);

    /**
     * Query audit events by any combination of filters, scoped to a tenant. Null filter
     * arguments match all values.
     *
     * @param tenantId     tenant scope
     * @param principalId  optional principal filter
     * @param action       optional action filter
     * @param resourceType optional resource type filter
     * @param resourceId   optional resource id filter
     * @param status       optional status filter
     * @param limit        maximum number of events
     * @return the matching audit events
     */
    List<IdentityAuditLogBO> list(Long tenantId, Long principalId, String action, String resourceType,
                                  Long resourceId, String status, int limit);

    /**
     * Convenience overload resolving the actor (tenant/principal/type) from the request header.
     */
    default void log(RequestHeader.PrincipalHeader actor, String action, String resourceType,
                     Long resourceId, String resourceName, String status, String errorCode) {
        log(actor == null ? 0L : actor.getTenantId(),
                actor == null ? 0L : actor.getPrincipalId(),
                actor == null || actor.getPrincipalType() == null ? "USER" : actor.getPrincipalType(),
                action, resourceType, resourceId, resourceName, status, errorCode);
    }
}

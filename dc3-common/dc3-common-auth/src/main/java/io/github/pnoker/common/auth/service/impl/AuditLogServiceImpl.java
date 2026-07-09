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

package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.dal.IdentityAuditLogManager;
import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import io.github.pnoker.common.auth.entity.builder.IdentityAuditLogBuilder;
import io.github.pnoker.common.auth.entity.model.IdentityAuditLogDO;
import io.github.pnoker.common.auth.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Identity audit log service implementation.
 *
 * @author pnoker
 * @version 2026.6.14
 * @since 2026.6.14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final IdentityAuditLogManager identityAuditLogManager;
    private final IdentityAuditLogBuilder identityAuditLogBuilder;

    @Override
    public void log(Long tenantId, Long principalId, String principalType, String action,
                    String resourceType, Long resourceId, String resourceName, String status, String errorCode) {
        IdentityAuditLogDO entity = new IdentityAuditLogDO();
        entity.setTenantId(tenantId == null ? 0L : tenantId);
        entity.setPrincipalId(principalId == null ? 0L : principalId);
        entity.setPrincipalType(principalType == null ? "USER" : principalType);
        entity.setAction(action);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId == null ? 0L : resourceId);
        entity.setResourceName(resourceName == null ? "" : resourceName);
        entity.setStatus(status);
        entity.setErrorCode(errorCode == null ? "" : errorCode);
        // Audit must never break the audited business operation.
        try {
            identityAuditLogManager.save(entity);
        } catch (Exception e) {
            log.warn("Failed to record identity audit log (action={}, resourceType={}, resourceId={})",
                    action, resourceType, resourceId, e);
        }
    }

    @Override
    public List<IdentityAuditLogBO> list(Long tenantId, Long principalId, String action, String resourceType,
                                         Long resourceId, String status, int limit) {
        int bounded = Math.max(1, Math.min(limit <= 0 ? 200 : limit, 500));
        List<IdentityAuditLogDO> entityDOList = identityAuditLogManager.listIdentityAudit(tenantId, principalId,
                action, resourceType, resourceId, status, bounded);
        return identityAuditLogBuilder.buildBOListByDOList(entityDOList);
    }
}

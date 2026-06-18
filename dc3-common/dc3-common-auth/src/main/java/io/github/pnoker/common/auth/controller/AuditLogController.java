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

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.auth.entity.model.IdentityAuditLogDO;
import io.github.pnoker.common.auth.service.AuditLogService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller exposing the identity/authorization audit log. Admin-only and tenant-scoped.
 *
 * @author pnoker
 * @version 2026.6.14
 * @since 2026.6.14
 */
@Tag(name = "identity_audit", description = "Identity audit trails: query authentication and authorization event logs for security compliance and forensic analysis")
@Slf4j
@RestController
@RequestMapping(AuthConstant.IDENTITY_AUDIT_URL_PREFIX)
@RequiredArgsConstructor
public class AuditLogController implements BaseController {

    private final AuditLogService auditLogService;

    @PreAuthorize("@perm.can('audit', 'list')")
    @Operation(summary = "List Identity Audit Log",
            description = "List identity and authorization audit entries for the current tenant, with optional filters " +
                    "by principal, action, resource type/id and status, plus a result limit. Admin-only; returns an " +
                    "append-only trail of who changed which identity or permission.")
    @PostMapping("/list")
    public Mono<R<List<IdentityAuditLogDO>>> list(
            @Parameter(description = "Principal ID") @RequestParam(value = "principalId", required = false) Long principalId,
            @Parameter(description = "Action") @RequestParam(value = "action", required = false) String action,
            @Parameter(description = "Resource type") @RequestParam(value = "resourceType", required = false) String resourceType,
            @Parameter(description = "Resource ID") @RequestParam(value = "resourceId", required = false) Long resourceId,
            @Parameter(description = "Status") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "Limit") @RequestParam(value = "limit", required = false) Integer limit) {
        return getTenantId().flatMap(tenantId -> async(() -> R.ok(auditLogService.list(
                tenantId, principalId, StringUtils.defaultString(action), StringUtils.defaultString(resourceType),
                resourceId, StringUtils.defaultString(status), limit == null ? 0 : limit
        ))));
    }
}

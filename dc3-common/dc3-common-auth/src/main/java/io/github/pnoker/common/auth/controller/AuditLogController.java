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

import io.github.pnoker.common.auth.entity.query.IdentityAuditLogCursorRequest;
import io.github.pnoker.common.auth.entity.vo.IdentityAuditLogVO;
import io.github.pnoker.common.auth.repository.IdentityAuditLogFilter;
import io.github.pnoker.common.auth.service.ReactiveAuditLogService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing the identity/authorization audit log. Admin-only and tenant-scoped.
 *
 * @author pnoker
 * @since 2026.6.14
 */
@Tag(
        name = "identity_audit",
        description =
                "Identity audit trails: query authentication and authorization event logs for security compliance and forensic analysis")
@Slf4j
@RestController
@RequestMapping(AuthConstant.IDENTITY_AUDIT_URL_PREFIX)
@RequiredArgsConstructor
public class AuditLogController implements BaseController {

    private final ReactiveAuditLogService auditLogService;

    /**
     * List identity and authorization audit entries for the current tenant.
     *
     * @param request signed-cursor request containing filters and page size
     * @return an append-only list of IdentityAuditLogVO matching the filters; admin-only, tenant-scoped
     */
    @PreAuthorize("@perm.can('audit', 'list')")
    @Operation(
            summary = "List Identity Audit Log",
            description =
                    "List identity and authorization audit entries for the current tenant using a signed opaque cursor. "
                            + "Filters are bound to the cursor and the fixed create-time/id descending order; no total count is computed.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list")
    public Mono<ResponseEntity<CursorPage<IdentityAuditLogVO>>> list(
            @RequestBody(required = false) IdentityAuditLogCursorRequest request) {
        IdentityAuditLogCursorRequest query = request == null ? new IdentityAuditLogCursorRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> auditLogService
                        .list(new IdentityAuditLogFilter(
                                tenantId,
                                query.principalId(),
                                query.action(),
                                query.resourceType(),
                                query.resourceId(),
                                query.status(),
                                query.cursor(),
                                query.limit()))
                        .map(ResponseEntity::ok));
    }
}

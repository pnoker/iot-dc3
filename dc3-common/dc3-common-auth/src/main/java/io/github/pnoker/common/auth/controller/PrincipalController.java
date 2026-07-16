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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.builder.PrincipalBuilder;
import io.github.pnoker.common.auth.entity.query.PrincipalQuery;
import io.github.pnoker.common.auth.entity.vo.PrincipalVO;
import io.github.pnoker.common.auth.service.AuditLogService;
import io.github.pnoker.common.auth.service.PrincipalService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing principal management endpoints. Admin-only — the principal roster is a
 * platform identity view, so tenant scope is enforced by {@code @perm.can('principal', ...)} RBAC
 * rather than a tenant column on {@code dc3_principal}.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
@Tag(name = "principal", description = "Security principals: manage users and service accounts as identity carriers for authentication, authorization, and access control")
@Slf4j
@RestController
@RequestMapping(AuthConstant.PRINCIPAL_URL_PREFIX)
@RequiredArgsConstructor
public class PrincipalController implements BaseController {

    private final PrincipalBuilder principalBuilder;

    private final PrincipalService principalService;

    private final AuditLogService auditLogService;

    /**
     * Fetch one principal (user or service account) by ID.
     *
     * @param id id of the principal to fetch
     * @return the matched PrincipalVO; the principal is the identity roles and permissions bind to
     */
    @PreAuthorize("@perm.can('principal', 'get')")
    @Operation(summary = "Get Principal by ID", description = "Fetch one principal (user or service account) by ID. " +
            "A principal is the abstract identity that roles and permissions bind to; use to resolve an identity before binding roles or auditing access.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<PrincipalVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return async(() -> R.ok(principalBuilder.buildVOByBO(principalService.getById(id))));
    }

    /**
     * Page through principals (users and service accounts) for the current tenant.
     *
     * @param entityQuery optional filter criteria; an empty query pages all principals
     * @return a page of PrincipalVO matching the query
     */
    @PreAuthorize("@perm.can('principal', 'list')")
    @Operation(summary = "List Principals", description = "Page through principals (users and service accounts) for the current tenant. " +
            "Accepts a PrincipalQuery body for filtering; returns a page of PrincipalVO entries for browsing or selecting a target identity.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<PrincipalVO>>> list(@RequestBody(required = false) PrincipalQuery entityQuery) {
        PrincipalQuery query = Objects.isNull(entityQuery) ? new PrincipalQuery() : entityQuery;
        return async(() -> R.ok(principalBuilder.buildVOPageByBOPage(principalService.list(query))));
    }

    /**
     * Batch-resolve principals by their IDs in one call.
     *
     * @param ids principal ids to resolve
     * @return the matching PrincipalVO entries (principalName / displayName) for name display
     */
    @PreAuthorize("@perm.can('principal', 'list')")
    @Operation(summary = "List Principals by IDs", description = "Batch-resolve principals by their IDs in one call. " +
            "Accepts a list of principal IDs and returns the matching PrincipalVO entries (principalName / displayName), " +
            "so callers can render principalId references from other lists as human-readable names.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_by_ids")
    public Mono<R<List<PrincipalVO>>> listByIds(@RequestBody Collection<Long> ids) {
        return async(() -> R.ok(principalBuilder.buildVOListByBOList(principalService.listByIds(ids))));
    }

    /**
     * Set a principal's enable flag to ENABLE so the identity can authenticate.
     *
     * @param id id of the principal to enable
     * @return update-success status; an ENABLE audit-log entry is recorded under the acting caller
     */
    @PreAuthorize("@perm.can('principal', 'update')")
    @Operation(summary = "Enable Principal", description = "Set a principal's enable flag to ENABLE by ID so the identity can authenticate. " +
            "Tenant-scoped; records an ENABLE audit-log entry under the acting caller.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/enable")
    public Mono<R<String>> enable(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return toggleEnableFlag(id, EnableFlagEnum.ENABLE);
    }

    /**
     * Set a principal's enable flag to DISABLE to revoke active access for that identity.
     *
     * @param id id of the principal to disable
     * @return update-success status; a DISABLE audit-log entry is recorded under the acting caller
     */
    @PreAuthorize("@perm.can('principal', 'update')")
    @Operation(summary = "Disable Principal", description = "Set a principal's enable flag to DISABLE by ID to revoke active access for that identity. " +
            "Tenant-scoped; records a DISABLE audit-log entry under the acting caller.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/disable")
    public Mono<R<String>> disable(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return toggleEnableFlag(id, EnableFlagEnum.DISABLE);
    }

    /**
     * Toggle a principal's enable flag and record an audit entry for the change.
     *
     * @param id     the principal id
     * @param target the target enable flag
     * @return update-success result
     */
    private Mono<R<String>> toggleEnableFlag(Long id, EnableFlagEnum target) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            principalService.setEnableFlag(id, target, header.getUserId(), header.getNickName());
            auditLogService.log(header, target == EnableFlagEnum.ENABLE ? "ENABLE" : "DISABLE",
                    "principal", id, null, "SUCCESS", null);
            return R.ok(SuccessCode.UPDATE);
        }));
    }
}

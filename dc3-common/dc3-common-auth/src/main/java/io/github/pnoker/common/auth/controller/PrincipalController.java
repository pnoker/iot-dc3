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
import io.github.pnoker.common.auth.entity.bo.PrincipalBO;
import io.github.pnoker.common.auth.entity.builder.PrincipalBuilder;
import io.github.pnoker.common.auth.entity.query.PrincipalQuery;
import io.github.pnoker.common.auth.entity.vo.PrincipalVO;
import io.github.pnoker.common.auth.service.AuditLogService;
import io.github.pnoker.common.auth.service.PrincipalService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResponseEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "principal", description = "Principals")
@Slf4j
@RestController
@RequestMapping(AuthConstant.PRINCIPAL_URL_PREFIX)
@RequiredArgsConstructor
public class PrincipalController implements BaseController {

    private final PrincipalBuilder principalBuilder;

    private final PrincipalService principalService;

    private final AuditLogService auditLogService;

    @PreAuthorize("@perm.can('principal', 'get')")
    @Operation(summary = "Get Principal by ID", description = "Get principal details by ID")
    @GetMapping("/get_by_id")
    public Mono<R<PrincipalVO>> getById(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return async(() -> R.ok(principalBuilder.buildVOByBO(principalService.getById(id))));
    }

    @PreAuthorize("@perm.can('principal', 'list')")
    @Operation(summary = "List Principals", description = "List principals with pagination")
    @PostMapping("/list")
    public Mono<R<Page<PrincipalVO>>> list(@RequestBody(required = false) PrincipalQuery entityQuery) {
        PrincipalQuery query = Objects.isNull(entityQuery) ? new PrincipalQuery() : entityQuery;
        return async(() -> R.ok(principalBuilder.buildVOPageByBOPage(principalService.list(query))));
    }

    @PreAuthorize("@perm.can('principal', 'update')")
    @Operation(summary = "Enable Principal", description = "Enable a principal by ID")
    @PostMapping("/enable")
    public Mono<R<String>> enable(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return toggleEnableFlag(id, EnableFlagEnum.ENABLE);
    }

    @PreAuthorize("@perm.can('principal', 'update')")
    @Operation(summary = "Disable Principal", description = "Disable a principal by ID")
    @PostMapping("/disable")
    public Mono<R<String>> disable(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return toggleEnableFlag(id, EnableFlagEnum.DISABLE);
    }

    private Mono<R<String>> toggleEnableFlag(Long id, EnableFlagEnum target) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            principalService.setEnableFlag(id, target, header.getUserId(), header.getNickName());
            auditLogService.log(header, target == EnableFlagEnum.ENABLE ? "ENABLE" : "DISABLE",
                    "principal", id, null, "SUCCESS", null);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }
}

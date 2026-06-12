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
import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.builder.ServiceAccountBuilder;
import io.github.pnoker.common.auth.entity.query.ServiceAccountQuery;
import io.github.pnoker.common.auth.entity.vo.ServiceAccountVO;
import io.github.pnoker.common.auth.service.ServiceAccountService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller exposing service account management endpoints.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Tag(name = "service_account", description = "Service accounts")
@Slf4j
@RestController
@RequestMapping(AuthConstant.SERVICE_ACCOUNT_URL_PREFIX)
@RequiredArgsConstructor
public class ServiceAccountController implements BaseController {

    private final ServiceAccountBuilder serviceAccountBuilder;

    private final ServiceAccountService serviceAccountService;

    private final TenantMembershipService tenantMembershipService;

    @PreAuthorize("@perm.can('service_account', 'add')")
    @Operation(summary = "Add Service Account", description = "Create a service account principal")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ServiceAccountVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            ServiceAccountBO entityBO = serviceAccountBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            tenantMembershipService.requireTenantMember(header.getTenantId(), entityBO.getOwnerPrincipalId());
            fillCreateAudit(entityBO, header);
            serviceAccountService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('service_account', 'delete')")
    @Operation(summary = "Delete Service Account", description = "Delete a service account by ID")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            ServiceAccountBO entityBO = requireTenant(tenantId, serviceAccountService.getById(id));
            serviceAccountService.delete(entityBO.getId());
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('service_account', 'update')")
    @Operation(summary = "Update Service Account", description = "Update a service account")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ServiceAccountVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            ServiceAccountBO current = requireTenant(header.getTenantId(), serviceAccountService.getById(entityVO.getId()));
            ServiceAccountBO entityBO = serviceAccountBuilder.buildBOByVO(entityVO);
            entityBO.setId(current.getId());
            entityBO.setTenantId(current.getTenantId());
            Long ownerPrincipalId = Objects.requireNonNullElse(entityBO.getOwnerPrincipalId(),
                    current.getOwnerPrincipalId());
            tenantMembershipService.requireTenantMember(header.getTenantId(), ownerPrincipalId);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            serviceAccountService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('service_account', 'get')")
    @Operation(summary = "Get Service Account by ID", description = "Get service account details by ID")
    @GetMapping("/get_by_id")
    public Mono<R<ServiceAccountVO>> getById(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            ServiceAccountBO entityBO = requireTenant(tenantId, serviceAccountService.getById(id));
            return R.ok(serviceAccountBuilder.buildVOByBO(entityBO));
        }));
    }

    @PreAuthorize("@perm.can('service_account', 'list')")
    @Operation(summary = "List Service Accounts", description = "List service accounts with pagination")
    @PostMapping("/list")
    public Mono<R<Page<ServiceAccountVO>>> list(@RequestBody(required = false) ServiceAccountQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            ServiceAccountQuery query = Objects.isNull(entityQuery) ? new ServiceAccountQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<ServiceAccountBO> entityPageBO = serviceAccountService.list(query);
            return R.ok(serviceAccountBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

    private void fillCreateAudit(ServiceAccountBO entityBO, RequestHeader.PrincipalHeader header) {
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getNickName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getNickName());
    }

}

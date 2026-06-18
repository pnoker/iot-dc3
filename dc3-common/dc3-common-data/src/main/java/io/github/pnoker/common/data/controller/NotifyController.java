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

package io.github.pnoker.common.data.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.query.NotifyQuery;
import io.github.pnoker.common.data.entity.vo.NotifyVO;
import io.github.pnoker.common.data.service.NotifyService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.SuccessCode;
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
 * Alarm notification policy controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "notify", description = "Notification rule definitions: manage alert rules that trigger on device events, data thresholds, and status changes with configurable severity and routing")
@Slf4j
@RestController
@RequestMapping(DataConstant.NOTIFY_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyController implements BaseController {

    private final NotifyBuilder notifyBuilder;

    private final NotifyService notifyService;

    /**
     * Create a notification (alert-routing) rule for the current tenant.
     *
     * @param entityVO notification rule payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('notify', 'add')")
    @Operation(summary = "Add Notification Rule", description = "Create a notification (alert-routing) rule for the current tenant that wires alarm triggers to delivery channels. Use to define how and where alerts are dispatched.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody NotifyVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyBO entityBO = notifyBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            notifyService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a notification rule owned by the current tenant.
     *
     * @param id id of the notification rule to delete
     * @return delete-success status; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('notify', 'delete')")
    @Operation(summary = "Delete Notification Rule", description = "Delete a notification rule by ID for the current tenant. Ownership is validated before deletion, so cross-tenant records cannot be removed.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, notifyService.getById(id));
            notifyService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update an existing notification rule owned by the current tenant.
     *
     * @param entityVO notification rule payload to update
     * @return update-success status; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('notify', 'update')")
    @Operation(summary = "Update Notification Rule", description = "Update an existing notification rule (routing, severity, enable flag) for the current tenant. Ownership is validated before the update is applied.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody NotifyVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyBO entityBO = notifyBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, notifyService.getById(entityBO.getId()));
            notifyService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Return a single notification rule owned by the current tenant.
     *
     * @param id id of the notification rule to retrieve
     * @return the matched NotifyVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('notify', 'get')")
    @Operation(summary = "Get Notification Rule by ID", description = "Return a single notification rule for the current tenant. Use to inspect routing, severity and channel bindings for one rule.")
    @GetMapping("/get_by_id")
    public Mono<R<NotifyVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyBO entityBO = requireTenant(tenantId, notifyService.getById(id));
            return R.ok(notifyBuilder.buildVOByBO(entityBO));
        }));
    }

    /**
     * Page through notification rules owned by the current tenant.
     *
     * @param entityQuery optional filter query (pagination, severity, channel); treated as empty when null
     * @return a page of NotifyVO matching the query
     */
    @PreAuthorize("@perm.can('notify', 'list')")
    @Operation(summary = "List Notification Rules", description = "Page through notification (alert-routing) rules for the current tenant with optional query filters. Use to enumerate which rules govern alert dispatch.")
    @PostMapping("/list")
    public Mono<R<Page<NotifyVO>>> list(@RequestBody(required = false) NotifyQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyQuery query = Objects.isNull(entityQuery) ? new NotifyQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<NotifyBO> entityPageBO = notifyService.list(query);
            return R.ok(notifyBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}

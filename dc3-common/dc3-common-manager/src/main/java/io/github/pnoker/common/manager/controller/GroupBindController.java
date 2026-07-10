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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.dal.entity.bo.GroupBO;
import io.github.pnoker.common.dal.entity.bo.GroupBindBO;
import io.github.pnoker.common.dal.entity.builder.GroupBindBuilder;
import io.github.pnoker.common.dal.entity.query.GroupBindQuery;
import io.github.pnoker.common.dal.entity.vo.GroupBindVO;
import io.github.pnoker.common.dal.service.GroupBindService;
import io.github.pnoker.common.dal.service.GroupService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.service.EntityTenantService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
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
 * REST controller exposing group binding management endpoints.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Tag(name = "group_bind", description = "Group membership bindings: associate devices, drivers, and other entities with logical groups for hierarchical organization and bulk operations")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.GROUP_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class GroupBindController implements BaseController {

    private final GroupBindBuilder groupBindBuilder;

    private final GroupBindService groupBindService;

    private final GroupService groupService;

    private final EntityTenantService entityTenantService;

    /**
     * Attach a tenant entity to a group.
     *
     * @param entityVO group binding payload to create (group id, entity id, entity type)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('group_bind', 'add')")
    @Operation(summary = "Add Group Binding", description = "Attach a tenant entity (device, driver, point, etc.) to a group. The entity's type must match the group's type and ownership is tenant-scoped; returns the new binding ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody GroupBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBindBO entityBO = groupBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            validateBind(tenantId, entityBO);
            groupBindService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Remove a group-to-entity binding by ID.
     *
     * @param id id of the group binding to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('group_bind', 'delete')")
    @Operation(summary = "Delete Group Binding", description = "Remove a group-to-entity binding by ID (tenant-scoped). Detaches the entity from the group without deleting the group or the entity.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, groupBindService.getById(id));
            groupBindService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Change the group or entity referenced by an existing binding.
     *
     * @param entityVO group binding payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('group_bind', 'update')")
    @Operation(summary = "Update Group Binding", description = "Change the group or entity referenced by an existing binding (tenant-scoped). The new entity's type must still match the target group's type.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody GroupBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBindBO entityBO = groupBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, groupBindService.getById(entityBO.getId()));
            validateBind(tenantId, entityBO);
            groupBindService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch a single group binding by ID.
     *
     * @param id id of the group binding to fetch (must be tenant-owned)
     * @return the matched GroupBindVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('group_bind', 'get')")
    @Operation(summary = "Get Group Binding by ID", description = "Fetch one group binding by ID (tenant-scoped). Returns the group ID, entity type and entity ID of the association; use to inspect a specific link before editing or removing it.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<GroupBindVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBindBO entityBO = requireTenant(tenantId, groupBindService.getById(id));
            GroupBindVO entityVO = groupBindBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Page through group bindings with filters.
     *
     * @param entityQuery query filters such as group id, entity type or entity id (may be null)
     * @return a page of GroupBindVO matching the query
     */
    @PreAuthorize("@perm.can('group_bind', 'list')")
    @Operation(summary = "List Group Bindings", description = "Page through group bindings for the current tenant, optionally filtered by group ID, entity type or entity ID. Returns a page of bindings; use to enumerate the members of a group or the groups an entity belongs to.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<GroupBindVO>>> list(@RequestBody(required = false) GroupBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBindQuery query = Objects.isNull(entityQuery) ? new GroupBindQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<GroupBindBO> entityPageBO = groupBindService.list(query);
            Page<GroupBindVO> entityPageVO = groupBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Validate a group binding: the group belongs to the tenant, its type matches the
     * entity type, and the bound entity itself belongs to the tenant.
     *
     * @param tenantId tenant scope
     * @param entityBO the binding to validate
     */
    private void validateBind(Long tenantId, GroupBindBO entityBO) {
        EntityTypeEnum entityTypeFlag = entityBO.getEntityTypeFlag();
        GroupBO groupBO = requireTenant(tenantId, groupService.getById(entityBO.getGroupId()));
        if (!Objects.equals(groupBO.getGroupTypeFlag(), entityTypeFlag)) {
            throw new NotFoundException("Resource does not exist");
        }
        entityTenantService.requireEntityTenant(tenantId, entityTypeFlag, entityBO.getEntityId());
    }

}

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
import io.github.pnoker.common.dal.entity.builder.GroupBuilder;
import io.github.pnoker.common.dal.entity.query.GroupQuery;
import io.github.pnoker.common.dal.entity.vo.GroupVO;
import io.github.pnoker.common.dal.service.GroupService;
import io.github.pnoker.common.entity.R;
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
 * REST controller exposing group management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "group", description = "Logical group hierarchy: create, update, and manage groups for organizing devices, drivers, and platform resources into hierarchical collections")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.GROUP_URL_PREFIX)
@RequiredArgsConstructor
public class GroupController implements BaseController {

    private final GroupBuilder groupBuilder;

    private final GroupService groupService;

    /**
     * @param entityVO {@link GroupVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('group', 'add')")
    @Operation(summary = "Add Group", description = "Create a group for the current tenant. A group is a logical grouping of devices, drivers, points or other entities used for batch operations; returns the new group ID.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody GroupVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBO entityBO = groupBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            groupService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('group', 'delete')")
    @Operation(summary = "Delete Group", description = "Permanently delete a group by ID (tenant-scoped). Removes the grouping definition without deleting its member entities; the action cannot be undone.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, groupService.getById(id));
            groupService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * @param entityVO {@link GroupVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('group', 'update')")
    @Operation(summary = "Update Group", description = "Update an existing group's attributes for the current tenant. Validates tenant ownership before applying the change; returns the updated group ID.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody GroupVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBO entityBO = groupBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, groupService.getById(entityBO.getId()));
            groupService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return GroupVO {@link GroupVO}
     */
    @PreAuthorize("@perm.can('group', 'get')")
    @Operation(summary = "Get Group by ID", description = "Fetch one group by ID for the current tenant. Use to inspect a grouping definition before assigning entities to it or performing batch operations.")
    @GetMapping("/get_by_id")
    public Mono<R<GroupVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupBO entityBO = requireTenant(tenantId, groupService.getById(id));
            GroupVO entityVO = groupBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * @param entityQuery {@link GroupQuery}
     * @return R Of GroupVO Page
     */
    @PreAuthorize("@perm.can('group', 'list')")
    @Operation(summary = "List Groups", description = "Page through groups for the current tenant with optional query filters. Returns a page of groups; use for browsing available groupings or selecting a target group.")
    @PostMapping("/list")
    public Mono<R<Page<GroupVO>>> list(@RequestBody(required = false) GroupQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            GroupQuery query = Objects.isNull(entityQuery) ? new GroupQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<GroupBO> entityPageBO = groupService.list(query);
            Page<GroupVO> entityPageVO = groupBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}

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

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.manager.entity.bo.GroupBO;
import io.github.pnoker.common.manager.entity.builder.GroupBuilder;
import io.github.pnoker.common.manager.entity.query.GroupListRequest;
import io.github.pnoker.common.manager.entity.vo.GroupVO;
import io.github.pnoker.common.manager.repository.GroupFilter;
import io.github.pnoker.common.manager.service.ReactiveGroupService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing group management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(name = "group", description = "Logical group hierarchy: create, update, and manage groups for organizing devices, drivers, and platform resources into hierarchical collections")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.GROUP_URL_PREFIX)
@RequiredArgsConstructor
public class GroupController implements BaseController {

    private final GroupBuilder groupBuilder;

    private final ReactiveGroupService groupService;

    /**
     * Create a group for the current tenant.
     *
     * @param entityVO group payload to create (name, group type)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('group', 'add')")
    @Operation(summary = "Add Group", description = "Create a group for the current tenant. A group is a logical grouping of devices, drivers, points or other entities used for batch operations; returns the new group ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<ResponseEntity<GroupVO>> add(@Validated(Add.class) @RequestBody GroupVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            GroupBO entityBO = groupBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tuple.getT1().getT1());entityBO.setCreatorId(tuple.getT1().getT2());entityBO.setCreatorName(tuple.getT2());entityBO.setOperatorId(tuple.getT1().getT2());entityBO.setOperatorName(tuple.getT2());
            return groupService.add(entityBO).map(groupBuilder::buildVOByBO).map(v->ResponseEntity.status(HttpStatus.CREATED).body(v));
        });
    }

    /**
     * Delete a group by ID.
     *
     * @param id id of the group to delete (must be tenant-owned)
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('group', 'delete')")
    @Operation(summary = "Delete Group", description = "Permanently delete a group by ID (tenant-scoped). Removes the grouping definition without deleting its member entities; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple->groupService.delete(tuple.getT1().getT1(),id,tuple.getT1().getT2(),tuple.getT2()).thenReturn(ResponseEntity.noContent().build()));
    }

    /**
     * Update an existing group's attributes.
     *
     * @param entityVO group payload to update (must carry an existing id)
     * @return update-success status
     */
    @PreAuthorize("@perm.can('group', 'update')")
    @Operation(summary = "Update Group", description = "Update an existing group's attributes for the current tenant. Validates tenant ownership before applying the change; returns the updated group ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<ResponseEntity<GroupVO>> update(@Validated(Update.class) @RequestBody GroupVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            GroupBO entityBO = groupBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tuple.getT1().getT1());entityBO.setOperatorId(tuple.getT1().getT2());entityBO.setOperatorName(tuple.getT2());
            return groupService.update(entityBO).map(groupBuilder::buildVOByBO).map(ResponseEntity::ok);
        });
    }

    /**
     * Fetch a single group by ID.
     *
     * @param id id of the group to fetch (must be tenant-owned)
     * @return the matched GroupVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('group', 'get')")
    @Operation(summary = "Get Group by ID", description = "Fetch one group by ID for the current tenant. Use to inspect a grouping definition before assigning entities to it or performing batch operations.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<GroupVO> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId->groupService.getById(tenantId,id)).map(groupBuilder::buildVOByBO);
    }

    /**
     * Page through groups with filters.
     *
     * @param entityQuery query filters (may be null)
     * @return a page of GroupVO matching the query
     */
    @PreAuthorize("@perm.can('group', 'list')")
    @Operation(summary = "List Groups", description = "Page through groups for the current tenant with optional query filters. Returns a page of groups; use for browsing available groupings or selecting a target group.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<OffsetPage<GroupVO>> list(@RequestBody(required = false) GroupListRequest request) {
        GroupListRequest query=request==null?new GroupListRequest():request;
        return getTenantId().flatMap(tenantId->groupService.list(new GroupFilter(tenantId,query.groupName(),query.parentGroupId(),query.position(),query.groupTypeFlag(),query.enableFlag(),query.offset(),query.limit(),query.sort())))
                .map(page->new OffsetPage<>(page.items().stream().map(groupBuilder::buildVOByBO).toList(),page.offset(),page.limit(),page.total(),page.hasNext()));
    }

}

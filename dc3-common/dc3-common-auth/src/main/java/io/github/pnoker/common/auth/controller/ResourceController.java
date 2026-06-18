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
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.ResourceTreeBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.query.ResourceQuery;
import io.github.pnoker.common.auth.entity.vo.ResourceTreeVO;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.auth.security.AdminChecker;
import io.github.pnoker.common.auth.service.ResourceService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
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

import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing resource management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "resource", description = "Protected resource registry: manage API endpoints, menu items, and other securable artifacts that require permission to access")
@Slf4j
@RestController
@RequestMapping(AuthConstant.RESOURCE_URL_PREFIX)
@RequiredArgsConstructor
public class ResourceController implements BaseController {

    private final ResourceBuilder resourceBuilder;

    private final ResourceService resourceService;

    private final AdminChecker adminChecker;

    /**
     * Create a new resource, the permission-grantable unit an endpoint declares.
     *
     * @param entityVO resource payload to create (e.g. code such as "device:add")
     * @return add-success status; restricted to system admins
     */
    @PreAuthorize("@perm.can('resource', 'add')")
    @Operation(summary = "Add Resource", description = "Create a new resource, the permission-grantable unit an endpoint declares (e.g. \"device:add\"). Restricted to system admins; returns the new resource ID.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ResourceVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            ResourceBO entityBO = resourceBuilder.buildBOByVO(entityVO);
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            resourceService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * Remove a resource permission unit by ID, severing any role bindings that granted it.
     *
     * @param id id of the resource to delete
     * @return delete-success status; restricted to system admins
     */
    @PreAuthorize("@perm.can('resource', 'delete')")
    @Operation(summary = "Delete Resource by ID", description = "Remove a resource permission unit by ID. Restricted to system admins; deleting severs any role bindings that granted it.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the resource record to delete; deleting it also severs any role bindings that granted this permission.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            resourceService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * Modify an existing resource's definition (e.g. its code or metadata).
     *
     * @param entityVO resource payload to update
     * @return update-success status; restricted to system admins, the change applies to every bound role
     */
    @PreAuthorize("@perm.can('resource', 'update')")
    @Operation(summary = "Update Resource", description = "Modify an existing resource's definition (e.g. its code or metadata). Restricted to system admins; the change takes effect for every role bound to this resource.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ResourceVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            ResourceBO entityBO = resourceBuilder.buildBOByVO(entityVO);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            resourceService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * Fetch one resource (the permission-grantable unit) by ID.
     *
     * @param id id of the resource to fetch
     * @return the matched ResourceVO; read access is open to all authenticated users
     */
    @PreAuthorize("@perm.can('resource', 'get')")
    @Operation(summary = "Get Resource by ID", description = "Fetch one resource (the permission-grantable unit) by ID. Read access is open to all authenticated users; use to resolve a permission code before binding it to a role.")
    @GetMapping("/get_by_id")
    public Mono<R<ResourceVO>> getById(@Parameter(description = "Primary key of the resource record to fetch; use to resolve a permission code before binding it to a role.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        // Read access to global resource data is open to all authenticated users.
        return async(() -> {
            ResourceBO entityBO = resourceService.getById(id);
            ResourceVO entityVO = resourceBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * Page through resources (the permission-grantable units) with query filters.
     *
     * @param entityQuery optional filter criteria; an empty query pages all resources
     * @return a page of ResourceVO matching the query; read access is open to all authenticated users
     */
    @PreAuthorize("@perm.can('resource', 'list')")
    @Operation(summary = "List Resources", description = "Page through resources (the permission-grantable units) with query filters. Read access is open to all authenticated users; returns a page of resources for browsing or selecting a target.")
    @PostMapping("/list")
    public Mono<R<Page<ResourceVO>>> list(@RequestBody(required = false) ResourceQuery entityQuery) {
        // Read access to global resource data is open to all authenticated users.
        return async(() -> {
            ResourceQuery query = Objects.isNull(entityQuery) ? new ResourceQuery() : entityQuery;
            Page<ResourceBO> entityPageBO = resourceService.list(query);
            Page<ResourceVO> entityPageVO = resourceBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        });
    }

    /**
     * Return resources as a nested tree reflecting the permission hierarchy.
     *
     * @param entityQuery optional filter criteria; an empty query returns the full resource tree
     * @return a tree of ResourceTreeVO mirroring the permission hierarchy; read access is open to all authenticated users
     */
    @PreAuthorize("@perm.can('resource', 'list')")
    @Operation(summary = "List Resource Tree", description = "Return resources as a nested tree reflecting the permission hierarchy (e.g. resource group -> permission code). Read access is open to all authenticated users; use to render permission pickers or inspect parent/child relationships.")
    @PostMapping("/list_tree")
    public Mono<R<List<ResourceTreeVO>>> listTree(@RequestBody(required = false) ResourceQuery entityQuery) {
        // Read access to global resource data is open to all authenticated users.
        return async(() -> {
            List<ResourceTreeBO> entityBOList = resourceService.listTree(entityQuery);
            return R.ok(resourceBuilder.buildTreeVOListByBOList(entityBOList));
        });
    }

}

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

import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.query.ResourceOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.ResourceTreeVO;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.auth.repository.ResourceFilter;
import io.github.pnoker.common.auth.security.ReactiveAdminChecker;
import io.github.pnoker.common.auth.service.ReactiveResourceService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** REST endpoints exposing the global resource registry. */
@Tag(
        name = "resource",
        description = "Protected resource registry: manage API endpoints, menu items, and other securable artifacts")
@RestController
@RequestMapping(AuthConstant.RESOURCE_URL_PREFIX)
@RequiredArgsConstructor
public class ResourceController implements BaseController {

    private final ResourceBuilder resourceBuilder;
    private final ReactiveResourceService resourceService;
    private final ReactiveAdminChecker adminChecker;

    /** Add one resource and return the stored view. */
    @PreAuthorize("@perm.can('resource', 'add')")
    @Operation(
            summary = "Add Resource",
            description = "Create a permission-grantable resource and return the persisted representation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/add")
    public Mono<ResponseEntity<ResourceVO>> add(@Validated(Add.class) @RequestBody ResourceVO entityVO) {
        return getPrincipalHeader()
                .flatMap(header -> adminChecker
                        .assertSystemAdmin(header.getTenantId())
                        .then(Mono.defer(() -> {
                            ResourceBO resource = resourceBuilder.buildBOByVO(entityVO);
                            resource.setCreatorId(header.getUserId());
                            resource.setCreatorName(header.getNickName());
                            resource.setOperatorId(header.getUserId());
                            resource.setOperatorName(header.getNickName());
                            return resourceService.add(resource);
                        }))
                        .map(saved -> ResponseEntity.status(201).body(resourceBuilder.buildVOByBO(saved))));
    }

    /** Delete the resource. */
    @PreAuthorize("@perm.can('resource', 'delete')")
    @Operation(
            summary = "Delete Resource by ID",
            description = "Delete a leaf resource and revoke all active role bindings for it.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                        @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(
            @Parameter(description = "Resource identifier owned by the global registry") @NotNull @RequestParam("id")
                    Long id) {
        return getPrincipalHeader()
                .flatMap(header -> adminChecker
                        .assertSystemAdmin(header.getTenantId())
                        .then(resourceService.delete(id, header.getUserId(), header.getNickName())))
                .thenReturn(ResponseEntity.noContent().build());
    }

    /** Update one resource and emit the updated row. */
    @PreAuthorize("@perm.can('resource', 'update')")
    @Operation(
            summary = "Update Resource",
            description = "Replace a resource definition and return the persisted representation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/update")
    public Mono<ResponseEntity<ResourceVO>> update(@Validated(Update.class) @RequestBody ResourceVO entityVO) {
        return getPrincipalHeader()
                .flatMap(header -> adminChecker
                        .assertSystemAdmin(header.getTenantId())
                        .then(Mono.defer(() -> {
                            ResourceBO resource = resourceBuilder.buildBOByVO(entityVO);
                            resource.setOperatorId(header.getUserId());
                            resource.setOperatorName(header.getNickName());
                            return resourceService.update(resource);
                        }))
                        .map(saved -> ResponseEntity.ok(resourceBuilder.buildVOByBO(saved))));
    }

    /** Resolve the resource by its id. */
    @PreAuthorize("@perm.can('resource', 'get')")
    @Operation(
            summary = "Get Resource by ID",
            description = "Fetch one resource by identifier.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_id")
    public Mono<ResponseEntity<ResourceVO>> getById(
            @Parameter(description = "Resource identifier") @NotNull @RequestParam("id") Long id) {
        return resourceService.getById(id).map(resource -> ResponseEntity.ok(resourceBuilder.buildVOByBO(resource)));
    }

    /** Page resources matching the tenant-scoped filters. */
    @PreAuthorize("@perm.can('resource', 'list')")
    @Operation(
            summary = "List Resources",
            description = "List resources with deterministic zero-based offset pagination.",
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
    public Mono<ResponseEntity<OffsetPage<ResourceVO>>> list(
            @RequestBody(required = false) ResourceOffsetRequest request) {
        ResourceOffsetRequest query = request == null ? new ResourceOffsetRequest() : request;
        ResourceFilter filter = new ResourceFilter(
                query.resourceName(),
                query.resourceCode(),
                query.resourceTypeFlags().isEmpty() && query.resourceTypeFlag() != null
                        ? List.of(query.resourceTypeFlag())
                        : query.resourceTypeFlags(),
                query.resourceScopeFlags().isEmpty() && query.resourceScopeFlag() != null
                        ? List.of(query.resourceScopeFlag())
                        : query.resourceScopeFlags(),
                query.parentResourceId(),
                query.enableFlag(),
                new PageRequest(query.offset(), query.limit(), query.sort()));
        return resourceService
                .list(filter)
                .map(page -> ResponseEntity.ok(OffsetPage.of(
                        page.items().stream().map(resourceBuilder::buildVOByBO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total())));
    }

    /** Emit the resource tree for the tenant. */
    @PreAuthorize("@perm.can('resource', 'list')")
    @Operation(
            summary = "List Resource Tree",
            description = "Return the complete resource hierarchy without pagination truncation.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list_tree")
    public Mono<ResponseEntity<List<ResourceTreeVO>>> listTree(
            @RequestBody(required = false) ResourceOffsetRequest request) {
        ResourceOffsetRequest query = request == null ? new ResourceOffsetRequest() : request;
        ResourceFilter filter = new ResourceFilter(
                query.resourceName(),
                query.resourceCode(),
                query.resourceTypeFlags().isEmpty() && query.resourceTypeFlag() != null
                        ? List.of(query.resourceTypeFlag())
                        : query.resourceTypeFlags(),
                query.resourceScopeFlags().isEmpty() && query.resourceScopeFlag() != null
                        ? List.of(query.resourceScopeFlag())
                        : query.resourceScopeFlags(),
                query.parentResourceId(),
                query.enableFlag(),
                new PageRequest(0, PageRequest.MAX_LIMIT, query.sort()));
        return resourceService
                .listTree(filter)
                .map(resourceBuilder::buildTreeVOByBO)
                .collectList()
                .map(ResponseEntity::ok);
    }
}

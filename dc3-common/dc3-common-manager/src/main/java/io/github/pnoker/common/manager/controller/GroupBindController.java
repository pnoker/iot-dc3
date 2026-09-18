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
import io.github.pnoker.common.manager.entity.bo.GroupBindBO;
import io.github.pnoker.common.manager.entity.builder.GroupBindBuilder;
import io.github.pnoker.common.manager.entity.query.GroupBindListRequest;
import io.github.pnoker.common.manager.entity.vo.GroupBindVO;
import io.github.pnoker.common.manager.repository.BindingFilter;
import io.github.pnoker.common.manager.service.ReactiveEntityTenantService;
import io.github.pnoker.common.manager.service.ReactiveGroupBindService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

/**
 * REST controller exposing group binding management endpoints.
 *
 * @author pnoker
 * @since 2026.5.11
 */
@Tag(
        name = "group_bind",
        description =
                "Group membership bindings: associate devices, drivers, and other entities with logical groups for hierarchical organization and bulk operations")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.GROUP_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class GroupBindController implements BaseController {

    private final GroupBindBuilder groupBindBuilder;

    private final ReactiveGroupBindService groupBindService;

    private final ReactiveEntityTenantService entityTenantService;

    /**
     * Attach a tenant entity to a group.
     *
     * @param entityVO group binding payload to create (group id, entity id, entity type)
     * @return the created group binding
     */
    @PreAuthorize("@perm.can('group_bind', 'add')")
    @Operation(
            summary = "Add Group Binding",
            description =
                    "Attach a tenant entity to a group. The entity type must match the group type and both resources must belong to the tenant; returns the created binding.",
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
    public Mono<ResponseEntity<GroupBindVO>> add(@Validated(Add.class) @RequestBody GroupBindVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    GroupBindBO entityBO = groupBindBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tuple.getT1().getT1());
                    entityBO.setCreatorId(tuple.getT1().getT2());
                    entityBO.setCreatorName(tuple.getT2());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return entityTenantService
                            .requireEntityTenant(
                                    entityBO.getTenantId(), entityBO.getEntityTypeFlag(), entityBO.getEntityId())
                            .then(groupBindService.add(entityBO))
                            .map(groupBindBuilder::buildVOByBO)
                            .map(created ->
                                    ResponseEntity.status(HttpStatus.CREATED).body(created));
                });
    }

    /**
     * Remove a group binding by ID.
     *
     * @param id id of the group binding to delete (must be tenant-owned)
     * @return an empty response after deletion
     */
    @PreAuthorize("@perm.can('group_bind', 'delete')")
    @Operation(
            summary = "Delete Group Binding",
            description = "Permanently remove one tenant-owned group binding by ID.",
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
            @Parameter(
                            description = "Primary key of the entity to delete. Must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> groupBindService
                        .delete(tuple.getT1().getT1(), id, tuple.getT1().getT2(), tuple.getT2())
                        .thenReturn(ResponseEntity.noContent().build()));
    }

    /**
     * Modify an existing group binding.
     *
     * @param entityVO group binding payload to update (must carry an existing id)
     * @return the updated group binding
     */
    @PreAuthorize("@perm.can('group_bind', 'update')")
    @Operation(
            summary = "Update Group Binding",
            description =
                    "Modify an existing group binding for the current tenant. The new group and entity must match the binding's entity type and tenant scope.",
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
    public Mono<ResponseEntity<GroupBindVO>> update(@Validated(Update.class) @RequestBody GroupBindVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    GroupBindBO entityBO = groupBindBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tuple.getT1().getT1());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return entityTenantService
                            .requireEntityTenant(
                                    entityBO.getTenantId(), entityBO.getEntityTypeFlag(), entityBO.getEntityId())
                            .then(groupBindService.update(entityBO))
                            .map(groupBindBuilder::buildVOByBO)
                            .map(ResponseEntity::ok);
                });
    }

    /**
     * Fetch a single group binding by ID.
     *
     * @param id id of the group binding to fetch (must be tenant-owned)
     * @return the matched group binding
     */
    @PreAuthorize("@perm.can('group_bind', 'get')")
    @Operation(
            summary = "Get Group Binding by ID",
            description = "Fetch one group binding by ID for the current tenant.",
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
    public Mono<GroupBindVO> getById(
            @Parameter(
                            description = "Primary key of the target record; must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> groupBindService.getById(tenantId, id))
                .map(groupBindBuilder::buildVOByBO);
    }

    /**
     * Page through group bindings with filters.
     *
     * @param request query filters (may be null)
     * @return a page of group bindings matching the query
     */
    @PreAuthorize("@perm.can('group_bind', 'list')")
    @Operation(
            summary = "List Group Bindings",
            description =
                    "Page through group bindings for the current tenant with optional group, entity type, and entity filters.",
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
    public Mono<OffsetPage<GroupBindVO>> list(@RequestBody(required = false) GroupBindListRequest request) {
        GroupBindListRequest query = request == null ? new GroupBindListRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> groupBindService.list(new BindingFilter(
                        tenantId,
                        query.entityTypeFlag(),
                        query.groupId(),
                        query.entityId(),
                        query.offset(),
                        query.limit(),
                        query.sort())))
                .map(page -> new OffsetPage<>(
                        page.items().stream().map(groupBindBuilder::buildVOByBO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total(),
                        page.hasNext()));
    }
}

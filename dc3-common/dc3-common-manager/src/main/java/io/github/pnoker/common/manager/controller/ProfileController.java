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
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.ProfileBuilder;
import io.github.pnoker.common.manager.entity.query.ProfileOffsetRequest;
import io.github.pnoker.common.manager.entity.vo.ProfileVO;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.manager.service.ReactiveProfileService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * REST controller exposing profile management endpoints.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(
        name = "profile",
        description =
                "Device profile templates: manage reusable configuration bundles that combine driver settings, data point definitions, and command templates for rapid device onboarding")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.PROFILE_URL_PREFIX)
@RequiredArgsConstructor
public class ProfileController implements BaseController {

    private final ProfileBuilder profileBuilder;

    private final ReactiveProfileService reactiveProfileService;

    /**
     * Register a new profile template for the current tenant, then return the add-success status.
     *
     * @param entityVO profile payload to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('profile', 'add')")
    @Operation(
            summary = "Add Profile",
            description =
                    "Register a new profile template for the current tenant. A profile is a reusable template bundling points, commands, events and attributes that devices instantiate; returns the new profile ID.",
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
    public Mono<ProfileVO> add(@Validated(Add.class) @RequestBody ProfileVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    Long tenantId = tuple.getT1().getT1();
                    ProfileBO entityBO = profileBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tenantId);
                    entityBO.setCreatorId(tuple.getT1().getT2());
                    entityBO.setCreatorName(tuple.getT2());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return reactiveProfileService.add(entityBO).map(profileBuilder::buildVOByBO);
                });
    }

    /**
     * Delete a profile template after verifying it belongs to the current tenant, then return the delete-success status.
     *
     * @param id id of the profile to delete
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('profile', 'delete')")
    @Operation(
            summary = "Delete Profile",
            description =
                    "Permanently delete a profile template by ID (tenant-scoped). The profile must belong to the current tenant; deletion cannot be undone.",
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
    public Mono<Void> delete(
            @Parameter(
                            description = "Primary key of the entity to delete. Must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id,
            @Parameter(
                            description = "Current optimistic-lock version required as a deletion precondition.",
                            example = "0")
                    @NotNull
                    @Min(0)
                    @RequestParam("version")
                    Integer version) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> reactiveProfileService
                        .delete(
                                tuple.getT1().getT1(),
                                id,
                                version,
                                tuple.getT1().getT2(),
                                tuple.getT2())
                        .then());
    }

    /**
     * Update an existing profile template after verifying tenant ownership, then return the update-success status.
     *
     * @param entityVO profile payload to update
     * @return update-success status
     */
    @PreAuthorize("@perm.can('profile', 'update')")
    @Operation(
            summary = "Update Profile",
            description =
                    "Modify an existing profile template's metadata (tenant-scoped). Only profile fields in the body change; points, commands, events and attributes are managed on their own endpoints.",
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
    public Mono<ProfileVO> update(@Validated(Update.class) @RequestBody ProfileVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    Long tenantId = tuple.getT1().getT1();
                    ProfileBO entityBO = profileBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tenantId);
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return reactiveProfileService.update(entityBO).map(profileBuilder::buildVOByBO);
                });
    }

    /**
     * Fetch one profile template by ID after verifying it belongs to the current tenant.
     *
     * @param id id of the profile to fetch
     * @return the matched ProfileVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('profile', 'get')")
    @Operation(
            summary = "Get Profile by ID",
            description =
                    "Fetch one profile template by ID (tenant-scoped). Use to inspect a profile's metadata before binding devices or editing its point, command and event definitions.",
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
    public Mono<ProfileVO> getById(
            @Parameter(
                            description = "Primary key of the target record; must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id) {
        return getTenantId()
                .flatMap(
                        tenantId -> reactiveProfileService.getById(tenantId, id).map(profileBuilder::buildVOByBO));
    }

    /**
     * Resolve a set of profile IDs to their templates, filtered to the current tenant.
     *
     * @param profileIds ids of the profiles to resolve
     * @return a map of id to ProfileVO for the tenant-owned matched ids
     */
    @PreAuthorize("@perm.can('profile', 'list')")
    @Operation(
            summary = "List Profiles by IDs",
            description =
                    "Resolve a set of profile IDs to their profile templates for the current tenant. Returns a map of ID to profile; missing or foreign-tenant IDs are omitted.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list_by_ids")
    public Mono<Map<String, ProfileVO>> listByIds(@RequestBody Set<Long> profileIds) {
        return getTenantId()
                .flatMap(tenantId -> reactiveProfileService
                        .listByIds(tenantId, profileIds == null ? List.of() : List.copyOf(profileIds))
                        .collectMap(bo -> String.valueOf(bo.getId()), profileBuilder::buildVOByBO));
    }

    /**
     * List every profile template instantiated by a given device, filtered to the current tenant.
     *
     * @param deviceId id of the device whose profiles are returned
     * @return a list of ProfileVO instantiated by the device
     */
    @PreAuthorize("@perm.can('profile', 'list')")
    @Operation(
            summary = "List Profiles by Device ID",
            description =
                    "Return every profile template instantiated by a given device (tenant-scoped). Use to discover which point, command and event definitions a device exposes through its driver.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list_by_device_id")
    public Mono<List<ProfileVO>> listByDeviceId(
            @Parameter(
                            description =
                                    "Identifier of the device whose instantiated profiles are returned; must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "device_id")
                    Long deviceId) {
        return getTenantId()
                .flatMap(tenantId -> reactiveProfileService
                        .listByDeviceId(tenantId, deviceId)
                        .map(profileBuilder::buildVOByBO)
                        .collectList());
    }

    /**
     * Page through profile templates for the current tenant using the supplied query filters.
     *
     * @param request     optional query filters; a new query is used when null
     * @return a page of ProfileVO matching the query
     */
    @PreAuthorize("@perm.can('profile', 'list')")
    @Operation(
            summary = "List Profiles",
            description =
                    "Page through profile templates for the current tenant with the filters in the query body. Returns a page of profiles; use for browsing or selecting a template for a device.",
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
    public Mono<OffsetPage<ProfileVO>> list(@RequestBody(required = false) ProfileOffsetRequest request) {
        ProfileOffsetRequest query = request == null ? new ProfileOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> reactiveProfileService
                        .list(new ProfileFilter(
                                tenantId,
                                query.profileName(),
                                query.profileCode(),
                                query.profileShareFlag(),
                                query.profileTypeFlag(),
                                query.enableFlag(),
                                query.groupId(),
                                query.labelId(),
                                query.version(),
                                query.deviceId(),
                                query.offset(),
                                query.limit(),
                                query.sort()))
                        .map(page -> OffsetPage.of(
                                page.items().stream()
                                        .map(profileBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total())));
    }
}

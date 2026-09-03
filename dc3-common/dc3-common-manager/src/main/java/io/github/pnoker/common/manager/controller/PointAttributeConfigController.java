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
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.PointAttributeConfigOffsetRequest;
import io.github.pnoker.common.manager.entity.vo.PointAttributeConfigVO;
import io.github.pnoker.common.manager.repository.PointAttributeConfigFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeService;
import io.github.pnoker.common.manager.service.ReactivePointService;
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
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** REST controller exposing point attribute config endpoints. */
@Tag(name = "point_attribute_config", description = "Per-device point attribute configuration")
@RestController
@RequestMapping(ManagerConstant.POINT_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class PointAttributeConfigController implements BaseController {
    private final PointAttributeConfigBuilder builder;
    private final ReactivePointAttributeConfigService configService;
    private final ReactivePointAttributeService attributeService;
    private final ReactivePointService pointService;
    private final ReactiveDeviceService deviceService;

    /** Add one point attribute config and return the stored view. */
    @PreAuthorize("@perm.can('point_attribute_config', 'add')")
    @Operation(
            summary = "Add Point Attribute Configuration",
            description = "Set a point attribute value for one device and point in the current tenant.",
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
    public Mono<PointAttributeConfigVO> add(@Validated(Add.class) @RequestBody PointAttributeConfigVO request) {
        return principal()
                .zipWith(getTenantId())
                .map(tuple -> {
                    PointAttributeConfigBO value = builder.buildBOByVO(request);
                    value.setTenantId(tuple.getT2());
                    value.setCreatorId(tuple.getT1().id());
                    value.setCreatorName(tuple.getT1().name());
                    value.setOperatorId(tuple.getT1().id());
                    value.setOperatorName(tuple.getT1().name());
                    return value;
                })
                .flatMap(configService::add)
                .map(builder::buildVOByBO);
    }

    /** Delete the point attribute config. */
    @PreAuthorize("@perm.can('point_attribute_config', 'delete')")
    @Operation(
            summary = "Delete Point Attribute Configuration",
            description = "Delete one point attribute configuration in the current tenant.",
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
            @Parameter(description = "Configuration identifier scoped to the current tenant.")
                    @NotNull
                    @RequestParam("id")
                    Long id,
            @Parameter(
                            description = "Current optimistic-lock version required as a deletion precondition.",
                            example = "0")
                    @NotNull
                    @Min(0)
                    @RequestParam("version")
                    Integer version) {
        return principal()
                .zipWith(getTenantId())
                .flatMap(tuple -> configService
                        .delete(
                                tuple.getT2(),
                                id,
                                version,
                                tuple.getT1().id(),
                                tuple.getT1().name())
                        .then());
    }

    /** Update one point attribute config and emit the updated row. */
    @PreAuthorize("@perm.can('point_attribute_config', 'update')")
    @Operation(
            summary = "Update Point Attribute Configuration",
            description = "Update a point attribute value with optimistic locking in the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PatchMapping("/update")
    public Mono<PointAttributeConfigVO> update(@Validated(Update.class) @RequestBody PointAttributeConfigVO request) {
        return principal()
                .zipWith(getTenantId())
                .map(tuple -> {
                    PointAttributeConfigBO value = builder.buildBOByVO(request);
                    value.setTenantId(tuple.getT2());
                    value.setOperatorId(tuple.getT1().id());
                    value.setOperatorName(tuple.getT1().name());
                    return value;
                })
                .flatMap(configService::update)
                .map(builder::buildVOByBO);
    }

    /** Resolve the point attribute config by its id. */
    @PreAuthorize("@perm.can('point_attribute_config', 'get')")
    @Operation(
            summary = "Get Point Attribute Configuration",
            description = "Fetch one point attribute configuration by identifier in the current tenant.",
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
    public Mono<PointAttributeConfigVO> getById(
            @Parameter(description = "Configuration identifier scoped to the current tenant.")
                    @NotNull
                    @RequestParam("id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> configService.getById(tenantId, id).map(builder::buildVOByBO));
    }

    /** Resolve the point attribute config by its tuple. */
    @PreAuthorize("@perm.can('point_attribute_config', 'get')")
    @Operation(
            summary = "Get Point Attribute Configuration by Device and Point",
            description =
                    "Fetch a point attribute configuration by device, point, and attribute identifiers in the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_attribute_id_and_device_id_and_point_id")
    public Mono<PointAttributeConfigVO> getByTuple(
            @Parameter(description = "Point attribute identifier scoped to the current tenant.")
                    @RequestParam("attribute_id")
                    Long attributeId,
            @Parameter(description = "Device identifier scoped to the current tenant.") @RequestParam("device_id")
                    Long deviceId,
            @Parameter(description = "Point identifier scoped to the current tenant.") @RequestParam("point_id")
                    Long pointId) {
        return getTenantId()
                .flatMap(tenantId -> validateRelations(tenantId, deviceId, pointId, attributeId)
                        .then(configService.getByAttributeIdAndDeviceIdAndPointId(
                                tenantId, attributeId, deviceId, pointId))
                        .map(builder::buildVOByBO));
    }

    /** List point attribute configs matched by device id. */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(
            summary = "List Point Attribute Configurations by Device",
            description = "List configurations attached to one device in the current tenant.",
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
    public Mono<List<PointAttributeConfigVO>> listByDeviceId(
            @Parameter(description = "Device identifier scoped to the current tenant.") @RequestParam("device_id")
                    Long deviceId) {
        return getTenantId()
                .flatMap(tenantId -> deviceService
                        .getById(tenantId, deviceId)
                        .thenMany(configService.listByDeviceId(tenantId, deviceId))
                        .map(builder::buildVOByBO)
                        .collectList());
    }

    /** List point attribute configs matched by device id and point id. */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(
            summary = "List Point Attribute Configurations by Device and Point",
            description = "List configurations attached to one device and point in the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list_by_device_id_and_point_id")
    public Mono<List<PointAttributeConfigVO>> listByDeviceIdAndPointId(
            @Parameter(description = "Device identifier scoped to the current tenant.") @RequestParam("device_id")
                    Long deviceId,
            @Parameter(description = "Point identifier scoped to the current tenant.") @RequestParam("point_id")
                    Long pointId) {
        return getTenantId()
                .flatMap(tenantId -> validateDevicePoint(tenantId, deviceId, pointId)
                        .thenMany(configService.listByDeviceIdAndPointId(tenantId, deviceId, pointId))
                        .map(builder::buildVOByBO)
                        .collectList());
    }

    /** Page point attribute configs matching the tenant-scoped filters. */
    @PreAuthorize("@perm.can('point_attribute_config', 'list')")
    @Operation(
            summary = "List Point Attribute Configurations",
            description = "Page through point attribute configurations using offset and limit in the current tenant.",
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
    public Mono<OffsetPage<PointAttributeConfigVO>> list(
            @RequestBody(required = false) PointAttributeConfigOffsetRequest request) {
        PointAttributeConfigOffsetRequest query = request == null ? new PointAttributeConfigOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> configService
                        .list(new PointAttributeConfigFilter(
                                tenantId,
                                query.attributeId(),
                                query.deviceId(),
                                query.pointId(),
                                query.enableFlag(),
                                query.version(),
                                query.offset(),
                                query.limit(),
                                query.sort()))
                        .map(page -> OffsetPage.of(
                                page.items().stream().map(builder::buildVOByBO).toList(),
                                page.offset(),
                                page.limit(),
                                page.total())));
    }

    private Mono<Void> validateRelations(Long tenantId, Long deviceId, Long pointId, Long attributeId) {
        return Mono.defer(() ->
                        Mono.zip(deviceService.getById(tenantId, deviceId), pointService.getById(tenantId, pointId)))
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .flatMap(tuple -> {
                    DevicePoint relation = new DevicePoint(tuple.getT1(), tuple.getT2());
                    if (!Objects.equals(
                            relation.device().getProfileId(), relation.point().getProfileId()))
                        return Mono.error(new NotFoundException("Resource does not exist"));
                    return attributeService
                            .getById(tenantId, attributeId)
                            .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                            .flatMap(attribute -> Objects.equals(
                                            attribute.getDriverId(),
                                            relation.device().getDriverId())
                                    ? Mono.empty()
                                    : Mono.error(new NotFoundException("Resource does not exist")));
                });
    }

    private Mono<Void> validateDevicePoint(Long tenantId, Long deviceId, Long pointId) {
        return Mono.defer(() ->
                        Mono.zip(deviceService.getById(tenantId, deviceId), pointService.getById(tenantId, pointId)))
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .flatMap(tuple -> Objects.equals(
                                tuple.getT1().getProfileId(), tuple.getT2().getProfileId())
                        ? Mono.empty()
                        : Mono.error(new NotFoundException("Resource does not exist")));
    }

    private Mono<Principal> principal() {
        return Mono.zip(getUserId().defaultIfEmpty(0L), getUserName().defaultIfEmpty(""), Principal::new);
    }

    private record Principal(Long id, String name) {}

    private record DevicePoint(
            io.github.pnoker.common.manager.entity.bo.DeviceBO device,
            io.github.pnoker.common.manager.entity.bo.PointBO point) {}
}

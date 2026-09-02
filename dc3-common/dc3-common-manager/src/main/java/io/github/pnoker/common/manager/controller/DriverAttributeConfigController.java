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
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.DriverAttributeConfigOffsetRequest;
import io.github.pnoker.common.manager.entity.vo.DriverAttributeConfigVO;
import io.github.pnoker.common.manager.repository.DriverAttributeConfigFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveDriverAttributeService;
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

@Tag(name = "driver_attribute_config", description = "Per-device driver attribute configuration")
@RestController
@RequestMapping(ManagerConstant.DRIVER_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class DriverAttributeConfigController implements BaseController {
    private final DriverAttributeConfigBuilder builder;
    private final ReactiveDriverAttributeConfigService configService;
    private final ReactiveDriverAttributeService attributeService;
    private final ReactiveDeviceService deviceService;

    @PreAuthorize("@perm.can('driver_attribute_config', 'add')")
    @Operation(
            summary = "Add Driver Attribute Configuration",
            description = "Set a driver attribute value for one device in the current tenant.",
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
    public Mono<DriverAttributeConfigVO> add(@Validated(Add.class) @RequestBody DriverAttributeConfigVO request) {
        return principal()
                .zipWith(getTenantId())
                .map(tuple -> {
                    DriverAttributeConfigBO value = builder.buildBOByVO(request);
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

    @PreAuthorize("@perm.can('driver_attribute_config', 'delete')")
    @Operation(
            summary = "Delete Driver Attribute Configuration",
            description = "Delete one driver attribute configuration by identifier in the current tenant.",
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

    @PreAuthorize("@perm.can('driver_attribute_config', 'update')")
    @Operation(
            summary = "Update Driver Attribute Configuration",
            description = "Update a driver attribute value with optimistic locking in the current tenant.",
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
    public Mono<DriverAttributeConfigVO> update(@Validated(Update.class) @RequestBody DriverAttributeConfigVO request) {
        return principal()
                .zipWith(getTenantId())
                .map(tuple -> {
                    DriverAttributeConfigBO value = builder.buildBOByVO(request);
                    value.setTenantId(tuple.getT2());
                    value.setOperatorId(tuple.getT1().id());
                    value.setOperatorName(tuple.getT1().name());
                    return value;
                })
                .flatMap(configService::update)
                .map(builder::buildVOByBO);
    }

    @PreAuthorize("@perm.can('driver_attribute_config', 'get')")
    @Operation(
            summary = "Get Driver Attribute Configuration",
            description = "Fetch one driver attribute configuration by identifier in the current tenant.",
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
    public Mono<DriverAttributeConfigVO> getById(
            @Parameter(description = "Configuration identifier scoped to the current tenant.")
                    @NotNull
                    @RequestParam("id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> configService.getById(tenantId, id).map(builder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('driver_attribute_config', 'get')")
    @Operation(
            summary = "Get Driver Attribute Configuration by Device and Attribute",
            description =
                    "Fetch a driver attribute configuration by device and attribute identifiers in the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_device_id_and_attribute_id")
    public Mono<DriverAttributeConfigVO> getByDeviceIdAndAttributeId(
            @Parameter(description = "Device identifier scoped to the current tenant.") @RequestParam("device_id")
                    Long deviceId,
            @Parameter(description = "Driver attribute identifier scoped to the current tenant.")
                    @RequestParam("attribute_id")
                    Long attributeId) {
        return getTenantId()
                .flatMap(tenantId -> validateRelations(tenantId, deviceId, attributeId)
                        .then(configService.getByAttributeIdAndDeviceId(tenantId, attributeId, deviceId))
                        .map(builder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('driver_attribute_config', 'list')")
    @Operation(
            summary = "List Driver Attribute Configurations by Device",
            description = "List driver attribute configurations attached to one device in the current tenant.",
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
    public Mono<List<DriverAttributeConfigVO>> listByDeviceId(
            @Parameter(description = "Device identifier scoped to the current tenant.") @RequestParam("device_id")
                    Long deviceId) {
        return getTenantId()
                .flatMap(tenantId -> deviceService
                        .getById(tenantId, deviceId)
                        .thenMany(configService.listByDeviceId(tenantId, deviceId))
                        .map(builder::buildVOByBO)
                        .collectList());
    }

    @PreAuthorize("@perm.can('driver_attribute_config', 'list')")
    @Operation(
            summary = "List Driver Attribute Configurations",
            description = "Page through driver attribute configurations using offset and limit in the current tenant.",
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
    public Mono<OffsetPage<DriverAttributeConfigVO>> list(
            @RequestBody(required = false) DriverAttributeConfigOffsetRequest request) {
        DriverAttributeConfigOffsetRequest query = request == null ? new DriverAttributeConfigOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> configService
                        .list(new DriverAttributeConfigFilter(
                                tenantId,
                                query.attributeId(),
                                query.deviceId(),
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

    private Mono<Void> validateRelations(Long tenantId, Long deviceId, Long attributeId) {
        return deviceService
                .getById(tenantId, deviceId)
                .zipWith(attributeService.getById(tenantId, attributeId))
                .flatMap(tuple -> Objects.equals(
                                tuple.getT1().getDriverId(), tuple.getT2().getDriverId())
                        ? Mono.empty()
                        : Mono.error(new NotFoundException("Resource does not exist")));
    }

    private Mono<Principal> principal() {
        return Mono.zip(getUserId().defaultIfEmpty(0L), getUserName().defaultIfEmpty(""), Principal::new);
    }

    private record Principal(Long id, String name) {}
}

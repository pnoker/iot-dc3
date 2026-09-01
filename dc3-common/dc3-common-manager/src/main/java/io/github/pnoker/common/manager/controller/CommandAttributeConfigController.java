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
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.builder.CommandAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.CommandAttributeConfigOffsetRequest;
import io.github.pnoker.common.manager.entity.vo.CommandAttributeConfigVO;
import io.github.pnoker.common.manager.repository.CommandAttributeConfigFilter;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveCommandAttributeService;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Tag(name = "command_attribute_config", description = "Per-device command attribute configuration")
@RestController
@RequestMapping(ManagerConstant.COMMAND_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class CommandAttributeConfigController implements BaseController {
    private final CommandAttributeConfigBuilder builder;
    private final ReactiveCommandAttributeConfigService configService;
    private final ReactiveCommandAttributeService attributeService;
    private final ReactiveCommandService commandService;
    private final ReactiveDeviceService deviceService;

    @PreAuthorize("@perm.can('command_attribute_config', 'add')")
    @Operation(summary = "Add Command Attribute Configuration", description = "Set a command attribute value for one device and command in the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "false"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @PostMapping("/add")
    public Mono<CommandAttributeConfigVO> add(@Validated(Add.class) @RequestBody CommandAttributeConfigVO request) {
        return principal().zipWith(getTenantId()).map(tuple -> {
            CommandAttributeConfigBO value = builder.buildBOByVO(request);
            value.setTenantId(tuple.getT2()); value.setCreatorId(tuple.getT1().id()); value.setCreatorName(tuple.getT1().name()); value.setOperatorId(tuple.getT1().id()); value.setOperatorName(tuple.getT1().name()); return value;
        }).flatMap(configService::add).map(builder::buildVOByBO);
    }

    @PreAuthorize("@perm.can('command_attribute_config', 'delete')")
    @Operation(summary = "Delete Command Attribute Configuration", description = "Delete one command attribute configuration in the current tenant by identifier.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "HIGH"), @ExtensionProperty(name = "destructive", value = "true"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @DeleteMapping("/delete")
    public Mono<Void> delete(@Parameter(description = "Configuration identifier scoped to the current tenant.") @NotNull @RequestParam("id") Long id,
                             @Parameter(description = "Current optimistic-lock version required as a deletion precondition.", example = "0") @NotNull @Min(0) @RequestParam("version") Integer version) {
        return principal().zipWith(getTenantId()).flatMap(tuple -> configService.delete(tuple.getT2(), id, version, tuple.getT1().id(), tuple.getT1().name()).then());
    }

    @PreAuthorize("@perm.can('command_attribute_config', 'update')")
    @Operation(summary = "Update Command Attribute Configuration", description = "Update a command attribute value with optimistic locking in the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @PatchMapping("/update")
    public Mono<CommandAttributeConfigVO> update(@Validated(Update.class) @RequestBody CommandAttributeConfigVO request) {
        return principal().zipWith(getTenantId()).map(tuple -> {
            CommandAttributeConfigBO value = builder.buildBOByVO(request); value.setTenantId(tuple.getT2()); value.setOperatorId(tuple.getT1().id()); value.setOperatorName(tuple.getT1().name()); return value;
        }).flatMap(configService::update).map(builder::buildVOByBO);
    }

    @PreAuthorize("@perm.can('command_attribute_config', 'get')")
    @Operation(summary = "Get Command Attribute Configuration", description = "Fetch one command attribute configuration by identifier in the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @GetMapping("/get_by_id")
    public Mono<CommandAttributeConfigVO> getById(@Parameter(description = "Configuration identifier scoped to the current tenant.") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenantId -> configService.getById(tenantId, id).map(builder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('command_attribute_config', 'get')")
    @Operation(summary = "Get Command Attribute Configuration by Attribute, Device and Command", description = "Fetch a configuration by attribute, device, and command identifiers in the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @GetMapping("/get_by_attribute_id_and_device_id_and_command_id")
    public Mono<CommandAttributeConfigVO> getByAttributeIdAndDeviceIdAndCommandId(
            @Parameter(description = "Command attribute identifier scoped to the current tenant.") @RequestParam("attribute_id") Long attributeId,
            @Parameter(description = "Device identifier scoped to the current tenant.") @RequestParam("device_id") Long deviceId,
            @Parameter(description = "Command identifier scoped to the current tenant.") @RequestParam("command_id") Long commandId) {
        return getTenantId().flatMap(tenantId -> validateRelations(tenantId, attributeId, deviceId, commandId).then(configService.getByAttributeIdAndDeviceIdAndCommandId(tenantId, attributeId, deviceId, commandId)).map(builder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @Operation(summary = "List Command Attribute Configurations by Device and Command", description = "List configurations for one device and command pair in the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @GetMapping("/list_by_device_id_and_command_id")
    public Mono<List<CommandAttributeConfigVO>> listByDeviceIdAndCommandId(
            @Parameter(description = "Device identifier scoped to the current tenant.") @RequestParam("device_id") Long deviceId,
            @Parameter(description = "Command identifier scoped to the current tenant.") @RequestParam("command_id") Long commandId) {
        return getTenantId().flatMap(tenantId -> validateRelations(tenantId, null, deviceId, commandId).thenMany(configService.listByDeviceIdAndCommandId(tenantId, deviceId, commandId)).map(builder::buildVOByBO).collectList());
    }

    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @Operation(summary = "List Command Attribute Configurations by Device", description = "List configurations attached to one device in the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @GetMapping("/list_by_device_id")
    public Mono<List<CommandAttributeConfigVO>> listByDeviceId(@Parameter(description = "Device identifier scoped to the current tenant.") @RequestParam("device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> deviceService.getById(tenantId, deviceId).thenMany(configService.listByDeviceId(tenantId, deviceId)).map(builder::buildVOByBO).collectList());
    }

    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @Operation(summary = "List Command Attribute Configurations", description = "Page through command attribute configurations using offset and limit in the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @PostMapping("/list")
    public Mono<OffsetPage<CommandAttributeConfigVO>> list(@RequestBody(required = false) CommandAttributeConfigOffsetRequest request) {
        CommandAttributeConfigOffsetRequest query = request == null ? new CommandAttributeConfigOffsetRequest() : request;
        return getTenantId().flatMap(tenantId -> configService.list(new CommandAttributeConfigFilter(tenantId, query.attributeId(), query.deviceId(), query.commandId(), query.enableFlag(), query.version(), query.offset(), query.limit(), query.sort())).map(page -> OffsetPage.of(page.items().stream().map(builder::buildVOByBO).toList(), page.offset(), page.limit(), page.total())));
    }

    private Mono<Void> validateRelations(Long tenantId, Long attributeId, Long deviceId, Long commandId) {
        return Mono.defer(() -> deviceService.getById(tenantId, deviceId).zipWith(commandService.getById(tenantId, commandId)))
                .flatMap(tuple -> {
                    if (!Objects.equals(tuple.getT1().getProfileId(), tuple.getT2().getProfileId())) {
                        return Mono.error(new NotFoundException("Resource does not exist"));
                    }
                    if (attributeId == null) {
                        return Mono.empty();
                    }
                    return attributeService.getById(tenantId, attributeId).flatMap(attribute ->
                            Objects.equals(attribute.getDriverId(), tuple.getT1().getDriverId())
                                    ? Mono.empty()
                                    : Mono.error(new NotFoundException("Resource does not exist")));
                });
    }
    private Mono<Principal> principal() { return Mono.zip(getUserId().defaultIfEmpty(0L), getUserName().defaultIfEmpty(""), Principal::new); }
    private record Principal(Long id, String name) { }
}

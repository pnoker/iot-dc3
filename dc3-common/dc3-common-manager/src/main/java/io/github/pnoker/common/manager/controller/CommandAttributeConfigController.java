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
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.builder.CommandAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.CommandAttributeConfigQuery;
import io.github.pnoker.common.manager.entity.vo.CommandAttributeConfigVO;
import io.github.pnoker.common.manager.service.CommandAttributeConfigService;
import io.github.pnoker.common.manager.service.CommandAttributeService;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.manager.service.DeviceService;
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

import java.util.List;
import java.util.Objects;

/**
 * Manages per-device command attribute configuration values that override the defaults declared on a profile template's command attributes.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "command_attribute_config", description = "Command attribute configuration values: set and update per-device customization values for command properties inherited from command attribute definitions")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.COMMAND_ATTRIBUTE_CONFIG_URL_PREFIX)
@RequiredArgsConstructor
public class CommandAttributeConfigController implements BaseController {

    private final CommandAttributeConfigBuilder commandAttributeConfigBuilder;

    private final CommandAttributeConfigService commandAttributeConfigService;

    private final DeviceService deviceService;

    private final CommandService commandService;

    private final CommandAttributeService commandAttributeService;

    /**
     * Set the concrete value of a command attribute field for a specific device and command, overriding the profile template default.
     *
     * @param entityVO command attribute config payload to create (attribute, device, command and configured value)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'add')")
    @Operation(summary = "Add Command Attribute Configuration", description = "Set the concrete value of a command attribute field for a specific device and command under the current tenant. The configured value overrides the field's default declared on the profile template; returns the new configuration ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody CommandAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeConfigBO entityBO = commandAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            commandAttributeConfigService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Permanently delete one command attribute configuration by ID, scoped to the current tenant.
     *
     * @param id id of the command attribute config to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'delete')")
    @Operation(summary = "Delete Command Attribute Configuration", description = "Permanently delete one command attribute configuration by ID (tenant-scoped). The device reverts to the attribute's default declared on the profile template; the deletion cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandAttributeConfigService.getById(id));
            commandAttributeConfigService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Change the configured value of an existing command attribute for a specific device and command, scoped to the current tenant.
     *
     * @param entityVO command attribute config payload carrying the updated value; ownership is verified before applying
     * @return update-success status
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'update')")
    @Operation(summary = "Update Command Attribute Configuration", description = "Change the configured value of an existing command attribute for a specific device and command. Verifies the record belongs to the current tenant before applying the update.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody CommandAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeConfigBO entityBO = commandAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, commandAttributeConfigService.getById(entityBO.getId()));
            commandAttributeConfigService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one command attribute configuration by its record ID, scoped to the current tenant.
     *
     * @param id id of the command attribute config to fetch; must belong to the current tenant
     * @return the matched CommandAttributeConfigVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'get')")
    @Operation(summary = "Get Command Attribute Configuration by ID", description = "Fetch one command attribute configuration by its record ID. Use to inspect the value a specific device applies to a command's attribute field, scoped to the current tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<CommandAttributeConfigVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeConfigBO entityBO = requireTenant(tenantId, commandAttributeConfigService.getById(id));
            CommandAttributeConfigVO entityVO = commandAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Fetch the configured value of one command attribute field by its attribute, device and command IDs, scoped to the current tenant.
     *
     * @param attributeId id of the command attribute whose configured value is looked up
     * @param deviceId    id of the device whose configured command attribute value is looked up
     * @param commandId   id of the command whose attribute configuration is looked up
     * @return the matched CommandAttributeConfigVO; fails if the device/command/attribute triple is invalid or not tenant-owned
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'get')")
    @Operation(summary = "Get Command Attribute Configuration by Attribute, Device, and Command IDs", description = "Fetch the configured value of one command attribute field by its attribute, device and command IDs. Use when the configuration ID is unknown but the attribute-device-command triple is, scoped to the current tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_attribute_id_and_device_id_and_command_id")
    public Mono<R<CommandAttributeConfigVO>> getByAttributeIdAndDeviceIdAndCommandId(
            @Parameter(description = "Identifier of the command attribute field whose configured value is being looked up; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "attribute_id") Long attributeId,
            @Parameter(description = "Identifier of the device whose configured command attribute value is being looked up; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId,
            @Parameter(description = "Identifier of the command whose attribute configuration is being looked up; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "command_id") Long commandId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireCommandConfigRelations(tenantId, deviceId, commandId, attributeId);
            CommandAttributeConfigBO entityBO = commandAttributeConfigService
                    .getByAttributeIdAndDeviceIdAndCommandId(attributeId, deviceId, commandId);
            requireTenant(tenantId, entityBO);
            CommandAttributeConfigVO entityVO = commandAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Return every command attribute configuration set on a specific device for a specific command, scoped to the current tenant.
     *
     * @param deviceId  id of the device whose command attribute configurations are listed
     * @param commandId id of the command whose attribute configurations are listed
     * @return a list of CommandAttributeConfigVO for the device-command pair
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @Operation(summary = "List Command Attribute Configurations by Device and Command IDs", description = "Return every command attribute configuration set on a specific device for a specific command, scoped to the current tenant. Use to retrieve the full set of values that device will send with that command.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id_and_command_id")
    public Mono<R<List<CommandAttributeConfigVO>>> listByDeviceIdAndCommandId(
            @Parameter(description = "Identifier of the device whose command attribute configurations are being listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId,
            @Parameter(description = "Identifier of the command whose attribute configurations are being listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "command_id") Long commandId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireCommandConfigRelations(tenantId, deviceId, commandId, null);
            List<CommandAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    commandAttributeConfigService.listByDeviceIdAndCommandId(deviceId, commandId));
            List<CommandAttributeConfigVO> entityVOList = commandAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Return every command attribute configuration set on a specific device across all of its commands, scoped to the current tenant.
     *
     * @param deviceId id of the device whose command attribute configurations are listed
     * @return a list of CommandAttributeConfigVO set on the device
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @Operation(summary = "List Command Attribute Configurations by Device ID", description = "Return every command attribute configuration set on a specific device across all of its commands, scoped to the current tenant. Use to review the full configuration of a device before sending commands.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id")
    public Mono<R<List<CommandAttributeConfigVO>>> listByDeviceId(
            @Parameter(description = "Identifier of the device whose command attribute configurations are being listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<CommandAttributeConfigBO> entityBOList = filterTenant(tenantId,
                    commandAttributeConfigService.listByDeviceId(deviceId));
            List<CommandAttributeConfigVO> entityVOList = commandAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Page through command attribute configurations for the current tenant with filters from the query body.
     *
     * @param entityQuery optional query filters; null treated as empty
     * @return a page of CommandAttributeConfigVO matching the query
     */
    @PreAuthorize("@perm.can('command_attribute_config', 'list')")
    @Operation(summary = "List Command Attribute Configurations", description = "Page through command attribute configurations for the current tenant with filters from the query body. Returns a page of configurations; use for browsing or auditing configured command attribute values across devices and commands.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<CommandAttributeConfigVO>>> list(
            @RequestBody(required = false) CommandAttributeConfigQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeConfigQuery query = Objects.isNull(entityQuery) ? new CommandAttributeConfigQuery()
                    : entityQuery;
            query.setTenantId(tenantId);
            Page<CommandAttributeConfigBO> entityPageBO = commandAttributeConfigService.list(query);
            Page<CommandAttributeConfigVO> entityPageVO = commandAttributeConfigBuilder
                    .buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * Validate that device, command, and (optional) attribute belong to the tenant,
     * share a profile, and that the attribute's driver matches the device's driver.
     *
     * @param tenantId    tenant scope
     * @param deviceId    the device to validate
     * @param commandId   the command to validate
     * @param attributeId the attribute to validate, may be null to skip
     */
    private void requireCommandConfigRelations(Long tenantId, Long deviceId, Long commandId, Long attributeId) {
        DeviceBO deviceBO = requireTenant(tenantId, deviceService.getById(deviceId));
        CommandBO commandBO = requireTenant(tenantId, commandService.getById(commandId));
        if (Objects.isNull(deviceBO.getProfileId()) || !Objects.equals(deviceBO.getProfileId(), commandBO.getProfileId())) {
            throw new NotFoundException("Resource does not exist");
        }

        if (Objects.nonNull(attributeId)) {
            CommandAttributeBO attributeBO = requireTenant(tenantId, commandAttributeService.getById(attributeId));
            if (!Objects.equals(deviceBO.getDriverId(), attributeBO.getDriverId())) {
                throw new NotFoundException("Resource does not exist");
            }
        }
    }

}

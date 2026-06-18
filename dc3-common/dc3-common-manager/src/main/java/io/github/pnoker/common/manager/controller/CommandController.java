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
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.builder.CommandBuilder;
import io.github.pnoker.common.manager.entity.query.CommandQuery;
import io.github.pnoker.common.manager.entity.vo.CommandVO;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.ProfileService;
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
 * Manages device command definitions declared on profile templates, including the downward control instructions a driver sends to devices.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "command", description = "Device command definitions: manage industrial device operations including read, write, and configuration commands with parameter specifications")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.COMMAND_URL_PREFIX)
@RequiredArgsConstructor
public class CommandController implements BaseController {

    private final CommandBuilder commandBuilder;

    private final CommandService commandService;

    private final ProfileService profileService;

    private final DeviceService deviceService;

    /**
     * Create a downward control instruction defined on a profile template for the current tenant.
     *
     * @param entityVO command payload to create, carrying its parameters and attributes
     * @return the id of the newly created command
     */
    @PreAuthorize("@perm.can('command', 'add')")
    @Operation(summary = "Add Command", description = "Create a downward control instruction defined on a profile template for the current tenant. " +
            "A command carries parameters and attributes that the driver sends to a device; returns the new command ID.")
    @PostMapping("/add")
    public Mono<R<Long>> add(@Validated(Add.class) @RequestBody CommandVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandBO entityBO = commandBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            commandService.add(entityBO);
            return R.ok(entityBO.getId());
        }));
    }

    /**
     * Permanently delete a command by ID, scoped to the current tenant.
     *
     * @param id id of the command to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('command', 'delete')")
    @Operation(summary = "Delete Command", description = "Permanently delete a command by ID (tenant-scoped). " +
            "Removes the command definition from its profile template; the action cannot be undone.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandService.getById(id));
            commandService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * Modify an existing command's parameters and attributes, scoped to the current tenant.
     *
     * @param entityVO command payload carrying the updated fields; ownership is verified before applying
     * @return update-success status
     */
    @PreAuthorize("@perm.can('command', 'update')")
    @Operation(summary = "Update Command", description = "Modify an existing command's parameters and attributes (tenant-scoped). " +
            "Ownership is verified before applying changes; returns an update-success response.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody CommandVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandBO entityBO = commandBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, commandService.getById(entityBO.getId()));
            commandService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * Fetch one command with its parameters and attributes, scoped to the current tenant.
     *
     * @param id id of the command to fetch; must belong to the current tenant
     * @return the matched CommandVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('command', 'get')")
    @Operation(summary = "Get Command by ID", description = "Fetch one command with its parameters and attributes (tenant-scoped). " +
            "Use to inspect a command before sending it to a device through the driver.")
    @GetMapping("/get_by_id")
    public Mono<R<CommandVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandBO entityBO = requireTenant(tenantId, commandService.getById(id));
            CommandVO entityVO = commandBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Return every control command declared on a given profile template, scoped to the current tenant.
     *
     * @param profileId id of the profile template whose commands are returned; must belong to the current tenant
     * @return a list of CommandVO declared on the profile
     */
    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands by Profile ID", description = "Return every control command declared on a given profile template (tenant-scoped). " +
            "Use to enumerate the downward instructions available to all devices that instantiate the profile.")
    @GetMapping("/list_by_profile_id")
    public Mono<R<List<CommandVO>>> listByProfileId(@Parameter(description = "Identifier of the profile template whose commands are returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, profileService.getById(profileId));
            List<CommandBO> entityBOList = filterTenant(tenantId, commandService.listByProfileId(profileId, tenantId));
            List<CommandVO> entityVOList = commandBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Return the control commands a given device can receive, resolved from its bound profile template, scoped to the current tenant.
     *
     * @param deviceId id of the device whose receivable commands are returned; must belong to the current tenant
     * @return a list of CommandVO the device can receive
     */
    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands by Device ID", description = "Return the control commands a given device can receive, resolved from its bound profile template (tenant-scoped). " +
            "Use to discover which downward instructions can be sent to a specific device through its driver.")
    @GetMapping("/list_by_device_id")
    public Mono<R<List<CommandVO>>> listByDeviceId(@Parameter(description = "Identifier of the device whose receivable commands are returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.getById(deviceId));
            List<CommandBO> entityBOList = filterTenant(tenantId, commandService.listByDeviceId(deviceId, tenantId));
            List<CommandVO> entityVOList = commandBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Page through control commands for the current tenant with query filters.
     *
     * @param entityQuery optional query filters (name, profile, enable flag, etc.); null treated as empty
     * @return a page of CommandVO matching the query
     */
    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands", description = "Page through control commands for the current tenant with query filters. " +
            "Returns a page of commands; use for browsing commands or selecting one to send to a device.")
    @PostMapping("/list")
    public Mono<R<Page<CommandVO>>> list(@RequestBody(required = false) CommandQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandQuery query = Objects.isNull(entityQuery) ? new CommandQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<CommandBO> entityPageBO = commandService.list(query);
            Page<CommandVO> entityPageVO = commandBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}

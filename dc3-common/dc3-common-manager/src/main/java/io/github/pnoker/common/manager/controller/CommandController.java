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
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.builder.CommandBuilder;
import io.github.pnoker.common.manager.entity.vo.CommandVO;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
import io.github.pnoker.common.manager.repository.CommandFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Manages device command definitions declared on profile templates, including the downward control instructions a driver sends to devices.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Tag(name = "command", description = "Device command definitions: manage industrial device operations including read, write, and configuration commands with parameter specifications")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.COMMAND_URL_PREFIX)
@RequiredArgsConstructor
public class CommandController implements BaseController {

    private final CommandBuilder commandBuilder;

    private final ReactiveCommandService reactiveCommandService;

    /**
     * Create a downward control instruction defined on a profile template for the current tenant.
     *
     * @param entityVO command payload to create, carrying its parameters and attributes
     * @return the id of the newly created command
     */
    @PreAuthorize("@perm.can('command', 'add')")
    @Operation(summary = "Add Command", description = "Create a downward control instruction defined on a profile template for the current tenant. " +
            "A command carries parameters and attributes that the driver sends to a device; returns the new command ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<CommandVO> add(@Validated(Add.class) @RequestBody CommandVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            Long tenantId = tuple.getT1().getT1();
            CommandBO entityBO = commandBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            entityBO.setCreatorId(tuple.getT1().getT2()); entityBO.setCreatorName(tuple.getT2());
            entityBO.setOperatorId(tuple.getT1().getT2()); entityBO.setOperatorName(tuple.getT2());
            return reactiveCommandService.add(entityBO).map(commandBuilder::buildVOByBO);
        });
    }

    /**
     * Permanently delete a command by ID, scoped to the current tenant.
     *
     * @param id id of the command to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('command', 'delete')")
    @Operation(summary = "Delete Command", description = "Permanently delete a command by ID (tenant-scoped). " +
            "Removes the command definition from its profile template; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id,
                             @Parameter(description = "Current optimistic-lock version required as a deletion precondition.", example = "0") @NotNull @Min(0) @RequestParam("version") Integer version) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> reactiveCommandService.delete(tuple.getT1().getT1(), id, version, tuple.getT1().getT2(), tuple.getT2()).then());
    }

    /**
     * Modify an existing command's parameters and attributes, scoped to the current tenant.
     *
     * @param entityVO command payload carrying the updated fields; ownership is verified before applying
     * @return update-success status
     */
    @PreAuthorize("@perm.can('command', 'update')")
    @Operation(summary = "Update Command", description = "Modify an existing command's parameters and attributes (tenant-scoped). " +
            "Ownership is verified before applying changes; returns an update-success response.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<CommandVO> update(@Validated(Update.class) @RequestBody CommandVO entityVO) {
        return getTenantId().zipWith(getUserId().defaultIfEmpty(0L)).zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
            Long tenantId = tuple.getT1().getT1();
            CommandBO entityBO = commandBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            entityBO.setOperatorId(tuple.getT1().getT2()); entityBO.setOperatorName(tuple.getT2());
            return reactiveCommandService.update(entityBO).map(commandBuilder::buildVOByBO);
        });
    }

    /**
     * Fetch one command with its parameters and attributes, scoped to the current tenant.
     *
     * @param id id of the command to fetch; must belong to the current tenant
     * @return the matched CommandVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('command', 'get')")
    @Operation(summary = "Get Command by ID", description = "Fetch one command with its parameters and attributes (tenant-scoped). " +
            "Use to inspect a command before sending it to a device through the driver.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<CommandVO> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> reactiveCommandService.getById(tenantId, id).map(commandBuilder::buildVOByBO));
    }

    /**
     * Return every control command declared on a given profile template, scoped to the current tenant.
     *
     * @param profileId id of the profile template whose commands are returned; must belong to the current tenant
     * @return a list of CommandVO declared on the profile
     */
    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands by Profile ID", description = "Return every control command declared on a given profile template (tenant-scoped). " +
            "Use to enumerate the downward instructions available to all devices that instantiate the profile.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_profile_id")
    public Mono<List<CommandVO>> listByProfileId(@Parameter(description = "Identifier of the profile template whose commands are returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "profile_id") Long profileId) {
        return getTenantId().flatMap(tenantId -> reactiveCommandService.listByProfileId(tenantId, profileId).map(commandBuilder::buildVOByBO).collectList());
    }

    /**
     * Return the control commands a given device can receive, resolved from its bound profile template, scoped to the current tenant.
     *
     * @param deviceId id of the device whose receivable commands are returned; must belong to the current tenant
     * @return a list of CommandVO the device can receive
     */
    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands by Device ID", description = "Return the control commands a given device can receive, resolved from its bound profile template (tenant-scoped). " +
            "Use to discover which downward instructions can be sent to a specific device through its driver.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_device_id")
    public Mono<List<CommandVO>> listByDeviceId(@Parameter(description = "Identifier of the device whose receivable commands are returned; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "device_id") Long deviceId) {
        return getTenantId().flatMap(tenantId -> reactiveCommandService.listByDeviceId(tenantId, deviceId).map(commandBuilder::buildVOByBO).collectList());
    }

    /**
     * Page through control commands for the current tenant with query filters.
     *
     * @param entityQuery optional query filters (name, profile, enable flag, etc.); null treated as empty
     * @return a page of CommandVO matching the query
     */
    @PreAuthorize("@perm.can('command', 'list')")
    @Operation(summary = "List Commands", description = "Page through control commands for the current tenant with query filters. " +
            "Returns a page of commands; use for browsing commands or selecting one to send to a device.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<OffsetPage<CommandVO>> list(@RequestBody(required = false) io.github.pnoker.common.manager.entity.query.CommandOffsetRequest request) {
        io.github.pnoker.common.manager.entity.query.CommandOffsetRequest query = request == null ? new io.github.pnoker.common.manager.entity.query.CommandOffsetRequest() : request;
        return getTenantId().flatMap(tenantId -> reactiveCommandService.list(new CommandFilter(
                        tenantId, query.commandName(), query.commandCode(), query.commandTypeFlag(), query.callTypeFlag(),
                        query.profileId(), query.enableFlag(), query.version(), query.deviceId(), query.offset(), query.limit(), query.sort()))
                .map(page -> OffsetPage.of(page.items().stream().map(commandBuilder::buildVOByBO).toList(), page.offset(), page.limit(), page.total())));
    }

}

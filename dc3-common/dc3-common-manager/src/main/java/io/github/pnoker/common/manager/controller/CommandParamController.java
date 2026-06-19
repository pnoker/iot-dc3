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
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.entity.builder.CommandParamBuilder;
import io.github.pnoker.common.manager.entity.query.CommandParamQuery;
import io.github.pnoker.common.manager.entity.vo.CommandParamVO;
import io.github.pnoker.common.manager.service.CommandParamService;
import io.github.pnoker.common.manager.service.CommandService;
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
 * Manages input and output parameter definitions declared on downward device commands.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "command_param", description = "Command parameter definitions: manage input parameters for device commands including name, data type, and validation rules")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.COMMAND_PARAM_URL_PREFIX)
@RequiredArgsConstructor
public class CommandParamController implements BaseController {

    private final CommandParamBuilder commandParamBuilder;

    private final CommandParamService commandParamService;

    private final CommandService commandService;

    /**
     * Define a new input or output parameter on a command for the current tenant.
     *
     * @param entityVO command parameter payload to create (name, code, direction, type, default value)
     * @return add-success status
     */
    @PreAuthorize("@perm.can('command_param', 'add')")
    @Operation(summary = "Add Command Parameter", description = "Define a new input or output parameter on a command for the current tenant. " +
            "A command parameter declares the name, code, direction, type and default value that a downward device command accepts or returns; returns the new parameter ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody CommandParamVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandParamBO entityBO = commandParamBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            commandParamService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Permanently delete a command parameter by ID, scoped to the current tenant.
     *
     * @param id id of the command parameter to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('command_param', 'delete')")
    @Operation(summary = "Delete Command Parameter", description = "Permanently delete a command parameter by ID (tenant-scoped). " +
            "Removes the parameter definition from its parent command; the action cannot be undone.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandParamService.getById(id));
            commandParamService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Modify an existing command parameter's name, code, direction, type, required flag or default value, scoped to the current tenant.
     *
     * @param entityVO command parameter payload carrying the updated fields; ownership is verified before applying
     * @return update-success status
     */
    @PreAuthorize("@perm.can('command_param', 'update')")
    @Operation(summary = "Update Command Parameter", description = "Modify an existing command parameter's name, code, direction, type, required flag or default value. " +
            "Verifies ownership against the current tenant before applying the change.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody CommandParamVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandParamBO entityBO = commandParamBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, commandParamService.getById(entityBO.getId()));
            commandParamService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one command parameter with its direction, type, required flag and default value, scoped to the current tenant.
     *
     * @param id id of the command parameter to fetch; must belong to the current tenant
     * @return the matched CommandParamVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('command_param', 'get')")
    @Operation(summary = "Get Command Parameter by ID", description = "Fetch one command parameter with its direction, type, required flag and default value. " +
            "Use to inspect what an input or output parameter of a downward device command looks like before issuing or interpreting the command.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<CommandParamVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandParamBO entityBO = requireTenant(tenantId, commandParamService.getById(id));
            CommandParamVO entityVO = commandParamBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Return every parameter declared on a given command, scoped to the current tenant.
     *
     * @param commandId id of the command whose parameters are listed; must belong to the current tenant
     * @return a list of CommandParamVO declared on the command
     */
    @PreAuthorize("@perm.can('command_param', 'list')")
    @Operation(summary = "List Command Parameters by Command ID", description = "Return every parameter declared on a given command, tenant-scoped. " +
            "Use to discover the input and output parameters a downward device command accepts or returns when building or validating a command call.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/list_by_command_id")
    public Mono<R<List<CommandParamVO>>> listByCommandId(@Parameter(description = "Identifier of the command whose parameters are listed; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "command_id") Long commandId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandService.getById(commandId));
            List<CommandParamBO> entityBOList = filterTenant(tenantId, commandParamService.listByCommandId(commandId));
            List<CommandParamVO> entityVOList = commandParamBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Page through command parameters for the current tenant with filters such as name, code, command and enable flag.
     *
     * @param entityQuery optional query filters; null treated as empty
     * @return a page of CommandParamVO matching the query
     */
    @PreAuthorize("@perm.can('command_param', 'list')")
    @Operation(summary = "List Command Parameters", description = "Page through command parameters for the current tenant with filters such as name, code, command and enable flag. " +
            "Returns a page of command parameters; use for browsing or auditing parameter definitions across commands.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<CommandParamVO>>> list(@RequestBody(required = false) CommandParamQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandParamQuery query = Objects.isNull(entityQuery) ? new CommandParamQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<CommandParamBO> entityPageBO = commandParamService.list(query);
            Page<CommandParamVO> entityPageVO = commandParamBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}

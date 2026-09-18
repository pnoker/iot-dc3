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
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.entity.builder.CommandParamBuilder;
import io.github.pnoker.common.manager.entity.query.CommandParamOffsetRequest;
import io.github.pnoker.common.manager.entity.vo.CommandParamVO;
import io.github.pnoker.common.manager.repository.CommandParamFilter;
import io.github.pnoker.common.manager.service.ReactiveCommandParamService;
import io.github.pnoker.common.manager.service.ReactiveCommandService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Reactive HTTP API for tenant-scoped command parameters. */
@Tag(name = "command_param", description = "Command parameter definitions")
@RestController
@RequestMapping(ManagerConstant.COMMAND_PARAM_URL_PREFIX)
@RequiredArgsConstructor
public class CommandParamController implements BaseController {
    private final CommandParamBuilder commandParamBuilder;
    private final ReactiveCommandParamService commandParamService;
    private final ReactiveCommandService commandService;

    /** Add one command param and return the stored view. */
    @PreAuthorize("@perm.can('command_param', 'add')")
    @Operation(
            summary = "Add Command Parameter",
            description =
                    "Create a tenant-scoped input or output parameter on a device command and return the persisted parameter record.",
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
    public Mono<CommandParamVO> add(@Validated(Add.class) @RequestBody CommandParamVO entityVO) {
        return actor().flatMap(actor -> {
            CommandParamBO value = commandParamBuilder.buildBOByVO(entityVO);
            value.setTenantId(actor.tenantId());
            value.setCreatorId(actor.userId());
            value.setCreatorName(actor.userName());
            value.setOperatorId(actor.userId());
            value.setOperatorName(actor.userName());
            return commandParamService.add(value).map(commandParamBuilder::buildVOByBO);
        });
    }

    /** Delete the command param. */
    @PreAuthorize("@perm.can('command_param', 'delete')")
    @Operation(
            summary = "Delete Command Parameter",
            description =
                    "Soft-delete one tenant-owned command parameter and notify affected drivers that command metadata changed.",
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @Parameter(description = "Tenant-scoped command parameter ID") @NotNull @RequestParam("id") Long id,
            @Parameter(
                            description = "Current optimistic-lock version required as a deletion precondition.",
                            example = "0")
                    @NotNull
                    @Min(0)
                    @RequestParam("version")
                    Integer version) {
        return actor().flatMap(actor -> commandParamService
                .delete(actor.tenantId(), id, version, actor.userId(), actor.userName())
                .then());
    }

    /** Update one command param and emit the updated row. */
    @PreAuthorize("@perm.can('command_param', 'update')")
    @Operation(
            summary = "Update Command Parameter",
            description =
                    "Update a tenant-owned command parameter using optimistic locking while preserving its parent command and stable code.",
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
    public Mono<CommandParamVO> update(@Validated(Update.class) @RequestBody CommandParamVO entityVO) {
        return actor().flatMap(actor -> {
            CommandParamBO value = commandParamBuilder.buildBOByVO(entityVO);
            value.setTenantId(actor.tenantId());
            value.setOperatorId(actor.userId());
            value.setOperatorName(actor.userName());
            return commandParamService.update(value).map(commandParamBuilder::buildVOByBO);
        });
    }

    /** Resolve the command param by its id. */
    @PreAuthorize("@perm.can('command_param', 'get')")
    @Operation(
            summary = "Get Command Parameter by ID",
            description =
                    "Fetch one command parameter by primary key with strict tenant isolation and soft-delete filtering.",
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
    public Mono<CommandParamVO> getById(
            @Parameter(description = "Primary key of the tenant-owned command parameter.") @NotNull @RequestParam("id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> commandParamService.getById(tenantId, id).map(commandParamBuilder::buildVOByBO));
    }

    /** List command params matched by command id. */
    @PreAuthorize("@perm.can('command_param', 'list')")
    @Operation(
            summary = "List Command Parameters by Command ID",
            description =
                    "List every active parameter declared by one tenant-owned command in deterministic identifier order.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list_by_command_id")
    public Mono<List<CommandParamVO>> listByCommandId(
            @Parameter(description = "Parent command identifier that must belong to the current tenant.")
                    @NotNull
                    @RequestParam("command_id")
                    Long commandId) {
        return getTenantId()
                .flatMap(tenantId -> commandService
                        .getById(tenantId, commandId)
                        .thenMany(commandParamService.listByCommandId(tenantId, commandId))
                        .map(commandParamBuilder::buildVOByBO)
                        .collectList());
    }

    /** Page command params matching the tenant-scoped filters. */
    @PreAuthorize("@perm.can('command_param', 'list')")
    @Operation(
            summary = "List Command Parameters",
            description =
                    "Return an offset page of tenant-scoped command parameters using validated filters and stable whitelisted sorting.",
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
    public Mono<OffsetPage<CommandParamVO>> list(@RequestBody(required = false) CommandParamOffsetRequest request) {
        CommandParamOffsetRequest query = request == null ? new CommandParamOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> commandParamService
                        .list(new CommandParamFilter(
                                tenantId,
                                query.paramName(),
                                query.paramCode(),
                                query.paramDirection(),
                                query.paramTypeFlag(),
                                query.commandId(),
                                query.enableFlag(),
                                query.version(),
                                query.offset(),
                                query.limit(),
                                query.sort()))
                        .map(page -> OffsetPage.of(
                                page.items().stream()
                                        .map(commandParamBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total())));
    }

    private Mono<Actor> actor() {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .map(tuple -> new Actor(tuple.getT1().getT1(), tuple.getT1().getT2(), tuple.getT2()));
    }

    private record Actor(Long tenantId, Long userId, String userName) {}
}

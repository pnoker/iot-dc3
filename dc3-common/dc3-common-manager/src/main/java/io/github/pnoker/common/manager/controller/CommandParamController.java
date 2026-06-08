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
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.entity.builder.CommandParamBuilder;
import io.github.pnoker.common.manager.entity.query.CommandParamQuery;
import io.github.pnoker.common.manager.entity.vo.CommandParamVO;
import io.github.pnoker.common.manager.service.CommandParamService;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing command param management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "command_param", description = "指令参数")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.COMMAND_PARAM_URL_PREFIX)
@RequiredArgsConstructor
public class CommandParamController implements BaseController {

    private final CommandParamBuilder commandParamBuilder;

    private final CommandParamService commandParamService;

    private final CommandService commandService;

    @PreAuthorize("@perm.can('command_param', 'add')")
    @Operation(summary = "新增CommandParam", description = "新增一条CommandParam记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody CommandParamVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandParamBO entityBO = commandParamBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            commandParamService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('command_param', 'delete')")
    @Operation(summary = "删除CommandParam", description = "删除指定ID的CommandParam")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandParamService.getById(id));
            commandParamService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('command_param', 'update')")
    @Operation(summary = "更新CommandParam", description = "更新CommandParam信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody CommandParamVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandParamBO entityBO = commandParamBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, commandParamService.getById(entityBO.getId()));
            commandParamService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('command_param', 'get')")
    @Operation(summary = "查询CommandParam", description = "根据ID查询CommandParam详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<CommandParamVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandParamBO entityBO = requireTenant(tenantId, commandParamService.getById(id));
            CommandParamVO entityVO = commandParamBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PreAuthorize("@perm.can('command_param', 'list')")
    @Operation(summary = "查询CommandParam列表", description = "根据关联条件查询CommandParam列表")
    @GetMapping("/list_by_command_id")
    public Mono<R<List<CommandParamVO>>> listByCommandId(@NotNull @RequestParam(value = "command_id") Long commandId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandService.getById(commandId));
            List<CommandParamBO> entityBOList = filterTenant(tenantId, commandParamService.listByCommandId(commandId));
            List<CommandParamVO> entityVOList = commandParamBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @PreAuthorize("@perm.can('command_param', 'list')")
    @Operation(summary = "查询CommandParam列表", description = "分页查询CommandParam列表")
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

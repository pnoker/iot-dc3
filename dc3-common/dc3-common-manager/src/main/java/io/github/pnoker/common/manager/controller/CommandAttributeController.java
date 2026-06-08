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
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.builder.CommandAttributeBuilder;
import io.github.pnoker.common.manager.entity.query.CommandAttributeQuery;
import io.github.pnoker.common.manager.entity.vo.CommandAttributeVO;
import io.github.pnoker.common.manager.service.CommandAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing command attribute management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "command_attribute", description = "指令属性")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.COMMAND_ATTRIBUTE_URL_PREFIX)
@RequiredArgsConstructor
public class CommandAttributeController implements BaseController {

    private final CommandAttributeBuilder commandAttributeBuilder;

    private final CommandAttributeService commandAttributeService;

    private final DriverService driverService;

    /**
     * 指令属性
     *
     * @param entityVO {@link CommandAttributeVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('command_attribute', 'add')")
    @Operation(summary = "新增CommandAttribute", description = "新增一条CommandAttribute记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody CommandAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeBO entityBO = commandAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            commandAttributeService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID 指令属性
     *
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('command_attribute', 'delete')")
    @Operation(summary = "删除CommandAttribute", description = "删除指定ID的CommandAttribute")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, commandAttributeService.getById(id));
            commandAttributeService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * 指令属性
     *
     * @param entityVO {@link CommandAttributeVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('command_attribute', 'update')")
    @Operation(summary = "更新CommandAttribute", description = "更新CommandAttribute信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody CommandAttributeVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeBO entityBO = commandAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, commandAttributeService.getById(entityBO.getId()));
            commandAttributeService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID 指令属性
     *
     * @param id ID
     * @return CommandAttributeVO {@link CommandAttributeVO}
     */
    @PreAuthorize("@perm.can('command_attribute', 'get')")
    @Operation(summary = "查询CommandAttribute", description = "根据ID查询CommandAttribute详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<CommandAttributeVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeBO entityBO = requireTenant(tenantId, commandAttributeService.getById(id));
            CommandAttributeVO entityVO = commandAttributeBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * Driver ID 指令属性
     *
     * @param id ID
     * @return 指令属性
     */
    @PreAuthorize("@perm.can('command_attribute', 'list')")
    @Operation(summary = "查询CommandAttribute列表", description = "根据关联条件查询CommandAttribute列表")
    @GetMapping("/list_by_driver_id")
    public Mono<R<List<CommandAttributeVO>>> listByDriverId(@NotNull @RequestParam(value = "driver_id") Long driverId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            try {
                requireTenant(tenantId, driverService.getById(driverId));
                List<CommandAttributeBO> entityBOList = filterTenant(tenantId, commandAttributeService.listByDriverId(driverId));
                List<CommandAttributeVO> entityVO = commandAttributeBuilder.buildVOListByBOList(entityBOList);
                return R.ok(entityVO);
            } catch (NotFoundException ne) {
                return R.ok(Collections.emptyList());
            }
        }));
    }

    /**
     * 指令属性
     *
     * @param entityQuery Dto
     * @return Page Of 指令属性
     */
    @PreAuthorize("@perm.can('command_attribute', 'list')")
    @Operation(summary = "查询CommandAttribute列表", description = "分页查询CommandAttribute列表")
    @PostMapping("/list")
    public Mono<R<Page<CommandAttributeVO>>> list(@RequestBody(required = false) CommandAttributeQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            CommandAttributeQuery query = Objects.isNull(entityQuery) ? new CommandAttributeQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<CommandAttributeBO> entityPageBO = commandAttributeService.list(query);
            Page<CommandAttributeVO> entityPageVO = commandAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}

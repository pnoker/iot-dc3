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

package io.github.pnoker.common.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.auth.entity.vo.RoleResourceBindVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * RoleResourceBind Controller
 *
 * @author pnoker
 * @version 2026.5.5
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_RESOURCE_URL_PREFIX)
public class RoleResourceBindController implements BaseController {

    private final RoleResourceBindBuilder roleResourceBindBuilder;

    private final RoleResourceBindService roleResourceBindService;

    private final ResourceBuilder resourceBuilder;

    private final RoleBuilder roleBuilder;

    public RoleResourceBindController(RoleResourceBindBuilder roleResourceBindBuilder,
                                      RoleResourceBindService roleResourceBindService, ResourceBuilder resourceBuilder, RoleBuilder roleBuilder) {
        this.roleResourceBindBuilder = roleResourceBindBuilder;
        this.roleResourceBindService = roleResourceBindService;
        this.resourceBuilder = resourceBuilder;
        this.roleBuilder = roleBuilder;
    }

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleResourceBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindBO entityBO = roleResourceBindBuilder.buildBOByVO(entityVO);
            roleResourceBindService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            roleResourceBindService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    @PostMapping("/list")
    public Mono<R<Page<RoleResourceBindVO>>> list(@RequestBody(required = false) RoleResourceBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindQuery query = Objects.isNull(entityQuery) ? new RoleResourceBindQuery() : entityQuery;
            Page<RoleResourceBindBO> entityPageBO = roleResourceBindService.selectByPage(query, tenantId);
            Page<RoleResourceBindVO> entityPageVO = roleResourceBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    @GetMapping("/list-resource-by-role/{roleId}")
    public Mono<R<List<ResourceVO>>> listResourceByRole(@NotNull @PathVariable(value = "roleId") Long roleId) {
        return async(() -> {
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByRoleId(roleId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        });
    }

    @GetMapping("/list-resource-by-user/{userId}")
    public Mono<R<List<ResourceVO>>> listResourceByUser(@NotNull @PathVariable(value = "userId") Long userId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByUserId(userId, tenantId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @GetMapping("/list-role-by-resource/{resourceId}")
    public Mono<R<List<RoleVO>>> listRoleByResource(@NotNull @PathVariable(value = "resourceId") Long resourceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<RoleBO> entityBOList = roleResourceBindService.listRoleByResourceId(resourceId, tenantId);
            List<RoleVO> entityVOList = roleBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

}

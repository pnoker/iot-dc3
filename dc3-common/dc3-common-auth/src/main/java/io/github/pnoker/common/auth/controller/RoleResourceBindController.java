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
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.auth.entity.vo.RoleResourceBindVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.valid.Add;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * REST controller exposing role-resource binding management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_RESOURCE_URL_PREFIX)
@RequiredArgsConstructor
public class RoleResourceBindController implements BaseController {

    private final RoleResourceBindBuilder roleResourceBindBuilder;

    private final RoleResourceBindService roleResourceBindService;

    private final ResourceBuilder resourceBuilder;

    private final RoleBuilder roleBuilder;

    private final RoleService roleService;

    private final TenantBindService tenantBindService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleResourceBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindBO entityBO = roleResourceBindBuilder.buildBOByVO(entityVO);
            requireTenant(tenantId, roleService.getById(entityBO.getRoleId()));
            roleResourceBindService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindBO entityBO = roleResourceBindService.getById(id);
            requireTenant(tenantId, roleService.getById(entityBO.getRoleId()));
            roleResourceBindService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<RoleResourceBindVO>>> list(@RequestBody(required = false) RoleResourceBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleResourceBindQuery query = Objects.isNull(entityQuery) ? new RoleResourceBindQuery() : entityQuery;
            Page<RoleResourceBindBO> entityPageBO = roleResourceBindService.list(query, tenantId);
            Page<RoleResourceBindVO> entityPageVO = roleResourceBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    @GetMapping("/list_resource_by_role")
    public Mono<R<List<ResourceVO>>> listResourceByRole(@NotNull @RequestParam(value = "role_id") Long roleId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(roleId));
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByRoleId(roleId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @GetMapping("/list_resource_by_user")
    public Mono<R<List<ResourceVO>>> listResourceByUser(@NotNull @RequestParam(value = "user_id") Long userId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenantMember(tenantId, userId);
            List<ResourceBO> entityBOList = roleResourceBindService.listResourceByUserId(userId, tenantId);
            List<ResourceVO> entityVOList = resourceBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @GetMapping("/list_role_by_resource")
    public Mono<R<List<RoleVO>>> listRoleByResource(@NotNull @RequestParam(value = "resource_id") Long resourceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<RoleBO> entityBOList = roleResourceBindService.listRoleByResourceId(resourceId, tenantId);
            List<RoleVO> entityVOList = roleBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    private void requireTenantMember(Long tenantId, Long userId) {
        TenantBindBO tenantBind = tenantBindService.getByTenantIdAndUserId(tenantId, userId);
        if (Objects.isNull(tenantBind)) {
            throw new NotFoundException("Resource does not exist");
        }
    }

}

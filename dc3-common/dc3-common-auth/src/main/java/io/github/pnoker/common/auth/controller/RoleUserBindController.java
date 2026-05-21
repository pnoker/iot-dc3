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

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleUserBindBO;
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleUserBindBuilder;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.query.RoleUserBindQuery;
import io.github.pnoker.common.auth.entity.vo.RoleUserBindVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.entity.vo.UserVO;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.auth.service.RoleUserBindService;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.valid.Add;
import jakarta.validation.constraints.NotNull;
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
 * REST controller exposing role-user binding management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_USER_URL_PREFIX)
@RequiredArgsConstructor
public class RoleUserBindController implements BaseController {

    private final RoleUserBindBuilder roleUserBindBuilder;

    private final RoleUserBindService roleUserBindService;

    private final RoleBuilder roleBuilder;

    private final UserBuilder userBuilder;

    private final RoleService roleService;

    private final TenantBindService tenantBindService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleUserBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleUserBindBO entityBO = roleUserBindBuilder.buildBOByVO(entityVO);
            requireTenant(tenantId, roleService.getById(entityBO.getRoleId()));
            requireTenantMember(tenantId, entityBO.getUserId());
            roleUserBindService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleUserBindBO entityBO = roleUserBindService.getById(id);
            requireTenant(tenantId, roleService.getById(entityBO.getRoleId()));
            requireTenantMember(tenantId, entityBO.getUserId());
            roleUserBindService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<RoleUserBindVO>>> list(@RequestBody(required = false) RoleUserBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleUserBindQuery query = Objects.isNull(entityQuery) ? new RoleUserBindQuery() : entityQuery;
            Page<RoleUserBindBO> entityPageBO = roleUserBindService.list(query, tenantId);
            Page<RoleUserBindVO> entityPageVO = roleUserBindBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    @GetMapping("/list_role_by_user")
    public Mono<R<List<RoleVO>>> listRoleByUser(@NotNull @RequestParam(value = "user_id") Long userId,
                                                @RequestParam(value = "tenant_id", required = false) Long ignoredTenantId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenantMember(tenantId, userId);
            List<RoleBO> entityBOList = roleUserBindService.listRoleByTenantIdAndUserId(tenantId, userId);
            List<RoleVO> entityVOList = roleBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    @GetMapping("/list_user_by_role")
    public Mono<R<List<UserVO>>> listUserByRole(@NotNull @RequestParam(value = "role_id") Long roleId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(roleId));
            List<UserBO> entityBOList = roleUserBindService.listUserByRoleId(roleId)
                    .stream()
                    .filter(user -> Objects.nonNull(tenantBindService.getByTenantIdAndUserId(tenantId, user.getId())))
                    .toList();
            List<UserVO> entityVOList = userBuilder.buildVOListByBOList(entityBOList);
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

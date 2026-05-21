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
import io.github.pnoker.common.auth.entity.bo.RoleTreeBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.query.RoleQuery;
import io.github.pnoker.common.auth.entity.vo.RoleTreeVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing role management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.ROLE_URL_PREFIX)
@RequiredArgsConstructor
public class RoleController implements BaseController {

    private final RoleBuilder roleBuilder;

    private final RoleService roleService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RoleVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            RoleBO entityBO = roleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            roleService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, roleService.getById(id));
            roleService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody RoleVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            RoleBO entityBO = roleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(header.getTenantId());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            requireTenant(header.getTenantId(), roleService.getById(entityBO.getId()));
            roleService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @GetMapping("/get_by_id")
    public Mono<R<RoleVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleBO entityBO = requireTenant(tenantId, roleService.getById(id));
            RoleVO entityVO = roleBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<RoleVO>>> list(@RequestBody(required = false) RoleQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleQuery query = Objects.isNull(entityQuery) ? new RoleQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<RoleBO> entityPageBO = roleService.list(query);
            Page<RoleVO> entityPageVO = roleBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    @PostMapping("/list_tree")
    public Mono<R<List<RoleTreeVO>>> listTree(@RequestBody(required = false) RoleQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RoleQuery query = Objects.isNull(entityQuery) ? new RoleQuery() : entityQuery;
            query.setTenantId(tenantId);
            List<RoleTreeBO> entityBOList = roleService.listTree(query);
            List<RoleTreeVO> entityVOList = new ArrayList<>(entityBOList.size());
            for (RoleTreeBO node : entityBOList) {
                entityVOList.add(toTreeVO(node));
            }
            return R.ok(entityVOList);
        }));
    }

    private RoleTreeVO toTreeVO(RoleTreeBO node) {
        RoleVO flat = roleBuilder.buildVOByBO(node);
        RoleTreeVO out = new RoleTreeVO();
        out.setId(flat.getId());
        out.setParentRoleId(flat.getParentRoleId());
        out.setRoleName(flat.getRoleName());
        out.setRoleCode(flat.getRoleCode());
        out.setRoleExt(flat.getRoleExt());
        out.setEnableFlag(flat.getEnableFlag());
        out.setRemark(flat.getRemark());
        out.setCreatorId(flat.getCreatorId());
        out.setCreatorName(flat.getCreatorName());
        out.setCreateTime(flat.getCreateTime());
        out.setOperatorId(flat.getOperatorId());
        out.setOperatorName(flat.getOperatorName());
        out.setOperateTime(flat.getOperateTime());
        if (Objects.nonNull(node.getChildren())) {
            List<RoleTreeVO> childVOs = new ArrayList<>(node.getChildren().size());
            for (RoleTreeBO child : node.getChildren()) {
                childVOs.add(toTreeVO(child));
            }
            out.setChildren(childVOs);
        }
        return out;
    }

}

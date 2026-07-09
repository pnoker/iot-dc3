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

package io.github.pnoker.common.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.RoleManager;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleTreeBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.auth.entity.query.RoleQuery;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Business service implementation for role operations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleBuilder roleBuilder;

    private final RoleManager roleManager;

    @Override
    public void add(RoleBO entityBO) {
        checkDuplicate(entityBO, false, true);

        RoleDO entityDO = roleBuilder.buildDOByBO(entityBO);
        if (!roleManager.save(entityDO)) {
            throw new AddException("Failed to create role");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        // Refuse deletion while sub roles exist, otherwise they would be orphaned
        LambdaQueryChainWrapper<RoleDO> wrapper = roleManager.lambdaQuery().eq(RoleDO::getParentRoleId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove role: some sub roles exists in the role");
        }

        if (!roleManager.removeById(id)) {
            throw new DeleteException("Failed to remove role");
        }
    }

    @Override
    public void update(RoleBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        RoleDO entityDO = roleBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!roleManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update role");
        }
    }

    @Override
    public RoleBO getById(Long id) {
        RoleDO entityDO = getDOById(id, true);
        return roleBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<RoleBO> list(RoleQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RoleDO> entityPageDO = roleManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return roleBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public List<RoleTreeBO> listTree(RoleQuery entityQuery) {
        RoleQuery effective = Objects.requireNonNullElseGet(entityQuery, RoleQuery::new);
        List<RoleDO> rows = roleManager.list(fuzzyQuery(effective));
        return assembleTree(rows);
    }

    /**
     * Build the role hierarchy from a flat list of DOs. Roles with {@code parent_role_id}
     * null or 0 become roots; children are linked by their {@code parent_role_id}, and
     * each level is ordered by role name.
     */
    private List<RoleTreeBO> assembleTree(List<RoleDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return new ArrayList<>();
        }
        Map<Long, RoleTreeBO> byId = new HashMap<>(rows.size());
        Map<Long, Long> parentByChild = new HashMap<>(rows.size());
        for (RoleDO row : rows) {
            RoleTreeBO node = RoleTreeBO.fromBO(roleBuilder.buildBOByDO(row));
            byId.put(row.getId(), node);
            parentByChild.put(row.getId(), row.getParentRoleId());
        }
        List<RoleTreeBO> roots = new ArrayList<>();
        for (Map.Entry<Long, RoleTreeBO> e : byId.entrySet()) {
            Long parentId = parentByChild.get(e.getKey());
            RoleTreeBO parent = Objects.isNull(parentId) || parentId == 0L ? null : byId.get(parentId);
            if (Objects.isNull(parent)) {
                roots.add(e.getValue());
            } else {
                parent.addChild(e.getValue());
            }
        }
        Comparator<RoleTreeBO> order = Comparator.comparing(RoleTreeBO::getRoleName,
                Comparator.nullsLast(Comparator.naturalOrder()));
        sortRecursive(roots, order);
        return roots;
    }

    private void sortRecursive(List<RoleTreeBO> nodes, Comparator<RoleTreeBO> order) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        nodes.sort(order);
        for (RoleTreeBO node : nodes) {
            sortRecursive(node.getChildren(), order);
        }
    }

    /**
     * Build fuzzy query wrapper for role search.
     *
     * @param entityQuery {@link RoleQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link RoleDO}
     */
    private LambdaQueryWrapper<RoleDO> fuzzyQuery(RoleQuery entityQuery) {
        LambdaQueryWrapper<RoleDO> wrapper = Wrappers.<RoleDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getRoleName()), RoleDO::getRoleName, entityQuery.getRoleName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getRoleCode()), RoleDO::getRoleCode, entityQuery.getRoleCode());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), RoleDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), RoleDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * Check whether a role is duplicated by parent role, name, code, and tenant.
     *
     * @param entityBO       {@link RoleBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(RoleBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<RoleDO> wrapper = Wrappers.<RoleDO>query().lambda();
        wrapper.eq(RoleDO::getParentRoleId, entityBO.getParentRoleId());
        wrapper.eq(RoleDO::getRoleName, entityBO.getRoleName());
        wrapper.eq(RoleDO::getRoleCode, entityBO.getRoleCode());
        wrapper.eq(RoleDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RoleDO one = roleManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Role has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get role data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link RoleDO} if found, otherwise {@code null} when {@code throwException}
     * is false
     */
    private RoleDO getDOById(Long id, boolean throwException) {
        RoleDO entityDO = roleManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Role does not exist");
        }
        return entityDO;
    }

}

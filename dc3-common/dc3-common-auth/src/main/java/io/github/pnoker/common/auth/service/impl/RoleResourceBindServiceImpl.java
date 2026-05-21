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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.ResourceManager;
import io.github.pnoker.common.auth.dal.RoleManager;
import io.github.pnoker.common.auth.dal.RoleResourceBindManager;
import io.github.pnoker.common.auth.dal.RoleUserBindManager;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.auth.entity.model.RoleResourceBindDO;
import io.github.pnoker.common.auth.entity.model.RoleUserBindDO;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Business service implementation for role-resource binding operations.
 *
 * @author linys
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleResourceBindServiceImpl implements RoleResourceBindService {

    private final ResourceBuilder resourceBuilder;

    private final RoleBuilder roleBuilder;

    private final RoleResourceBindBuilder roleResourceBindBuilder;

    private final RoleResourceBindManager roleResourceBindManager;

    private final ResourceManager resourceManager;

    private final RoleManager roleManager;

    private final RoleUserBindManager roleUserBindManager;

    @Override
    public void add(RoleResourceBindBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create role resource bind: role resource bind has been duplicated");
        }

        RoleResourceBindDO entityDO = roleResourceBindBuilder.buildDOByBO(entityBO);
        if (!roleResourceBindManager.save(entityDO)) {
            throw new AddException("Failed to create role resource bind");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!roleResourceBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove role resource bind");
        }
    }

    @Override
    public void update(RoleResourceBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update role resource bind: role resource bind has been duplicated");
        }

        RoleResourceBindDO entityDO = roleResourceBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!roleResourceBindManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update role resource bind");
        }
    }

    @Override
    public RoleResourceBindBO getById(Long id) {
        RoleResourceBindDO entityDO = getDOById(id, true);
        return roleResourceBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<RoleResourceBindBO> list(RoleResourceBindQuery entityQuery) {
        return list(entityQuery, null);
    }

    @Override
    public Page<RoleResourceBindBO> list(RoleResourceBindQuery entityQuery, Long tenantId) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RoleResourceBindDO> entityPageDO = roleResourceBindManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery, tenantId));
        return roleResourceBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public List<ResourceBO> listResourceByUserId(Long userId, Long tenantId) {
        if (Objects.isNull(userId)) {
            return Collections.emptyList();
        }
        // Step 1: roles the user is bound to.
        LambdaQueryWrapper<RoleUserBindDO> userWrapper = Wrappers.<RoleUserBindDO>query().lambda();
        userWrapper.eq(RoleUserBindDO::getUserId, userId);
        userWrapper.select(RoleUserBindDO::getRoleId);
        List<Long> roleIds = roleUserBindManager.listObjs(userWrapper, o -> (Long) o);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        // Step 2: narrow to the caller's tenant so a stale cross-tenant binding
        // cannot leak resources outside the current scope.
        if (Objects.nonNull(tenantId)) {
            LambdaQueryWrapper<RoleDO> roleWrapper = Wrappers.<RoleDO>query().lambda();
            roleWrapper.in(RoleDO::getId, roleIds);
            roleWrapper.eq(RoleDO::getTenantId, tenantId);
            roleWrapper.select(RoleDO::getId);
            roleIds = roleManager.listObjs(roleWrapper, o -> (Long) o);
            if (CollectionUtils.isEmpty(roleIds)) {
                return Collections.emptyList();
            }
        }
        // Step 3: role -> resource bindings.
        LambdaQueryWrapper<RoleResourceBindDO> bindWrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        bindWrapper.in(RoleResourceBindDO::getRoleId, roleIds);
        bindWrapper.select(RoleResourceBindDO::getResourceId);
        List<Long> resourceIds = roleResourceBindManager.listObjs(bindWrapper, o -> (Long) o)
                .stream()
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(resourceIds)) {
            return Collections.emptyList();
        }
        // Step 4: fetch resources and drop disabled ones (same rule as
        // listResourceByRoleId).
        List<ResourceDO> resourceDOList = resourceManager.listByIds(resourceIds)
                .stream()
                .filter(e -> EnableFlagEnum.ENABLE.getIndex().equals(e.getEnableFlag()))
                .toList();
        return resourceBuilder.buildBOListByDOList(resourceDOList);
    }

    @Override
    public List<RoleBO> listRoleByResourceId(Long resourceId, Long tenantId) {
        if (Objects.isNull(resourceId)) {
            return Collections.emptyList();
        }
        // Step 1: role_ids currently bound to this resource.
        LambdaQueryWrapper<RoleResourceBindDO> bindWrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        bindWrapper.eq(RoleResourceBindDO::getResourceId, resourceId);
        bindWrapper.select(RoleResourceBindDO::getRoleId);
        List<Long> roleIds = roleResourceBindManager.listObjs(bindWrapper, o -> (Long) o);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        // Step 2: fetch roles; filter by tenant (caller-scoped) and enabled flag.
        List<RoleDO> enabled = roleManager.listByIds(roleIds)
                .stream()
                .filter(e -> EnableFlagEnum.ENABLE.getIndex().equals(e.getEnableFlag())
                        && (Objects.isNull(tenantId) || tenantId.equals(e.getTenantId())))
                .toList();
        return roleBuilder.buildBOListByDOList(enabled);
    }

    @Override
    public List<ResourceBO> listResourceByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleResourceBindDO> wrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        wrapper.eq(RoleResourceBindDO::getRoleId, roleId);
        List<RoleResourceBindDO> entityDOList = roleResourceBindManager.list(wrapper);
        if (CollectionUtils.isNotEmpty(entityDOList)) {
            List<ResourceDO> resourceDOList = resourceManager
                    .listByIds(entityDOList.stream().map(RoleResourceBindDO::getResourceId).toList());
            List<ResourceDO> collect = resourceDOList.stream()
                    .filter(e -> EnableFlagEnum.ENABLE.getIndex().equals(e.getEnableFlag()))
                    .toList();
            return resourceBuilder.buildBOListByDOList(collect);
        }

        return null;
    }

    /**
     * @param entityQuery {@link RoleResourceBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<RoleResourceBindDO> fuzzyQuery(RoleResourceBindQuery entityQuery, Long tenantId) {
        LambdaQueryWrapper<RoleResourceBindDO> wrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getRoleId()), RoleResourceBindDO::getRoleId,
                entityQuery.getRoleId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getResourceId()), RoleResourceBindDO::getResourceId,
                entityQuery.getResourceId());
        if (Objects.nonNull(tenantId)) {
            LambdaQueryWrapper<RoleDO> roleWrapper = Wrappers.<RoleDO>query().lambda();
            roleWrapper.eq(RoleDO::getTenantId, tenantId);
            roleWrapper.select(RoleDO::getId);
            List<Long> roleIds = roleManager.listObjs(roleWrapper, o -> (Long) o);
            if (CollectionUtils.isEmpty(roleIds)) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(RoleResourceBindDO::getRoleId, roleIds);
            }
        }
        return wrapper;
    }

    /**
     * @param entityBO {@link RoleResourceBindBO}
     * @param isUpdate
     * @return
     */
    private boolean checkDuplicate(RoleResourceBindBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<RoleResourceBindDO> wrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        wrapper.eq(RoleResourceBindDO::getRoleId, entityBO.getRoleId());
        wrapper.eq(RoleResourceBindDO::getResourceId, entityBO.getResourceId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RoleResourceBindDO one = roleResourceBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link RoleResourceBindDO}
     */
    private RoleResourceBindDO getDOById(Long id, boolean throwException) {
        RoleResourceBindDO entityDO = roleResourceBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Role resource bind does not exist");
        }
        return entityDO;
    }

}

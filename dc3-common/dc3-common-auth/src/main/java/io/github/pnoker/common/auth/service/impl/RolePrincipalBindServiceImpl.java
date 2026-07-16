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
import io.github.pnoker.common.auth.dal.RoleManager;
import io.github.pnoker.common.auth.dal.RolePrincipalBindManager;
import io.github.pnoker.common.auth.dal.UserManager;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RolePrincipalBindBO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RolePrincipalBindBuilder;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.auth.entity.model.RolePrincipalBindDO;
import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.common.auth.entity.query.RolePrincipalBindQuery;
import io.github.pnoker.common.auth.service.RolePrincipalBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
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
 * Role-principal binding service implementation.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolePrincipalBindServiceImpl implements RolePrincipalBindService {

    private final RolePrincipalBindBuilder rolePrincipalBindBuilder;

    private final RoleBuilder roleBuilder;

    private final UserBuilder userBuilder;

    private final RolePrincipalBindManager rolePrincipalBindManager;

    private final RoleManager roleManager;

    private final UserManager userManager;

    @Override
    public void add(RolePrincipalBindBO entityBO) {
        checkDuplicate(entityBO, false, true);
        RolePrincipalBindDO entityDO = rolePrincipalBindBuilder.buildDOByBO(entityBO);
        if (!rolePrincipalBindManager.save(entityDO)) {
            throw new AddException("Failed to create role principal bind");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);
        if (!rolePrincipalBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove role principal bind");
        }
    }

    @Override
    public void update(RolePrincipalBindBO entityBO) {
        getDOById(entityBO.getId(), true);
        checkDuplicate(entityBO, true, true);
        RolePrincipalBindDO entityDO = rolePrincipalBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!rolePrincipalBindManager.updateById(entityDO)) {
            throw new UpdateException("The role principal bind update failed");
        }
    }

    @Override
    public RolePrincipalBindBO getById(Long id) {
        return rolePrincipalBindBuilder.buildBOByDO(getDOById(id, true));
    }

    @Override
    public Page<RolePrincipalBindBO> list(RolePrincipalBindQuery entityQuery) {
        return list(entityQuery, null);
    }

    @Override
    public Page<RolePrincipalBindBO> list(RolePrincipalBindQuery entityQuery, Long tenantId) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RolePrincipalBindDO> page = rolePrincipalBindManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery, tenantId));
        return rolePrincipalBindBuilder.buildBOPageByDOPage(page);
    }

    @Override
    public List<RoleBO> listRoleByTenantIdAndPrincipalId(Long tenantId, Long principalId) {
        LambdaQueryWrapper<RolePrincipalBindDO> wrapper = Wrappers.<RolePrincipalBindDO>query().lambda();
        wrapper.eq(RolePrincipalBindDO::getTenantId, tenantId);
        wrapper.eq(RolePrincipalBindDO::getPrincipalId, principalId);
        wrapper.select(RolePrincipalBindDO::getRoleId);
        List<Long> roleIds = rolePrincipalBindManager.listObjs(wrapper, o -> (Long) o);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<RoleDO> roles = roleManager.listByIds(roleIds)
                .stream()
                .filter(role -> EnableFlagEnum.ENABLE.getIndex().equals(role.getEnableFlag()))
                .filter(role -> Objects.equals(tenantId, role.getTenantId()))
                .toList();
        return roleBuilder.buildBOListByDOList(roles);
    }

    @Override
    public List<UserBO> listUserByRoleId(Long roleId) {
        if (Objects.isNull(roleId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<RolePrincipalBindDO> wrapper = Wrappers.<RolePrincipalBindDO>query().lambda();
        wrapper.eq(RolePrincipalBindDO::getRoleId, roleId);
        wrapper.eq(RolePrincipalBindDO::getPrincipalType, PrincipalTypeEnum.USER.getValue());
        wrapper.select(RolePrincipalBindDO::getPrincipalId);
        List<Long> principalIds = rolePrincipalBindManager.listObjs(wrapper, o -> (Long) o);
        if (CollectionUtils.isEmpty(principalIds)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<UserDO> userWrapper = Wrappers.<UserDO>query().lambda();
        userWrapper.in(UserDO::getPrincipalId, principalIds);
        userWrapper.eq(UserDO::getEnableFlag, EnableFlagEnum.ENABLE.getIndex());
        return userBuilder.buildBOListByDOList(userManager.list(userWrapper));
    }

    /**
     * Build fuzzy query wrapper for role-principal binding search.
     *
     * @param entityQuery {@link RolePrincipalBindQuery} query parameters
     * @param tenantId    optional tenant filter
     * @return {@link LambdaQueryWrapper} for {@link RolePrincipalBindDO}
     */
    private LambdaQueryWrapper<RolePrincipalBindDO> fuzzyQuery(RolePrincipalBindQuery entityQuery, Long tenantId) {
        LambdaQueryWrapper<RolePrincipalBindDO> wrapper = Wrappers.<RolePrincipalBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getTenantId()), RolePrincipalBindDO::getTenantId,
                entityQuery.getTenantId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getRoleId()), RolePrincipalBindDO::getRoleId,
                entityQuery.getRoleId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getPrincipalId()), RolePrincipalBindDO::getPrincipalId,
                entityQuery.getPrincipalId());
        wrapper.eq(Objects.nonNull(entityQuery.getPrincipalType()), RolePrincipalBindDO::getPrincipalType,
                Objects.isNull(entityQuery.getPrincipalType()) ? null : entityQuery.getPrincipalType().getValue());
        wrapper.eq(Objects.nonNull(tenantId), RolePrincipalBindDO::getTenantId, tenantId);
        return wrapper;
    }

    /**
     * Check whether a role-principal binding is duplicated by tenant, role, and principal.
     *
     * @param entityBO       {@link RolePrincipalBindBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(RolePrincipalBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<RolePrincipalBindDO> wrapper = Wrappers.<RolePrincipalBindDO>query().lambda();
        wrapper.eq(RolePrincipalBindDO::getTenantId, entityBO.getTenantId());
        wrapper.eq(RolePrincipalBindDO::getRoleId, entityBO.getRoleId());
        wrapper.eq(RolePrincipalBindDO::getPrincipalId, entityBO.getPrincipalId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RolePrincipalBindDO one = rolePrincipalBindManager.getOne(wrapper);
        boolean duplicate = Objects.nonNull(one) && (!isUpdate || !one.getId().equals(entityBO.getId()));
        if (throwException && duplicate) {
            throw new DuplicateException("Role principal bind has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get role-principal binding data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link RolePrincipalBindDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private RolePrincipalBindDO getDOById(Long id, boolean throwException) {
        RolePrincipalBindDO entityDO = rolePrincipalBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Role principal bind does not exist");
        }
        return entityDO;
    }

}

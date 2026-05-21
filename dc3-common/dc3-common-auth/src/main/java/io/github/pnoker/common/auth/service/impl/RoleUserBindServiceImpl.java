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
import io.github.pnoker.common.auth.dal.RoleUserBindManager;
import io.github.pnoker.common.auth.dal.UserManager;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.bo.RoleUserBindBO;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleUserBindBuilder;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.auth.entity.model.RoleUserBindDO;
import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.common.auth.entity.query.RoleUserBindQuery;
import io.github.pnoker.common.auth.service.RoleUserBindService;
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
 * Business service implementation for role-user binding operations.
 *
 * @author linys
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleUserBindServiceImpl implements RoleUserBindService {

    private final RoleUserBindBuilder roleUserBindBuilder;

    private final RoleBuilder roleBuilder;

    private final UserBuilder userBuilder;

    private final RoleUserBindManager roleUserBindManager;

    private final RoleManager roleManager;

    private final UserManager userManager;

    @Override
    public void add(RoleUserBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        RoleUserBindDO entityDO = roleUserBindBuilder.buildDOByBO(entityBO);
        if (!roleUserBindManager.save(entityDO)) {
            throw new AddException("Failed to create role user bind");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!roleUserBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove role user bind");
        }
    }

    @Override
    public void update(RoleUserBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        RoleUserBindDO entityDO = roleUserBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!roleUserBindManager.updateById(entityDO)) {
            throw new UpdateException("The role user bind update failed");
        }
    }

    @Override
    public RoleUserBindBO getById(Long id) {
        RoleUserBindDO entityDO = getDOById(id, true);
        return roleUserBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<RoleUserBindBO> list(RoleUserBindQuery entityQuery) {
        return list(entityQuery, null);
    }

    @Override
    public Page<RoleUserBindBO> list(RoleUserBindQuery entityQuery, Long tenantId) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RoleUserBindDO> entityPageDO = roleUserBindManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery, tenantId));
        return roleUserBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public List<UserBO> listUserByRoleId(Long roleId) {
        if (Objects.isNull(roleId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        wrapper.eq(RoleUserBindDO::getRoleId, roleId);
        wrapper.select(RoleUserBindDO::getUserId);
        List<Long> userIds = roleUserBindManager.listObjs(wrapper, o -> (Long) o);
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        List<UserDO> enabled = userManager.listByIds(userIds)
                .stream()
                .filter(e -> EnableFlagEnum.ENABLE.getIndex().equals(e.getEnableFlag()))
                .toList();
        return userBuilder.buildBOListByDOList(enabled);
    }

    @Override
    public List<RoleBO> listRoleByTenantIdAndUserId(Long tenantId, Long userId) {
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        wrapper.eq(RoleUserBindDO::getUserId, userId);
        List<RoleUserBindDO> roleUserBindBOList = roleUserBindManager.list(wrapper);
        if (CollectionUtils.isNotEmpty(roleUserBindBOList)) {
            List<RoleDO> roleBOList = roleManager
                    .listByIds(roleUserBindBOList.stream().map(RoleUserBindDO::getRoleId).toList());
            List<RoleDO> collect = roleBOList.stream()
                    .filter(e -> EnableFlagEnum.ENABLE.getIndex().equals(e.getEnableFlag())
                            && (Objects.isNull(tenantId) || tenantId.equals(e.getTenantId())))
                    .toList();
            return roleBuilder.buildBOListByDOList(collect);
        }

        return null;
    }

    /**
     * @param entityQuery {@link RoleUserBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<RoleUserBindDO> fuzzyQuery(RoleUserBindQuery entityQuery, Long tenantId) {
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getUserId()), RoleUserBindDO::getUserId,
                entityQuery.getUserId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getRoleId()), RoleUserBindDO::getRoleId,
                entityQuery.getRoleId());
        if (Objects.nonNull(tenantId)) {
            LambdaQueryWrapper<RoleDO> roleWrapper = Wrappers.<RoleDO>query().lambda();
            roleWrapper.eq(RoleDO::getTenantId, tenantId);
            roleWrapper.select(RoleDO::getId);
            List<Long> roleIds = roleManager.listObjs(roleWrapper, o -> (Long) o);
            if (CollectionUtils.isEmpty(roleIds)) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(RoleUserBindDO::getRoleId, roleIds);
            }
        }
        return wrapper;
    }

    /**
     * @param entityBO       {@link RoleUserBindBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(RoleUserBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<RoleUserBindDO> wrapper = Wrappers.<RoleUserBindDO>query().lambda();
        wrapper.eq(RoleUserBindDO::getRoleId, entityBO.getRoleId());
        wrapper.eq(RoleUserBindDO::getUserId, entityBO.getUserId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RoleUserBindDO one = roleUserBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Role user bind has been duplicated");
        }
        return duplicate;
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link RoleUserBindDO}
     */
    private RoleUserBindDO getDOById(Long id, boolean throwException) {
        RoleUserBindDO entityDO = roleUserBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Role user bind does not exist");
        }
        return entityDO;
    }

}

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
import io.github.pnoker.common.auth.dal.UserLoginManager;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.builder.UserLoginBuilder;
import io.github.pnoker.common.auth.entity.model.UserLoginDO;
import io.github.pnoker.common.auth.entity.query.UserLoginQuery;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.auth.service.UserLoginService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * Business service implementation for user login record operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {

    private final UserLoginBuilder userLoginBuilder;

    private final UserLoginManager userLoginManager;

    private final TenantBindService tenantBindService;

    @Override
    public void add(UserLoginBO entityBO) {
        checkDuplicate(entityBO, false, true);

        UserLoginDO entityDO = userLoginBuilder.buildDOByBO(entityBO);
        if (!userLoginManager.save(entityDO)) {
            throw new AddException("Failed to create user: {}", entityBO.toString());
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!userLoginManager.removeById(id)) {
            throw new DeleteException("Failed to remove user login");
        }
    }

    @Override
    public void update(UserLoginBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        UserLoginDO entityDO = userLoginBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!userLoginManager.updateById(entityDO)) {
            throw new UpdateException("The user login update failed");
        }
    }

    @Override
    public UserLoginBO getById(Long id) {
        UserLoginDO entityDO = getDOById(id, true);
        return userLoginBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<UserLoginBO> list(UserLoginQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<UserLoginDO> entityPageBO = userLoginManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return userLoginBuilder.buildBOPageByDOPage(entityPageBO);
    }

    @Override
    public UserLoginBO getByLoginName(String loginName, boolean throwException) {
        if (StringUtils.isEmpty(loginName)) {
            if (throwException) {
                throw new EmptyException("The login name is empty");
            }
            return null;
        }

        LambdaQueryWrapper<UserLoginDO> wrapper = Wrappers.<UserLoginDO>query().lambda();
        wrapper.eq(UserLoginDO::getLoginName, loginName);
        wrapper.eq(UserLoginDO::getEnableFlag, EnableFlagEnum.ENABLE);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserLoginDO userLogin = userLoginManager.getOne(wrapper);
        if (Objects.isNull(userLogin)) {
            if (throwException) {
                throw new NotFoundException();
            }
            return null;
        }
        return userLoginBuilder.buildBOByDO(userLogin);
    }

    @Override
    public boolean checkLoginNameValid(String loginName) {
        UserLoginBO userLogin = getByLoginName(loginName, false);
        if (Objects.nonNull(userLogin)) {
            return EnableFlagEnum.ENABLE.equals(userLogin.getEnableFlag());
        }

        return false;
    }

    @Override
    public boolean checkLoginNameAvailable(String loginName, Long tenantId) {
        List<Long> userIds = tenantBindService.listUserIdsByTenantId(tenantId);
        if (CollectionUtils.isEmpty(userIds)) {
            return true;
        }
        LambdaQueryWrapper<UserLoginDO> wrapper = Wrappers.<UserLoginDO>query().lambda();
        wrapper.eq(UserLoginDO::getLoginName, loginName);
        wrapper.in(UserLoginDO::getUserId, userIds);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        return Objects.isNull(userLoginManager.getOne(wrapper));
    }

    /**
     * @param entityQuery {@link UserLoginQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<UserLoginDO> fuzzyQuery(UserLoginQuery entityQuery) {
        LambdaQueryWrapper<UserLoginDO> wrapper = Wrappers.<UserLoginDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getLoginName()), UserLoginDO::getLoginName,
                entityQuery.getLoginName());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getUserId()), UserLoginDO::getUserId, entityQuery.getUserId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getUserPasswordId()), UserLoginDO::getUserPasswordId,
                entityQuery.getUserPasswordId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), UserLoginDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        if (Objects.nonNull(entityQuery.getTenantId())) {
            List<Long> userIds = tenantBindService.listUserIdsByTenantId(entityQuery.getTenantId());
            if (CollectionUtils.isEmpty(userIds)) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(UserLoginDO::getUserId, userIds);
            }
        }
        return wrapper;
    }

    /**
     * @param entityBO       {@link UserLoginBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(UserLoginBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<UserLoginDO> wrapper = Wrappers.<UserLoginDO>query().lambda();
        wrapper.eq(UserLoginDO::getLoginName, entityBO.getLoginName());
        wrapper.eq(UserLoginDO::getUserId, entityBO.getUserId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserLoginDO one = userLoginManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("User login has been duplicated");
        }
        return duplicate;
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link UserLoginDO}
     */
    private UserLoginDO getDOById(Long id, boolean throwException) {
        UserLoginDO entityDO = userLoginManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("User login does not exist");
        }
        return entityDO;
    }

}

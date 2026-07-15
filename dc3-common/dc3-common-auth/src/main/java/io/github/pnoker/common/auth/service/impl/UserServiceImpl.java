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
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.UserManager;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.common.auth.entity.query.UserQuery;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.auth.service.UserService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.EmptyException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * User service implementation.
 *
 * <p>
 * Provides CRUD operations and lookup utilities for user entities.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserBuilder userBuilder;

    private final UserManager userManager;

    private final TenantMembershipService tenantMembershipService;

    @Override
    public void add(UserBO entityBO) {
        checkDuplicate(entityBO, false, true);

        // When phone number is present, check whether it is already occupied.
        if (StringUtils.isNotEmpty(entityBO.getPhone())) {
            UserBO existingByPhone = getByPhone(entityBO.getPhone(), false);
            if (Objects.nonNull(existingByPhone)) {
                throw new DuplicateException("The user already exists with phone: {}", entityBO.getPhone());
            }
        }

        // When email is present, check whether it is already occupied.
        if (StringUtils.isNotEmpty(entityBO.getEmail())) {
            UserBO existingByEmail = getByEmail(entityBO.getEmail(), false);
            if (Objects.nonNull(existingByEmail)) {
                throw new DuplicateException("The user already exists with email: {}", entityBO.getEmail());
            }
        }

        UserDO entityDO = userBuilder.buildDOByBO(entityBO);
        if (!userManager.save(entityDO)) {
            throw new AddException("Failed to create user: {}", entityBO.toString());
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!userManager.removeById(id)) {
            throw new DeleteException("Failed to remove user");
        }
    }

    @Override
    public void update(UserBO entityBO) {
        UserDO existingDO = getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        // Check whether phone number is updated.
        if (StringUtils.isNotEmpty(entityBO.getPhone())) {
            if (!entityBO.getPhone().equals(existingDO.getPhone())) {
                UserBO existingByPhone = getByPhone(entityBO.getPhone(), false);
                if (Objects.nonNull(existingByPhone)) {
                    throw new DuplicateException("The user already exists with phone {}", entityBO.getPhone());
                }
            }
        }

        // Check whether email is updated.
        if (StringUtils.isNotEmpty(entityBO.getEmail())) {
            if (!entityBO.getEmail().equals(existingDO.getEmail())) {
                UserBO existingByEmail = getByEmail(entityBO.getEmail(), false);
                if (Objects.nonNull(existingByEmail)) {
                    throw new DuplicateException("The user already exists with email {}", entityBO.getEmail());
                }
            }
        }

        UserDO entityDO = userBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!userManager.updateById(entityDO)) {
            throw new UpdateException("The user update failed");
        }
    }

    @Override
    public UserBO getById(Long id) {
        UserDO entityDO = getDOById(id, true);
        return userBuilder.buildBOByDO(entityDO);
    }

    @Override
    public UserBO getByUserName(String userName, boolean throwException) {
        if (StringUtils.isEmpty(userName)) {
            if (throwException) {
                throw new EmptyException("The name is empty");
            }
            return null;
        }

        return selectByKey(UserDO::getUserName, userName, throwException);
    }

    @Override
    public UserBO getByPhone(String phone, boolean throwException) {
        if (StringUtils.isEmpty(phone)) {
            if (throwException) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(UserDO::getPhone, phone, throwException);
    }

    @Override
    public UserBO getByEmail(String email, boolean throwException) {
        if (StringUtils.isEmpty(email)) {
            if (throwException) {
                throw new EmptyException("The email is empty");
            }
            return null;
        }

        return selectByKey(UserDO::getEmail, email, throwException);
    }

    @Override
    public UserBO getByPrincipalId(Long principalId, boolean throwException) {
        if (Objects.isNull(principalId)) {
            if (throwException) {
                throw new EmptyException("The principal id is empty");
            }
            return null;
        }

        return selectByKey(UserDO::getPrincipalId, principalId, throwException);
    }

    @Override
    public Page<UserBO> list(UserQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<UserDO> page = userManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return userBuilder.buildBOPageByDOPage(page);
    }

    /**
     * Build fuzzy query wrapper for user search.
     *
     * @param entityQuery {@link UserQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link UserDO}
     */
    private LambdaQueryWrapper<UserDO> fuzzyQuery(UserQuery entityQuery) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.<UserDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getNickName()), UserDO::getNickName, entityQuery.getNickName());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getUserName()), UserDO::getUserName, entityQuery.getUserName());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getPhone()), UserDO::getPhone, entityQuery.getPhone());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getEmail()), UserDO::getEmail, entityQuery.getEmail());
        wrapper.eq(Objects.nonNull(entityQuery.getPrincipalId()), UserDO::getPrincipalId, entityQuery.getPrincipalId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), UserDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        // Tenant scope. Users have no tenant_id column; principals join tenants through
        // dc3_tenant_membership.
        if (Objects.nonNull(entityQuery.getTenantId())) {
            List<Long> principalIds = tenantMembershipService.listPrincipalIdsByTenantId(entityQuery.getTenantId());
            if (CollectionUtils.isEmpty(principalIds)) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(UserDO::getPrincipalId, principalIds);
            }
        }
        return wrapper;
    }

    /**
     * Query a single user by a dynamic key field, optionally throwing when not found.
     *
     * @param key           the lambda field reference to match
     * @param value         the value to match
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return the user BO, or null when not found and throwException is false
     */
    private UserBO selectByKey(SFunction<UserDO, ?> key, Object value, boolean throwException) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.<UserDO>query().lambda();
        wrapper.eq(key, value);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserDO userDO = userManager.getOne(wrapper);
        if (Objects.isNull(userDO)) {
            if (throwException) {
                throw new NotFoundException();
            }
            return null;
        }
        return userBuilder.buildBOByDO(userDO);
    }

    /**
     * Check whether a user is duplicated by username.
     *
     * @param entityBO       {@link UserBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(UserBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.<UserDO>query().lambda();
        wrapper.eq(UserDO::getUserName, entityBO.getUserName());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserDO one = userManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("User has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get user data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link UserDO} if found, otherwise {@code null} when {@code throwException}
     * is false
     */
    private UserDO getDOById(Long id, boolean throwException) {
        UserDO entityDO = userManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("User does not exist");
        }
        return entityDO;
    }

}

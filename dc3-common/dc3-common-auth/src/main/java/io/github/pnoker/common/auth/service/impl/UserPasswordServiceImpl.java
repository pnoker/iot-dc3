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
import io.github.pnoker.common.auth.dal.UserPasswordManager;
import io.github.pnoker.common.auth.entity.bo.UserPasswordBO;
import io.github.pnoker.common.auth.entity.builder.UserPasswordBuilder;
import io.github.pnoker.common.auth.entity.model.UserPasswordDO;
import io.github.pnoker.common.auth.entity.query.UserPasswordQuery;
import io.github.pnoker.common.auth.service.UserPasswordService;
import io.github.pnoker.common.constant.common.AlgorithmConstant;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.PageUtil;
import io.github.pnoker.common.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Business service implementation for user password operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPasswordServiceImpl implements UserPasswordService {

    private final UserPasswordBuilder userPasswordBuilder;

    private final UserPasswordManager userPasswordManager;

    @Override
    public void add(UserPasswordBO entityBO) {
        checkDuplicate(entityBO, false, true);

        UserPasswordDO entityDO = userPasswordBuilder.buildDOByBO(entityBO);
        String prehashed = DecodeUtil.md5(entityDO.getLoginPassword());
        entityDO.setLoginPassword(PasswordUtil.encode(prehashed));
        if (!userPasswordManager.save(entityDO)) {
            throw new AddException("Failed to create user password");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!userPasswordManager.removeById(id)) {
            throw new DeleteException("Failed to remove user password");
        }
    }

    @Override
    public void update(UserPasswordBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        UserPasswordDO entityDO = userPasswordBuilder.buildDOByBO(entityBO);
        String prehashed = DecodeUtil.md5(entityDO.getLoginPassword());
        entityDO.setLoginPassword(PasswordUtil.encode(prehashed));
        entityDO.setOperateTime(null);
        if (!userPasswordManager.updateById(entityDO)) {
            throw new UpdateException("The user password update failed");
        }
    }

    @Override
    public UserPasswordBO getById(Long id) {
        UserPasswordDO entityDO = getDOById(id, true);
        return userPasswordBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<UserPasswordBO> list(UserPasswordQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<UserPasswordDO> entityPageDO = userPasswordManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return userPasswordBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public void restPassword(Long id) {
        UserPasswordBO userPasswordBO = getById(id);
        if (Objects.nonNull(userPasswordBO)) {
            userPasswordBO.setLoginPassword(AlgorithmConstant.DEFAULT_PASSWORD);
            update(userPasswordBO);
        }
    }

    /**
     * @param entityQuery {@link UserPasswordQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<UserPasswordDO> fuzzyQuery(UserPasswordQuery entityQuery) {
        LambdaQueryWrapper<UserPasswordDO> wrapper = Wrappers.<UserPasswordDO>query().lambda();
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getLoginPassword()), UserPasswordDO::getLoginPassword,
                entityQuery.getLoginPassword());
        return wrapper;
    }

    /**
     * @param entityBO       {@link UserPasswordBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(UserPasswordBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<UserPasswordDO> wrapper = Wrappers.<UserPasswordDO>query().lambda();
        wrapper.eq(UserPasswordDO::getLoginPassword, entityBO.getLoginPassword());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserPasswordDO one = userPasswordManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("User password has been duplicated");
        }
        return duplicate;
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link UserPasswordDO}
     */
    private UserPasswordDO getDOById(Long id, boolean throwException) {
        UserPasswordDO entityDO = userPasswordManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("User password does not exist");
        }
        return entityDO;
    }

}

/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.UserLoginBO;
import io.github.pnoker.center.auth.entity.builder.UserLoginBuilder;
import io.github.pnoker.center.auth.entity.model.UserLoginDO;
import io.github.pnoker.center.auth.entity.query.UserLoginQuery;
import io.github.pnoker.center.auth.manager.UserLoginManager;
import io.github.pnoker.center.auth.service.UserLoginService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 用户服务接口实现类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class UserLoginServiceImpl implements UserLoginService {

    @Resource
    private UserLoginBuilder userLoginBuilder;

    @Resource
    private UserLoginManager userLoginManager;

    @Override
    @Transactional
    public void save(UserLoginBO entityBO) {
        // 判断登录名称是否存在
        UserLoginBO selectByLoginName = selectByLoginName(entityBO.getLoginName(), false);
        if (ObjectUtil.isNotNull(selectByLoginName)) {
            throw new DuplicateException("The user already exists with login name: {}", entityBO.getLoginName());
        }

        UserLoginDO entityDO = userLoginBuilder.buildDOByBO(entityBO);
        // 插入 user 数据，并返回插入后的 user
        if (!userLoginManager.save(entityDO)) {
            throw new AddException("The user add failed: {}", entityBO.toString());
        }
    }

    @Override
    @Transactional
    public void remove(Long id) {
        UserLoginBO userLogin = selectById(id);
        if (ObjectUtil.isNull(userLogin)) {
            throw new NotFoundException("The user login does not exist");
        }

        if (!userLoginManager.removeById(id)) {
            throw new DeleteException("The user login delete failed");
        }
    }

    @Override
    public void update(UserLoginBO entityBO) {
        UserLoginBO selectById = selectById(entityBO.getId());
        if (ObjectUtil.isNull(selectById)) {
            throw new NotFoundException("The user login does not exist");
        }
        entityBO.setLoginName(null);
        entityBO.setOperateTime(null);
        UserLoginDO entityDO = userLoginBuilder.buildDOByBO(entityBO);
        if (!userLoginManager.updateById(entityDO)) {
            throw new UpdateException("The user login update failed");
        }
    }

    @Override
    public UserLoginBO selectById(Long id) {
        return null;
    }

    @Override
    public Page<UserLoginBO> selectByPage(UserLoginQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<UserLoginDO> entityPageBO = userLoginManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return userLoginBuilder.buildBOPageByDOPage(entityPageBO);
    }

    @Override
    public UserLoginBO selectByLoginName(String loginName, boolean throwException) {
        if (CharSequenceUtil.isEmpty(loginName)) {
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
        if (ObjectUtil.isNull(userLogin)) {
            throw new NotFoundException();
        }
        return userLoginBuilder.buildBOByDO(userLogin);
    }

    @Override
    public boolean checkLoginNameValid(String loginName) {
        UserLoginBO userLogin = selectByLoginName(loginName, false);
        if (ObjectUtil.isNotNull(userLogin)) {
            return EnableFlagEnum.ENABLE.equals(userLogin.getEnableFlag());
        }

        return false;
    }

    private LambdaQueryWrapper<UserLoginDO> fuzzyQuery(UserLoginQuery query) {
        LambdaQueryWrapper<UserLoginDO> wrapper = Wrappers.<UserLoginDO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getLoginName()), UserLoginDO::getLoginName, query.getLoginName());
        }
        return wrapper;
    }

}

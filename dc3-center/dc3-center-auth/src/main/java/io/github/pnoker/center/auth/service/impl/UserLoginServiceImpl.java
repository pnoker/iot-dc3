/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.center.auth.entity.query.UserLoginPageQuery;
import io.github.pnoker.center.auth.mapper.UserLoginMapper;
import io.github.pnoker.center.auth.service.UserLoginService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.UserLogin;
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
    private UserLoginMapper userLoginMapper;

    @Override
    @Transactional
    public void add(UserLogin entityDO) {
        // 判断登录名称是否存在
        UserLogin selectByLoginName = selectByLoginName(entityDO.getLoginName(), false);
        if (ObjectUtil.isNotNull(selectByLoginName)) {
            throw new DuplicateException("The user already exists with login name: {}", entityDO.getLoginName());
        }

        // 插入 user 数据，并返回插入后的 user
        if (userLoginMapper.insert(entityDO) < 1) {
            throw new AddException("The user add failed: {}", entityDO.toString());
        }
    }

    @Override
    @Transactional
    public void delete(String id) {
        UserLogin userLogin = selectById(id);
        if (ObjectUtil.isNull(userLogin)) {
            throw new NotFoundException("The user login does not exist");
        }

        if (userLoginMapper.deleteById(id) < 1) {
            throw new DeleteException("The user login delete failed");
        }
    }

    @Override
    public void update(UserLogin entityDO) {
        UserLogin selectById = selectById(entityDO.getId());
        if (ObjectUtil.isNull(selectById)) {
            throw new NotFoundException("The user login does not exist");
        }
        entityDO.setLoginName(null);
        entityDO.setOperateTime(null);
        if (userLoginMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The user login update failed");
        }
    }

    @Override
    public UserLogin selectById(String id) {
        return userLoginMapper.selectById(id);
    }

    @Override
    public Page<UserLogin> list(UserLoginPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return userLoginMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    @Override
    public UserLogin selectByLoginName(String loginName, boolean throwException) {
        if (CharSequenceUtil.isEmpty(loginName)) {
            if (throwException) {
                throw new EmptyException("The login name is empty");
            }
            return null;
        }

        LambdaQueryWrapper<UserLogin> queryWrapper = Wrappers.<UserLogin>query().lambda();
        queryWrapper.eq(UserLogin::getLoginName, loginName);
        queryWrapper.eq(UserLogin::getEnableFlag, EnableFlagEnum.ENABLE);
        queryWrapper.last("limit 1");
        UserLogin userLogin = userLoginMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(userLogin)) {
            throw new NotFoundException();
        }
        return userLogin;
    }

    @Override
    public boolean checkLoginNameValid(String loginName) {
        UserLogin userLogin = selectByLoginName(loginName, false);
        if (ObjectUtil.isNotNull(userLogin)) {
            return EnableFlagEnum.ENABLE.equals(userLogin.getEnableFlag());
        }

        return false;
    }

    private LambdaQueryWrapper<UserLogin> fuzzyQuery(UserLoginPageQuery query) {
        LambdaQueryWrapper<UserLogin> queryWrapper = Wrappers.<UserLogin>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getLoginName()), UserLogin::getLoginName, query.getLoginName());
        }
        return queryWrapper;
    }

}

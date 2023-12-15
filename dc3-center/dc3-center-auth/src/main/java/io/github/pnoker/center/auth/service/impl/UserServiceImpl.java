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
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.UserBO;
import io.github.pnoker.center.auth.entity.query.UserQuery;
import io.github.pnoker.center.auth.mapper.UserMapper;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
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
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional
    public void save(UserBO entityBO) {
        // 判断用户是否存在
        UserBO selectByUserNameBO = selectByUserName(entityBO.getUserName(), false);
        if (ObjectUtil.isNotNull(selectByUserNameBO)) {
            throw new DuplicateException("The user already exists with userName: {}", entityBO.getUserName());
        }

        // 判断 phone 是否存在，如果有 phone 不为空，检查该 phone 是否被占用
        if (CharSequenceUtil.isNotEmpty(entityBO.getPhone())) {
            UserBO selectByPhone = selectByPhone(entityBO.getPhone(), false);
            if (ObjectUtil.isNotNull(selectByPhone)) {
                throw new DuplicateException("The user already exists with phone: {}", entityBO.getPhone());
            }
        }

        // 判断 email 是否存在，如果有 email 不为空，检查该 email 是否被占用
        if (CharSequenceUtil.isNotEmpty(entityBO.getEmail())) {
            UserBO selectByEmail = selectByEmail(entityBO.getEmail(), false);
            if (ObjectUtil.isNotNull(selectByEmail)) {
                throw new DuplicateException("The user already exists with email: {}", entityBO.getEmail());
            }
        }

        // 插入 user 数据，并返回插入后的 user
        if (userMapper.insert(entityBO) < 1) {
            throw new AddException("The user add failed: {}", entityBO.toString());
        }
    }

    @Override
    @Transactional
    public void remove(Long id) {
        UserBO userBO = selectById(id);
        if (ObjectUtil.isNull(userBO)) {
            throw new NotFoundException("The user does not exist");
        }

        if (userMapper.deleteById(id) < 1) {
            throw new DeleteException("The user delete failed");
        }
    }

    @Override
    public void update(UserBO entityBO) {
        UserBO selectById = selectById(entityBO.getId());
        // 判断 phone 是否更新
        if (CharSequenceUtil.isNotEmpty(entityBO.getPhone())) {
            if (!entityBO.getPhone().equals(selectById.getPhone())) {
                UserBO selectByPhone = selectByPhone(entityBO.getPhone(), false);
                if (ObjectUtil.isNotNull(selectByPhone)) {
                    throw new DuplicateException("The user already exists with phone {}", entityBO.getPhone());
                }
            }
        } else {
            entityBO.setPhone(null);
        }

        // 判断 email 是否更新
        if (CharSequenceUtil.isNotEmpty(entityBO.getEmail())) {
            if (!entityBO.getEmail().equals(selectById.getEmail())) {
                UserBO selectByEmail = selectByEmail(entityBO.getEmail(), false);
                if (ObjectUtil.isNotNull(selectByEmail)) {
                    throw new DuplicateException("The user already exists with email {}", entityBO.getEmail());
                }
            }
        } else {
            entityBO.setEmail(null);
        }

        entityBO.setUserName(null);
        entityBO.setOperateTime(null);
        if (userMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The user update failed");
        }
    }

    @Override
    public UserBO selectById(Long id) {
        return null;
    }

    public UserBO selectByUserName(String userName, boolean throwException) {
        if (CharSequenceUtil.isEmpty(userName)) {
            if (throwException) {
                throw new EmptyException("The name is empty");
            }
            return null;
        }

        return selectByKey(UserBO::getUserName, userName, throwException);
    }

    @Override
    public UserBO selectByPhone(String phone, boolean throwException) {
        if (CharSequenceUtil.isEmpty(phone)) {
            if (throwException) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(UserBO::getPhone, phone, throwException);
    }

    @Override
    public UserBO selectByEmail(String email, boolean throwException) {
        if (CharSequenceUtil.isEmpty(email)) {
            if (throwException) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(UserBO::getEmail, email, throwException);
    }

    @Override
    public Page<UserBO> selectByPage(UserQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return userMapper.selectPage(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<UserBO> fuzzyQuery(UserQuery query) {
        LambdaQueryWrapper<UserBO> wrapper = Wrappers.<UserBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getNickName()), UserBO::getNickName, query.getNickName());
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getUserName()), UserBO::getUserName, query.getUserName());
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getPhone()), UserBO::getPhone, query.getPhone());
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getEmail()), UserBO::getEmail, query.getEmail());
        }
        return wrapper;
    }

    private UserBO selectByKey(SFunction<UserBO, ?> key, String value, boolean throwException) {
        LambdaQueryWrapper<UserBO> wrapper = Wrappers.<UserBO>query().lambda();
        wrapper.eq(key, value);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserBO userBO = userMapper.selectOne(wrapper);
        if (ObjectUtil.isNull(userBO)) {
            if (throwException) {
                throw new NotFoundException();
            }
            return null;
        }
        return userBO;
    }

}

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
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.query.UserDto;
import io.github.pnoker.center.auth.mapper.UserMapper;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.User;
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
    public User add(User user) {
        // todo 不通过，会返回密码数据
        // 判断用户是否存在
        User selectByUserName = selectByUserName(user.getUserName(), false);
        if (ObjectUtil.isNotNull(selectByUserName)) {
            throw new DuplicateException("The user already exists with username: {}", user.getUserName());
        }

        // 判断 phone 是否存在，如果有 phone 不为空，检查该 phone 是否被占用
        if (CharSequenceUtil.isNotEmpty(user.getPhone())) {
            User selectByPhone = selectByPhone(user.getPhone(), false);
            if (ObjectUtil.isNotNull(selectByPhone)) {
                throw new DuplicateException("The user already exists with phone: {}", user.getPhone());
            }
        }

        // 判断 email 是否存在，如果有 email 不为空，检查该 email 是否被占用
        if (CharSequenceUtil.isNotEmpty(user.getEmail())) {
            User selectByEmail = selectByEmail(user.getEmail(), false);
            if (ObjectUtil.isNotNull(selectByEmail)) {
                throw new DuplicateException("The user already exists with email: {}", user.getEmail());
            }
        }

        // 插入 user 数据，并返回插入后的 user
        if (userMapper.insert(user) > 0) {
            return userMapper.selectById(user.getId());
        }

        throw new AddException("The user add failed: {}", user.toString());
    }

    @Override
    @Transactional
    public Boolean delete(String id) {
        User user = selectById(id);
        if (ObjectUtil.isNull(user)) {
            throw new NotFoundException();
        }
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public User update(User user) {
        User selectById = selectById(user.getId());
        // 判断 phone 是否修改
        if (CharSequenceUtil.isNotEmpty(user.getPhone())) {
            if (!user.getPhone().equals(selectById.getPhone())) {
                User selectByPhone = selectByPhone(user.getPhone(), false);
                if (ObjectUtil.isNotNull(selectByPhone)) {
                    throw new DuplicateException("The user already exists with phone {}", user.getPhone());
                }
            }
        } else {
            user.setPhone(null);
        }

        // 判断 email 是否修改
        if (CharSequenceUtil.isNotEmpty(user.getEmail())) {
            if (!user.getEmail().equals(selectById.getEmail())) {
                User selectByEmail = selectByEmail(user.getEmail(), false);
                if (ObjectUtil.isNotNull(selectByEmail)) {
                    throw new DuplicateException("The user already exists with email {}", user.getEmail());
                }
            }
        } else {
            user.setEmail(null);
        }

        user.setUserName(null);
        user.setOperateTime(null);
        if (userMapper.updateById(user) > 0) {
            User select = userMapper.selectById(user.getId());
            user.setUserName(select.getUserName());
            return select;
        }
        throw new ServiceException("The user update failed");
    }

    @Override
    public User selectById(String id) {
        return userMapper.selectById(id);
    }

    public User selectByUserName(String userName, boolean throwException) {
        if (CharSequenceUtil.isEmpty(userName)) {
            if (throwException) {
                throw new EmptyException("The name is empty");
            }
            return null;
        }

        return selectByKey(User::getUserName, userName, throwException);
    }

    @Override
    public User selectByPhone(String phone, boolean throwException) {
        if (CharSequenceUtil.isEmpty(phone)) {
            if (throwException) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(User::getPhone, phone, throwException);
    }

    @Override
    public User selectByEmail(String email, boolean throwException) {
        if (CharSequenceUtil.isEmpty(email)) {
            if (throwException) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(User::getEmail, email, throwException);
    }

    @Override
    public Page<User> list(UserDto entityDTO) {
        if (ObjectUtil.isNull(entityDTO.getPage())) {
            entityDTO.setPage(new Pages());
        }
        return userMapper.selectPage(entityDTO.getPage().convert(), fuzzyQuery(entityDTO));
    }

    @Override
    public LambdaQueryWrapper<User> fuzzyQuery(UserDto entityDTO) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        if (ObjectUtil.isNotNull(entityDTO)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(entityDTO.getNickName()), User::getNickName, entityDTO.getNickName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(entityDTO.getUserName()), User::getUserName, entityDTO.getUserName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(entityDTO.getPhone()), User::getPhone, entityDTO.getPhone());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(entityDTO.getEmail()), User::getEmail, entityDTO.getEmail());
        }
        return queryWrapper;
    }

    private User selectByKey(SFunction<User, ?> key, String value, boolean throwException) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        queryWrapper.eq(key, value);
        queryWrapper.last("limit 1");
        User user = userMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(user)) {
            if (throwException) {
                throw new NotFoundException();
            }
            return null;
        }
        return user;
    }

}

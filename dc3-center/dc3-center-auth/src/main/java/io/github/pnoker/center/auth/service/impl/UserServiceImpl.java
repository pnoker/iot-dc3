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
    public void add(User entityDO) {
        // 判断用户是否存在
        User selectByUserName = selectByUserName(entityDO.getUserName(), false);
        if (ObjectUtil.isNotNull(selectByUserName)) {
            throw new DuplicateException("The user already exists with username: {}", entityDO.getUserName());
        }

        // 判断 phone 是否存在，如果有 phone 不为空，检查该 phone 是否被占用
        if (CharSequenceUtil.isNotEmpty(entityDO.getPhone())) {
            User selectByPhone = selectByPhone(entityDO.getPhone(), false);
            if (ObjectUtil.isNotNull(selectByPhone)) {
                throw new DuplicateException("The user already exists with phone: {}", entityDO.getPhone());
            }
        }

        // 判断 email 是否存在，如果有 email 不为空，检查该 email 是否被占用
        if (CharSequenceUtil.isNotEmpty(entityDO.getEmail())) {
            User selectByEmail = selectByEmail(entityDO.getEmail(), false);
            if (ObjectUtil.isNotNull(selectByEmail)) {
                throw new DuplicateException("The user already exists with email: {}", entityDO.getEmail());
            }
        }

        // 插入 user 数据，并返回插入后的 user
        if (userMapper.insert(entityDO) < 1) {
            throw new AddException("The user add failed: {}", entityDO.toString());
        }
    }

    @Override
    @Transactional
    public void delete(String id) {
        User user = selectById(id);
        if (ObjectUtil.isNull(user)) {
            throw new NotFoundException("The user does not exist");
        }

        if (userMapper.deleteById(id) < 1) {
            throw new DeleteException("The user delete failed");
        }
    }

    @Override
    public void update(User entityDO) {
        User selectById = selectById(entityDO.getId());
        // 判断 phone 是否修改
        if (CharSequenceUtil.isNotEmpty(entityDO.getPhone())) {
            if (!entityDO.getPhone().equals(selectById.getPhone())) {
                User selectByPhone = selectByPhone(entityDO.getPhone(), false);
                if (ObjectUtil.isNotNull(selectByPhone)) {
                    throw new DuplicateException("The user already exists with phone {}", entityDO.getPhone());
                }
            }
        } else {
            entityDO.setPhone(null);
        }

        // 判断 email 是否修改
        if (CharSequenceUtil.isNotEmpty(entityDO.getEmail())) {
            if (!entityDO.getEmail().equals(selectById.getEmail())) {
                User selectByEmail = selectByEmail(entityDO.getEmail(), false);
                if (ObjectUtil.isNotNull(selectByEmail)) {
                    throw new DuplicateException("The user already exists with email {}", entityDO.getEmail());
                }
            }
        } else {
            entityDO.setEmail(null);
        }

        entityDO.setUserName(null);
        entityDO.setOperateTime(null);
        if (userMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The user update failed");
        }
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
    public Page<User> list(UserDto queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return userMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<User> fuzzyQuery(UserDto query) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getNickName()), User::getNickName, query.getNickName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getUserName()), User::getUserName, query.getUserName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getPhone()), User::getPhone, query.getPhone());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getEmail()), User::getEmail, query.getEmail());
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

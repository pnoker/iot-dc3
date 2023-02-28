/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import io.github.pnoker.api.center.auth.dto.UserExtDto;
import io.github.pnoker.center.auth.mapper.UserExtMapper;
import io.github.pnoker.center.auth.service.UserExtService;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.common.model.UserExt;
import io.github.pnoker.common.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 用户拓展服务接口实现类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class UserExtServiceImpl implements UserExtService {

    @Resource
    private UserExtMapper userExtMapper;

    @Override
    @Transactional
    public UserExt add(UserExt userExt) {
        // todo 不通过，会返回密码数据
        // 判断用户是否存在
        UserExt selectByUserName = selectByUserName(userExt.getUserName(), false);
        if (ObjectUtil.isNotNull(selectByUserName)) {
            throw new DuplicateException("The user already exists with username: {}", userExt.getUserName());
        }

        // 判断 phone 是否存在，如果有 phone 不为空，检查该 phone 是否被占用
        if (CharSequenceUtil.isNotBlank(userExt.getPhone())) {
            UserExt selectByPhone = selectByPhone(userExt.getPhone(), false);
            if (ObjectUtil.isNotNull(selectByPhone)) {
                throw new DuplicateException("The user already exists with phone: {}", userExt.getPhone());
            }
        }

        // 判断 email 是否存在，如果有 email 不为空，检查该 email 是否被占用
        if (CharSequenceUtil.isNotBlank(userExt.getEmail())) {
            UserExt selectByEmail = selectByEmail(userExt.getEmail(), false);
            if (ObjectUtil.isNotNull(selectByEmail)) {
                throw new DuplicateException("The user already exists with email: {}", userExt.getEmail());
            }
        }

        // 插入 user 数据，并返回插入后的 user
        if (userExtMapper.insert(userExt) > 0) {
            return userExtMapper.selectById(userExt.getId());
        }

        throw new AddException("The user add failed: {}", userExt.toString());
    }

    @Override
    @Transactional
    public Boolean delete(String id) {
        UserExt userExt = selectById(id);
        if (null == userExt) {
            throw new NotFoundException();
        }
        return userExtMapper.deleteById(id) > 0;
    }

    @Override
    public UserExt update(UserExt userExt) {
        UserExt selectById = selectById(userExt.getId());
        // 判断 phone 是否修改
        if (CharSequenceUtil.isNotBlank(userExt.getPhone())) {
            if (!userExt.getPhone().equals(selectById.getPhone())) {
                UserExt selectByPhone = selectByPhone(userExt.getPhone(), false);
                if (ObjectUtil.isNotNull(selectByPhone)) {
                    throw new DuplicateException("The user already exists with phone {}", userExt.getPhone());
                }
            }
        } else {
            userExt.setPhone(null);
        }

        // 判断 email 是否修改
        if (CharSequenceUtil.isNotBlank(userExt.getEmail())) {
            if (!userExt.getEmail().equals(selectById.getEmail())) {
                UserExt selectByEmail = selectByEmail(userExt.getEmail(), false);
                if (ObjectUtil.isNotNull(selectByEmail)) {
                    throw new DuplicateException("The user already exists with email {}", userExt.getEmail());
                }
            }
        } else {
            userExt.setEmail(null);
        }

        userExt.setUserName(null);
        userExt.setUpdateTime(null);
        if (userExtMapper.updateById(userExt) > 0) {
            UserExt select = userExtMapper.selectById(userExt.getId());
            userExt.setUserName(select.getUserName());
            return select;
        }
        throw new ServiceException("The user update failed");
    }

    @Override
    public UserExt selectById(String id) {
        return userExtMapper.selectById(id);
    }

    public UserExt selectByUserName(String userName, boolean isEx) {
        if (CharSequenceUtil.isEmpty(userName)) {
            if (isEx) {
                throw new EmptyException("The name is empty");
            }
            return null;
        }

        return selectByKey(UserExt::getUserName, userName, isEx);
    }

    @Override
    public UserExt selectByPhone(String phone, boolean isEx) {
        if (CharSequenceUtil.isEmpty(phone)) {
            if (isEx) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(UserExt::getPhone, phone, isEx);
    }

    @Override
    public UserExt selectByEmail(String email, boolean isEx) {
        if (CharSequenceUtil.isEmpty(email)) {
            if (isEx) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(UserExt::getEmail, email, isEx);
    }

    @Override
    public Page<UserExt> list(UserExtDto userExtDto) {
        if (ObjectUtil.isNull(userExtDto.getPage())) {
            userExtDto.setPage(new Pages());
        }
        return userExtMapper.selectPage(userExtDto.getPage().convert(), fuzzyQuery(userExtDto));
    }

    @Override
    public LambdaQueryWrapper<UserExt> fuzzyQuery(UserExtDto userExtDto) {
        LambdaQueryWrapper<UserExt> queryWrapper = Wrappers.<UserExt>query().lambda();
        if (ObjectUtil.isNotNull(userExtDto)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(userExtDto.getNickName()), UserExt::getNickName, userExtDto.getNickName());
            queryWrapper.like(CharSequenceUtil.isNotBlank(userExtDto.getUserName()), UserExt::getUserName, userExtDto.getUserName());
            queryWrapper.like(CharSequenceUtil.isNotBlank(userExtDto.getPhone()), UserExt::getPhone, userExtDto.getPhone());
            queryWrapper.like(CharSequenceUtil.isNotBlank(userExtDto.getEmail()), UserExt::getEmail, userExtDto.getEmail());
        }
        return queryWrapper;
    }

    private UserExt selectByKey(SFunction<UserExt, ?> key, String value, boolean isEx) {
        LambdaQueryWrapper<UserExt> queryWrapper = Wrappers.<UserExt>query().lambda();
        queryWrapper.eq(key, value);
        UserExt userExt = userExtMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(userExt)) {
            if (isEx) {
                throw new NotFoundException();
            }
            return null;
        }
        return userExt;
    }

}

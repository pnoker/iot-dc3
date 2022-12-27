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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.auth.dto.UserPasswordDto;
import io.github.pnoker.center.auth.mapper.UserPasswordMapper;
import io.github.pnoker.center.auth.service.UserPasswordService;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.common.constant.common.AlgorithmConstant;
import io.github.pnoker.common.entity.UserPassword;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.utils.DecodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 用户密码服务接口实现类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class UserPasswordServiceImpl implements UserPasswordService {

    @Resource
    private UserPasswordMapper userPasswordMapper;

    @Override
    @Transactional
    public UserPassword add(UserPassword userPassword) {
        userPassword.setPassword(DecodeUtil.md5(userPassword.getPassword()));
        // 插入 userPassword 数据，并返回插入后的 userPassword
        if (userPasswordMapper.insert(userPassword) > 0) {
            return userPasswordMapper.selectById(userPassword.getId());
        }

        throw new AddException("The user password add failed: {}", userPassword.toString());
    }

    @Override
    @Transactional
    public Boolean delete(String id) {
        UserPassword userPassword = selectById(id);
        if (null == userPassword) {
            throw new NotFoundException();
        }
        return userPasswordMapper.deleteById(id) > 0;
    }

    @Override
    public UserPassword update(UserPassword userPassword) {
        UserPassword selectById = selectById(userPassword.getId());
        if (null == selectById) {
            throw new NotFoundException();
        }
        userPassword.setPassword(DecodeUtil.md5(userPassword.getPassword()));
        userPassword.setUpdateTime(null);
        if (userPasswordMapper.updateById(userPassword) > 0) {
            return userPasswordMapper.selectById(userPassword.getId());
        }
        throw new ServiceException("The user password update failed");
    }

    @Override
    public UserPassword selectById(String id) {
        return userPasswordMapper.selectById(id);
    }

    @Override
    public Page<UserPassword> list(UserPasswordDto userPasswordDto) {
        if (ObjectUtil.isNull(userPasswordDto.getPage())) {
            userPasswordDto.setPage(new Pages());
        }
        return userPasswordMapper.selectPage(userPasswordDto.getPage().convert(), fuzzyQuery(userPasswordDto));
    }

    @Override
    public Boolean restPassword(String id) {
        UserPassword userPassword = selectById(id);
        if (ObjectUtil.isNotNull(userPassword)) {
            userPassword.setPassword(DecodeUtil.md5(AlgorithmConstant.DEFAULT_PASSWORD));
            return null != update(userPassword);
        }
        return false;
    }

    @Override
    public LambdaQueryWrapper<UserPassword> fuzzyQuery(UserPasswordDto userPasswordDto) {
        return Wrappers.<UserPassword>query().lambda();
    }

}

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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.query.UserPasswordPageQuery;
import io.github.pnoker.center.auth.mapper.UserPasswordMapper;
import io.github.pnoker.center.auth.service.UserPasswordService;
import io.github.pnoker.common.constant.common.AlgorithmConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.model.UserPassword;
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
    public void add(UserPassword entityDO) {
        entityDO.setLoginPassword(DecodeUtil.md5(entityDO.getLoginPassword()));
        // 插入 userPassword 数据，并返回插入后的 userPassword
        if (userPasswordMapper.insert(entityDO) < 1) {
            throw new AddException("The user password add failed: {}", entityDO.toString());
        }
    }

    @Override
    @Transactional
    public void delete(String id) {
        UserPassword userPassword = selectById(id);
        if (ObjectUtil.isNull(userPassword)) {
            throw new NotFoundException("The user password does not exist");
        }

        if (userPasswordMapper.deleteById(id) < 1) {
            throw new DeleteException("The user password delete failed");
        }
    }

    @Override
    public void update(UserPassword entityDO) {
        UserPassword selectById = selectById(entityDO.getId());
        if (ObjectUtil.isNull(selectById)) {
            throw new NotFoundException();
        }
        entityDO.setLoginPassword(DecodeUtil.md5(entityDO.getLoginPassword()));
        entityDO.setOperateTime(null);
        if (userPasswordMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The user password update failed");
        }
    }

    @Override
    public UserPassword selectById(String id) {
        return userPasswordMapper.selectById(id);
    }

    @Override
    public Page<UserPassword> list(UserPasswordPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return userPasswordMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    @Override
    public void restPassword(String id) {
        UserPassword userPassword = selectById(id);
        if (ObjectUtil.isNotNull(userPassword)) {
            userPassword.setLoginPassword(DecodeUtil.md5(AlgorithmConstant.DEFAULT_PASSWORD));
            update(userPassword);
        }
    }

    private LambdaQueryWrapper<UserPassword> fuzzyQuery(UserPasswordPageQuery query) {
        return Wrappers.<UserPassword>query().lambda();
    }

}

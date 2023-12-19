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
import io.github.pnoker.center.auth.entity.builder.UserBuilder;
import io.github.pnoker.center.auth.entity.model.UserDO;
import io.github.pnoker.center.auth.entity.query.UserQuery;
import io.github.pnoker.center.auth.manager.UserManager;
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
    private UserBuilder userBuilder;

    @Resource
    private UserManager userManager;

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
        UserDO entityDO = userBuilder.buildDOByBO(entityBO);
        if (!userManager.save(entityDO)) {
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

        if (!userManager.removeById(id)) {
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
        UserDO entityDO = userBuilder.buildDOByBO(entityBO);
        if (!userManager.updateById(entityDO)) {
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

        return selectByKey(UserDO::getUserName, userName, throwException);
    }

    @Override
    public UserBO selectByPhone(String phone, boolean throwException) {
        if (CharSequenceUtil.isEmpty(phone)) {
            if (throwException) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(UserDO::getPhone, phone, throwException);
    }

    @Override
    public UserBO selectByEmail(String email, boolean throwException) {
        if (CharSequenceUtil.isEmpty(email)) {
            if (throwException) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(UserDO::getEmail, email, throwException);
    }

    @Override
    public Page<UserBO> selectByPage(UserQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<UserDO> page = userManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return userBuilder.buildBOPageByDOPage(page);
    }

    private LambdaQueryWrapper<UserDO> fuzzyQuery(UserQuery query) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.<UserDO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getNickName()), UserDO::getNickName, query.getNickName());
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getUserName()), UserDO::getUserName, query.getUserName());
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getPhone()), UserDO::getPhone, query.getPhone());
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getEmail()), UserDO::getEmail, query.getEmail());
        }
        return wrapper;
    }

    private UserBO selectByKey(SFunction<UserDO, ?> key, String value, boolean throwException) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.<UserDO>query().lambda();
        wrapper.eq(key, value);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserDO userDO = userManager.getOne(wrapper);
        if (ObjectUtil.isNull(userDO)) {
            if (throwException) {
                throw new NotFoundException();
            }
            return null;
        }
        return userBuilder.buildBOByDO(userDO);
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link UserDO}
     */
    private UserDO getDOById(Long id, boolean throwException) {
        UserDO entityDO = userManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("用户不存在");
        }
        return entityDO;
    }

}

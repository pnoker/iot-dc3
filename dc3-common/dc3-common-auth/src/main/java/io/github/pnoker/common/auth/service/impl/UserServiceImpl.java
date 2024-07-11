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

package io.github.pnoker.common.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.UserManager;
import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.entity.builder.UserBuilder;
import io.github.pnoker.common.auth.entity.model.UserDO;
import io.github.pnoker.common.auth.entity.query.UserQuery;
import io.github.pnoker.common.auth.service.UserService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
    public void save(UserBO entityBO) {
        checkDuplicate(entityBO, false, true);

        // 判断手机号是否存在, 如果有手机号不为空, 检查该手机号是否被占用
        if (CharSequenceUtil.isNotEmpty(entityBO.getPhone())) {
            UserBO selectByPhone = selectByPhone(entityBO.getPhone(), false);
            if (Objects.nonNull(selectByPhone)) {
                throw new DuplicateException("The user already exists with phone: {}", entityBO.getPhone());
            }
        }

        // 判断邮箱是否存在, 如果有邮箱不为空, 检查该邮箱是否被占用
        if (CharSequenceUtil.isNotEmpty(entityBO.getEmail())) {
            UserBO selectByEmail = selectByEmail(entityBO.getEmail(), false);
            if (Objects.nonNull(selectByEmail)) {
                throw new DuplicateException("The user already exists with email: {}", entityBO.getEmail());
            }
        }

        UserDO entityDO = userBuilder.buildDOByBO(entityBO);
        if (!userManager.save(entityDO)) {
            throw new AddException("Failed to create user: {}", entityBO.toString());
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!userManager.removeById(id)) {
            throw new DeleteException("Failed to remove user");
        }
    }

    @Override
    public void update(UserBO entityBO) {
        UserDO selectById = getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        // 判断手机号是否更新
        if (CharSequenceUtil.isNotEmpty(entityBO.getPhone())) {
            if (!entityBO.getPhone().equals(selectById.getPhone())) {
                UserBO selectByPhone = selectByPhone(entityBO.getPhone(), false);
                if (Objects.nonNull(selectByPhone)) {
                    throw new DuplicateException("The user already exists with phone {}", entityBO.getPhone());
                }
            }
        }

        // 判断邮箱是否更新
        if (CharSequenceUtil.isNotEmpty(entityBO.getEmail())) {
            if (!entityBO.getEmail().equals(selectById.getEmail())) {
                UserBO selectByEmail = selectByEmail(entityBO.getEmail(), false);
                if (Objects.nonNull(selectByEmail)) {
                    throw new DuplicateException("The user already exists with email {}", entityBO.getEmail());
                }
            }
        }

        UserDO entityDO = userBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!userManager.updateById(entityDO)) {
            throw new UpdateException("The user update failed");
        }
    }

    @Override
    public UserBO selectById(Long id) {
        UserDO entityDO = getDOById(id, true);
        return userBuilder.buildBOByDO(entityDO);
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
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<UserDO> page = userManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return userBuilder.buildBOPageByDOPage(page);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link UserQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<UserDO> fuzzyQuery(UserQuery entityQuery) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.<UserDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getNickName()), UserDO::getNickName, entityQuery.getNickName());
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getUserName()), UserDO::getUserName, entityQuery.getUserName());
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getPhone()), UserDO::getPhone, entityQuery.getPhone());
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getEmail()), UserDO::getEmail, entityQuery.getEmail());
        return wrapper;
    }

    private UserBO selectByKey(SFunction<UserDO, ?> key, String value, boolean throwException) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.<UserDO>query().lambda();
        wrapper.eq(key, value);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserDO userDO = userManager.getOne(wrapper);
        if (Objects.isNull(userDO)) {
            if (throwException) {
                throw new NotFoundException();
            }
            return null;
        }
        return userBuilder.buildBOByDO(userDO);
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link UserBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(UserBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.<UserDO>query().lambda();
        wrapper.eq(UserDO::getUserName, entityBO.getUserName());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserDO one = userManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("User has been duplicated");
        }
        return duplicate;
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
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("User does not exist");
        }
        return entityDO;
    }

}

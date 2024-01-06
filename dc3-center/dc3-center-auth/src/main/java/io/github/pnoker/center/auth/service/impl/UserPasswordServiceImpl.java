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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.dal.UserPasswordManager;
import io.github.pnoker.center.auth.entity.bo.UserPasswordBO;
import io.github.pnoker.center.auth.entity.builder.UserPasswordBuilder;
import io.github.pnoker.center.auth.entity.model.UserPasswordDO;
import io.github.pnoker.center.auth.entity.query.UserPasswordQuery;
import io.github.pnoker.center.auth.service.UserPasswordService;
import io.github.pnoker.common.constant.common.AlgorithmConstant;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.PageUtil;
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
    private UserPasswordBuilder userPasswordBuilder;

    @Resource
    private UserPasswordManager userPasswordManager;

    @Override
    @Transactional
    public void save(UserPasswordBO entityBO) {
        checkDuplicate(entityBO, false, true);

        UserPasswordDO entityDO = userPasswordBuilder.buildDOByBO(entityBO);
        entityDO.setLoginPassword(DecodeUtil.md5(entityDO.getLoginPassword()));
        if (!userPasswordManager.save(entityDO)) {
            throw new AddException("The user password add failed: {}", entityBO.toString());
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!userPasswordManager.removeById(id)) {
            throw new DeleteException("The user password delete failed");
        }
    }

    @Override
    public void update(UserPasswordBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        UserPasswordDO entityDO = userPasswordBuilder.buildDOByBO(entityBO);
        entityDO.setLoginPassword(DecodeUtil.md5(entityDO.getLoginPassword()));
        entityDO.setOperateTime(null);
        if (!userPasswordManager.updateById(entityDO)) {
            throw new UpdateException("The user password update failed");
        }
    }

    @Override
    public UserPasswordBO selectById(Long id) {
        UserPasswordDO entityDO = getDOById(id, true);
        return userPasswordBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<UserPasswordBO> selectByPage(UserPasswordQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<UserPasswordDO> entityPageDO = userPasswordManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return userPasswordBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public void restPassword(Long id) {
        UserPasswordBO userPasswordBO = selectById(id);
        if (ObjectUtil.isNotNull(userPasswordBO)) {
            userPasswordBO.setLoginPassword(DecodeUtil.md5(AlgorithmConstant.DEFAULT_PASSWORD));
            update(userPasswordBO);
        }
    }

    private LambdaQueryWrapper<UserPasswordDO> fuzzyQuery(UserPasswordQuery entityQuery) {
        return Wrappers.<UserPasswordDO>query().lambda();
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link UserPasswordBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(UserPasswordBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<UserPasswordDO> wrapper = Wrappers.<UserPasswordDO>query().lambda();
        wrapper.eq(UserPasswordDO::getLoginPassword, entityBO.getLoginPassword());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        UserPasswordDO one = userPasswordManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("用户密码重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link UserPasswordDO}
     */
    private UserPasswordDO getDOById(Long id, boolean throwException) {
        UserPasswordDO entityDO = userPasswordManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("用户密码不存在");
        }
        return entityDO;
    }
}

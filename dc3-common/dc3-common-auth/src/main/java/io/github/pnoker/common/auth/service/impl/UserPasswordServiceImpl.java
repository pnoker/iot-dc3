/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.UserPasswordManager;
import io.github.pnoker.common.auth.entity.bo.UserPasswordBO;
import io.github.pnoker.common.auth.entity.builder.UserPasswordBuilder;
import io.github.pnoker.common.auth.entity.model.UserPasswordDO;
import io.github.pnoker.common.auth.entity.query.UserPasswordQuery;
import io.github.pnoker.common.auth.service.UserPasswordService;
import io.github.pnoker.common.constant.common.AlgorithmConstant;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户密码服务接口实现类
 *
 * @author pnoker
 * @version 2025.6.0
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
    public void save(UserPasswordBO entityBO) {
        checkDuplicate(entityBO, false, true);

        UserPasswordDO entityDO = userPasswordBuilder.buildDOByBO(entityBO);
        entityDO.setLoginPassword(DecodeUtil.md5(entityDO.getLoginPassword()));
        if (!userPasswordManager.save(entityDO)) {
            throw new AddException("Failed to create user password: {}", entityBO.toString());
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!userPasswordManager.removeById(id)) {
            throw new DeleteException("Failed to remove user password");
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
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<UserPasswordDO> entityPageDO = userPasswordManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return userPasswordBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public void restPassword(Long id) {
        UserPasswordBO userPasswordBO = selectById(id);
        if (Objects.nonNull(userPasswordBO)) {
            userPasswordBO.setLoginPassword(DecodeUtil.md5(AlgorithmConstant.DEFAULT_PASSWORD));
            update(userPasswordBO);
        }
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link UserPasswordQuery}
     * @return {@link LambdaQueryWrapper}
     */
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
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("User password has been duplicated");
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
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("User password does not exist");
        }
        return entityDO;
    }
}

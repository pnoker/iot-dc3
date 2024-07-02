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
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.RoleManager;
import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.auth.entity.query.RoleQuery;
import io.github.pnoker.common.auth.service.RoleService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * Role Service Impl
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleBuilder roleBuilder;

    @Resource
    private RoleManager roleManager;

    @Override
    public void save(RoleBO entityBO) {
        checkDuplicate(entityBO, false, true);

        RoleDO entityDO = roleBuilder.buildDOByBO(entityBO);
        if (!roleManager.save(entityDO)) {
            throw new AddException("Failed to create role");
        }
    }


    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除角色之前需要检查该角色是否存在关联
        LambdaQueryChainWrapper<RoleDO> wrapper = roleManager.lambdaQuery().eq(RoleDO::getParentRoleId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove role: some sub roles exists in the role");
        }

        if (!roleManager.removeById(id)) {
            throw new DeleteException("Failed to remove role");
        }
    }


    @Override
    public void update(RoleBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        RoleDO entityDO = roleBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!roleManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update role");
        }
    }


    @Override
    public RoleBO selectById(Long id) {
        RoleDO entityDO = getDOById(id, true);
        return roleBuilder.buildBOByDO(entityDO);
    }


    @Override
    public Page<RoleBO> selectByPage(RoleQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RoleDO> entityPageDO = roleManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return roleBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link RoleQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<RoleDO> fuzzyQuery(RoleQuery entityQuery) {
        LambdaQueryWrapper<RoleDO> wrapper = Wrappers.<RoleDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getRoleName()), RoleDO::getRoleName, entityQuery.getRoleName());
        wrapper.eq(CharSequenceUtil.isNotEmpty(entityQuery.getRoleCode()), RoleDO::getRoleCode, entityQuery.getRoleCode());
        wrapper.eq(RoleDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link RoleBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(RoleBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<RoleDO> wrapper = Wrappers.<RoleDO>query().lambda();
        wrapper.eq(RoleDO::getParentRoleId, entityBO.getParentRoleId());
        wrapper.eq(RoleDO::getRoleName, entityBO.getRoleName());
        wrapper.eq(RoleDO::getRoleCode, entityBO.getRoleCode());
        wrapper.eq(RoleDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RoleDO one = roleManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Role has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link RoleDO}
     */
    private RoleDO getDOById(Long id, boolean throwException) {
        RoleDO entityDO = roleManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Role does not exist");
        }
        return entityDO;
    }

}

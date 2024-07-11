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

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.ResourceManager;
import io.github.pnoker.common.auth.dal.RoleResourceBindManager;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.entity.model.RoleResourceBindDO;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindQuery;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author linys
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RoleResourceBindServiceImpl implements RoleResourceBindService {

    @Resource
    private ResourceBuilder resourceBuilder;
    @Resource
    private RoleResourceBindBuilder roleResourceBindBuilder;

    @Resource
    private RoleResourceBindManager roleResourceBindManager;
    @Resource
    private ResourceManager resourceManager;

    @Override
    public void save(RoleResourceBindBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create role resource bind: role resource bind has been duplicated");
        }

        RoleResourceBindDO entityDO = roleResourceBindBuilder.buildDOByBO(entityBO);
        if (!roleResourceBindManager.save(entityDO)) {
            throw new AddException("Failed to create role resource bind");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!roleResourceBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove role resource bind");
        }
    }

    @Override
    public void update(RoleResourceBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update role resource bind: role resource bind has been duplicated");
        }

        RoleResourceBindDO entityDO = roleResourceBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!roleResourceBindManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update role resource bind");
        }
    }

    @Override
    public RoleResourceBindBO selectById(Long id) {
        RoleResourceBindDO entityDO = getDOById(id, true);
        return roleResourceBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<RoleResourceBindBO> selectByPage(RoleResourceBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RoleResourceBindDO> entityPageDO = roleResourceBindManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return roleResourceBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public List<ResourceBO> listResourceByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleResourceBindDO> wrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        wrapper.eq(RoleResourceBindDO::getRoleId, roleId);
        List<RoleResourceBindDO> entityDOList = roleResourceBindManager.list(wrapper);
        if (CollUtil.isNotEmpty(entityDOList)) {
            List<ResourceDO> resourceDOList = resourceManager.listByIds(entityDOList.stream()
                    .map(RoleResourceBindDO::getResourceId).toList());
            List<ResourceDO> collect = resourceDOList.stream().filter(e -> EnableFlagEnum.ENABLE.getIndex().equals(e.getEnableFlag()))
                    .toList();
            return resourceBuilder.buildBOListByDOList(collect);
        }

        return null;
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link RoleResourceBindQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<RoleResourceBindDO> fuzzyQuery(RoleResourceBindQuery entityQuery) {
        LambdaQueryWrapper<RoleResourceBindDO> wrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getRoleId()), RoleResourceBindDO::getResourceId, entityQuery.getRoleId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getResourceId()), RoleResourceBindDO::getResourceId, entityQuery.getResourceId());
        wrapper.eq(RoleResourceBindDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO {@link RoleResourceBindBO}
     * @param isUpdate 是否为更新操作
     * @return 是否重复
     */
    private boolean checkDuplicate(RoleResourceBindBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<RoleResourceBindDO> wrapper = Wrappers.<RoleResourceBindDO>query().lambda();
        wrapper.eq(RoleResourceBindDO::getRoleId, entityBO.getRoleId());
        wrapper.eq(RoleResourceBindDO::getResourceId, entityBO.getResourceId());
        wrapper.eq(RoleResourceBindDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RoleResourceBindDO one = roleResourceBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link RoleResourceBindDO}
     */
    private RoleResourceBindDO getDOById(Long id, boolean throwException) {
        RoleResourceBindDO entityDO = roleResourceBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Role resource bind does not exist");
        }
        return entityDO;
    }
}

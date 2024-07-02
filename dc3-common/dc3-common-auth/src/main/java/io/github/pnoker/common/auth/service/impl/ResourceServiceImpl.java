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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.dal.ResourceManager;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.entity.query.ResourceQuery;
import io.github.pnoker.common.auth.service.ResourceService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author linys
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {

    @Resource
    private ResourceBuilder resourceBuilder;

    @Resource
    private ResourceManager resourceManager;

    @Override
    public void save(ResourceBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create resource: resource has been duplicated");
        }

        ResourceDO entityDO = resourceBuilder.buildDOByBO(entityBO);
        if (!resourceManager.save(entityDO)) {
            throw new AddException("Failed to create resource");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!resourceManager.removeById(id)) {
            throw new DeleteException("Failed to remove resource");
        }
    }

    @Override
    public void update(ResourceBO entityBO) {
        getDOById(entityBO.getId(), true);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update resource: resource has been duplicated");
        }

        ResourceDO entityDO = resourceBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!resourceManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update resource");
        }
    }

    @Override
    public ResourceBO selectById(Long id) {
        ResourceDO entityDO = getDOById(id, true);
        return resourceBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<ResourceBO> selectByPage(ResourceQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ResourceDO> entityPageDO = resourceManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return resourceBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link ResourceQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<ResourceDO> fuzzyQuery(ResourceQuery entityQuery) {
        LambdaQueryWrapper<ResourceDO> wrapper = Wrappers.<ResourceDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getResourceName()), ResourceDO::getResourceName, entityQuery.getResourceName());
        wrapper.eq(CharSequenceUtil.isNotEmpty(entityQuery.getResourceCode()), ResourceDO::getResourceCode, entityQuery.getResourceCode());
        wrapper.eq(Objects.nonNull(entityQuery.getResourceTypeFlag()), ResourceDO::getResourceTypeFlag, entityQuery.getResourceTypeFlag());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), ResourceDO::getEnableFlag, entityQuery.getEnableFlag());
        wrapper.eq(ResourceDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO {@link ResourceBO}
     * @param isUpdate 是否为更新操作
     * @return 是否重复
     */
    private boolean checkDuplicate(ResourceBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<ResourceDO> wrapper = Wrappers.<ResourceDO>query().lambda();
        wrapper.eq(ResourceDO::getParentResourceId, entityBO.getParentResourceId());
        wrapper.eq(ResourceDO::getResourceName, entityBO.getResourceName());
        wrapper.eq(ResourceDO::getResourceCode, entityBO.getResourceCode());
        wrapper.eq(ResourceDO::getResourceTypeFlag, entityBO.getResourceTypeFlag());
        wrapper.eq(ResourceDO::getResourceScopeFlag, entityBO.getResourceScopeFlag());
        wrapper.eq(ResourceDO::getEntityId, entityBO.getEntityId());
        wrapper.eq(ResourceDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ResourceDO one = resourceManager.getOne(wrapper);
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
     * @return {@link ResourceDO}
     */
    private ResourceDO getDOById(Long id, boolean throwException) {
        ResourceDO entityDO = resourceManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Resource does not exist");
        }
        return entityDO;
    }
}

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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.dal.ResourceManager;
import io.github.pnoker.center.auth.entity.bo.ResourceBO;
import io.github.pnoker.center.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.center.auth.entity.model.ResourceDO;
import io.github.pnoker.center.auth.entity.query.ResourceQuery;
import io.github.pnoker.center.auth.service.ResourceService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
        ResourceDO entityDO = resourceBuilder.buildDOByBO(entityBO);

        if (!resourceManager.save(entityDO)) {
            throw new AddException("The resource add failed");
        }
    }

    @Override
    public void remove(Long id) {
        selectById(id);
        if (!resourceManager.removeById(id)) {
            throw new DeleteException("The resource delete failed");
        }
    }

    @Override
    public void update(ResourceBO entityBO) {
        selectById(entityBO.getId());
        ResourceDO entityDO = resourceBuilder.buildDOByBO(entityBO);
        if (!resourceManager.updateById(entityDO)) {
            throw new UpdateException("The resource update failed");
        }
    }

    @Override
    public ResourceBO selectById(Long id) {
        return null;
    }

    @Override
    public Page<ResourceBO> selectByPage(ResourceQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<ResourceDO> entityPageDO = resourceManager.page(PageUtil.page(entityQuery.getPage()), buildQueryWrapper(entityQuery));
        return resourceBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<ResourceDO> buildQueryWrapper(ResourceQuery pageQuery) {
        LambdaQueryWrapper<ResourceDO> wrapper = Wrappers.<ResourceDO>query().lambda();
        if (ObjectUtil.isNotNull(pageQuery)) {
            wrapper.eq(ObjectUtil.isNotEmpty(getTenantId()), ResourceDO::getTenantId, getTenantId());
            wrapper.like(CharSequenceUtil.isNotEmpty(pageQuery.getResourceName()), ResourceDO::getResourceName, pageQuery.getResourceName());
            wrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getResourceCode()), ResourceDO::getResourceCode, pageQuery.getResourceCode());
            wrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getResourceTypeFlag()), ResourceDO::getResourceTypeFlag, pageQuery.getResourceTypeFlag());
            wrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getEnableFlag()), ResourceDO::getEnableFlag, pageQuery.getEnableFlag());

        }
        return wrapper;
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
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("资源不存在");
        }
        return entityDO;
    }
}

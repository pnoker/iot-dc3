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
import io.github.pnoker.center.auth.entity.query.ResourcePageQuery;
import io.github.pnoker.center.auth.mapper.ResourceMapper;
import io.github.pnoker.center.auth.service.ResourceService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.model.Resource;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author linys
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {

    @javax.annotation.Resource
    private ResourceMapper resourceMapper;


    @Override
    public void save(Resource entityBO) {
        //todo check if exists
        if (resourceMapper.insert(entityBO) < 1) {
            throw new AddException("The resource add failed");
        }
    }

    @Override
    public void remove(Long id) {
        selectById(id);
        if (resourceMapper.deleteById(id) < 1) {
            throw new DeleteException("The resource delete failed");
        }
    }

    @Override
    public void update(Resource entityBO) {
        selectById(entityBO.getId());
        if (resourceMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The resource update failed");
        }
    }

    @Override
    public Resource selectById(Long id) {
        return null;
    }

    @Override
    public Page<Resource> selectByPage(ResourcePageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return resourceMapper.selectPage(PageUtil.page(entityQuery.getPage()), buildQueryWrapper(entityQuery));
    }

    private LambdaQueryWrapper<Resource> buildQueryWrapper(ResourcePageQuery pageQuery) {
        LambdaQueryWrapper<Resource> queryWrapper = Wrappers.<Resource>query().lambda();
        if (ObjectUtil.isNotNull(pageQuery)) {
            queryWrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getTenantId()), Resource::getTenantId, pageQuery.getTenantId());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getParentResourceId()), Resource::getParentResourceId, pageQuery.getParentResourceId());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(pageQuery.getResourceName()), Resource::getResourceName, pageQuery.getResourceName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pageQuery.getResourceCode()), Resource::getResourceCode, pageQuery.getResourceCode());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getResourceTypeFlag()), Resource::getResourceTypeFlag, pageQuery.getResourceTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(pageQuery.getEnableFlag()), Resource::getEnableFlag, pageQuery.getEnableFlag());

        }
        return queryWrapper;
    }
}

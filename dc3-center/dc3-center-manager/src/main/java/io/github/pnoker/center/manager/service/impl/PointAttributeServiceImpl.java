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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.center.manager.entity.query.PointAttributeBOPageQuery;
import io.github.pnoker.center.manager.mapper.PointAttributeMapper;
import io.github.pnoker.center.manager.service.PointAttributeService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * PointAttributeService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointAttributeServiceImpl implements PointAttributeService {

    @Resource
    private PointAttributeMapper pointAttributeMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(PointAttributeBO entityBO) {
        try {
            selectByNameAndDriverId(entityBO.getAttributeName(), entityBO.getDriverId());
            throw new DuplicateException("The point attribute already exists");
        } catch (NotFoundException notFoundException) {
            if (pointAttributeMapper.insert(entityBO) < 1) {
                throw new AddException("The point attribute {} add failed", entityBO.getAttributeName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        PointAttributeBO pointAttributeBO = selectById(id);
        if (ObjectUtil.isNull(pointAttributeBO)) {
            throw new NotFoundException("The point attribute does not exist");
        }

        if (pointAttributeMapper.deleteById(id) < 1) {
            throw new DeleteException("The point attribute delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(PointAttributeBO entityBO) {
        selectById(entityBO.getId());
        entityBO.setOperateTime(null);
        if (pointAttributeMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The point attribute update failed");
        }
    }

    @Override
    public PointAttributeBO selectById(Long id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttributeBO selectByNameAndDriverId(String name, Long driverId) {
        LambdaQueryWrapper<PointAttributeBO> queryWrapper = Wrappers.<PointAttributeBO>query().lambda();
        queryWrapper.eq(PointAttributeBO::getAttributeName, name);
        queryWrapper.eq(PointAttributeBO::getDriverId, driverId);
        queryWrapper.last("limit 1");
        PointAttributeBO pointAttributeBO = pointAttributeMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(pointAttributeBO)) {
            throw new NotFoundException();
        }
        return pointAttributeBO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointAttributeBO> selectByDriverId(Long driverId, boolean throwException) {
        PointAttributeBOPageQuery pointAttributePageQuery = new PointAttributeBOPageQuery();
        pointAttributePageQuery.setDriverId(driverId);
        List<PointAttributeBO> pointAttributeBOS = pointAttributeMapper.selectList(fuzzyQuery(pointAttributePageQuery));
        if (throwException) {
            if (ObjectUtil.isNull(pointAttributeBOS) || pointAttributeBOS.isEmpty()) {
                throw new NotFoundException();
            }
        }
        return pointAttributeBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PointAttributeBO> selectByPage(PointAttributeBOPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return pointAttributeMapper.selectPage(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<PointAttributeBO> fuzzyQuery(PointAttributeBOPageQuery query) {
        LambdaQueryWrapper<PointAttributeBO> queryWrapper = Wrappers.<PointAttributeBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getAttributeName()), PointAttributeBO::getAttributeName, query.getAttributeName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getDisplayName()), PointAttributeBO::getDisplayName, query.getDisplayName());
            queryWrapper.eq(ObjectUtil.isNotNull(query.getAttributeTypeFlag()), PointAttributeBO::getAttributeTypeFlag, query.getAttributeTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getDriverId()), PointAttributeBO::getDriverId, query.getDriverId());
        }
        return queryWrapper;
    }

}

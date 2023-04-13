/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.center.manager.entity.query.PointAttributePageQuery;
import io.github.pnoker.center.manager.mapper.PointAttributeMapper;
import io.github.pnoker.center.manager.service.PointAttributeService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.PointAttribute;
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
    public void add(PointAttribute entityDO) {
        try {
            selectByNameAndDriverId(entityDO.getAttributeName(), entityDO.getDriverId());
            throw new DuplicateException("The point attribute already exists");
        } catch (NotFoundException notFoundException) {
            if (pointAttributeMapper.insert(entityDO) < 1) {
                throw new AddException("The point attribute {} add failed", entityDO.getAttributeName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        PointAttribute pointAttribute = selectById(id);
        if (ObjectUtil.isNull(pointAttribute)) {
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
    public void update(PointAttribute entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (pointAttributeMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The point attribute update failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttribute selectById(String id) {
        PointAttribute pointAttribute = pointAttributeMapper.selectById(id);
        if (ObjectUtil.isNull(pointAttribute)) {
            throw new NotFoundException();
        }
        return pointAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttribute selectByNameAndDriverId(String name, String driverId) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        queryWrapper.eq(PointAttribute::getAttributeName, name);
        queryWrapper.eq(PointAttribute::getDriverId, driverId);
        queryWrapper.last("limit 1");
        PointAttribute pointAttribute = pointAttributeMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(pointAttribute)) {
            throw new NotFoundException();
        }
        return pointAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PointAttribute> selectByDriverId(String driverId, boolean throwException) {
        PointAttributePageQuery pointAttributePageQuery = new PointAttributePageQuery();
        pointAttributePageQuery.setDriverId(driverId);
        List<PointAttribute> pointAttributes = pointAttributeMapper.selectList(fuzzyQuery(pointAttributePageQuery));
        if (throwException) {
            if (ObjectUtil.isNull(pointAttributes) || pointAttributes.isEmpty()) {
                throw new NotFoundException();
            }
        }
        return pointAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<PointAttribute> list(PointAttributePageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return pointAttributeMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<PointAttribute> fuzzyQuery(PointAttributePageQuery query) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getAttributeName()), PointAttribute::getAttributeName, query.getAttributeName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getDisplayName()), PointAttribute::getDisplayName, query.getDisplayName());
            queryWrapper.eq(ObjectUtil.isNotNull(query.getAttributeTypeFlag()), PointAttribute::getAttributeTypeFlag, query.getAttributeTypeFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDriverId()), PointAttribute::getDriverId, query.getDriverId());
        }
        return queryWrapper;
    }

}

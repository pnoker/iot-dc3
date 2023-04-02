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
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
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
    public PointAttribute add(PointAttribute pointAttribute) {
        try {
            selectByNameAndDriverId(pointAttribute.getAttributeName(), pointAttribute.getDriverId());
            throw new DuplicateException("The point attribute already exists");
        } catch (NotFoundException notFoundException) {
            if (pointAttributeMapper.insert(pointAttribute) > 0) {
                return pointAttributeMapper.selectById(pointAttribute.getId());
            }
            throw new ServiceException("The point attribute add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        selectById(id);
        return pointAttributeMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttribute update(PointAttribute pointAttribute) {
        selectById(pointAttribute.getId());
        pointAttribute.setOperateTime(null);
        if (pointAttributeMapper.updateById(pointAttribute) > 0) {
            PointAttribute select = pointAttributeMapper.selectById(pointAttribute.getId());
            pointAttribute.setAttributeName(select.getAttributeName());
            pointAttribute.setDriverId(select.getDriverId());
            return select;
        }
        throw new ServiceException("The point attribute update failed");
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
    public Page<PointAttribute> list(PointAttributePageQuery pointAttributePageQuery) {
        if (ObjectUtil.isNull(pointAttributePageQuery.getPage())) {
            pointAttributePageQuery.setPage(new Pages());
        }
        return pointAttributeMapper.selectPage(pointAttributePageQuery.getPage().convert(), fuzzyQuery(pointAttributePageQuery));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<PointAttribute> fuzzyQuery(PointAttributePageQuery pointAttributePageQuery) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        if (ObjectUtil.isNotNull(pointAttributePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(pointAttributePageQuery.getAttributeName()), PointAttribute::getAttributeName, pointAttributePageQuery.getAttributeName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(pointAttributePageQuery.getDisplayName()), PointAttribute::getDisplayName, pointAttributePageQuery.getDisplayName());
            queryWrapper.eq(ObjectUtil.isNotNull(pointAttributePageQuery.getAttributeTypeFlag()), PointAttribute::getAttributeTypeFlag, pointAttributePageQuery.getAttributeTypeFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(pointAttributePageQuery.getDriverId()), PointAttribute::getDriverId, pointAttributePageQuery.getDriverId());
        }
        return queryWrapper;
    }

}

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
import io.github.pnoker.center.manager.entity.query.DriverAttributePageQuery;
import io.github.pnoker.center.manager.mapper.DriverAttributeMapper;
import io.github.pnoker.center.manager.service.DriverAttributeService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.DriverAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * DriverAttributeService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverAttributeServiceImpl implements DriverAttributeService {

    @Resource
    private DriverAttributeMapper driverAttributeMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttribute add(DriverAttribute driverAttribute) {
        try {
            selectByNameAndDriverId(driverAttribute.getAttributeName(), driverAttribute.getDriverId());
            throw new DuplicateException("The driver attribute already exists");
        } catch (NotFoundException notFoundException) {
            if (driverAttributeMapper.insert(driverAttribute) > 0) {
                return driverAttributeMapper.selectById(driverAttribute.getId());
            }
            throw new ServiceException("The driver attribute add failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        selectById(id);
        return driverAttributeMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttribute update(DriverAttribute driverAttribute) {
        selectById(driverAttribute.getId());
        driverAttribute.setUpdateTime(null);
        if (driverAttributeMapper.updateById(driverAttribute) > 0) {
            DriverAttribute select = driverAttributeMapper.selectById(driverAttribute.getId());
            driverAttribute.setAttributeName(select.getAttributeName());
            driverAttribute.setDriverId(select.getDriverId());
            return select;
        }
        throw new ServiceException("The driver attribute update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttribute selectById(String id) {
        DriverAttribute driverAttribute = driverAttributeMapper.selectById(id);
        if (ObjectUtil.isNull(driverAttribute)) {
            throw new NotFoundException();
        }
        return driverAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttribute selectByNameAndDriverId(String name, String driverId) {
        LambdaQueryWrapper<DriverAttribute> queryWrapper = Wrappers.<DriverAttribute>query().lambda();
        queryWrapper.eq(DriverAttribute::getAttributeName, name);
        queryWrapper.eq(DriverAttribute::getDriverId, driverId);
        queryWrapper.last("limit 1");
        DriverAttribute driverAttribute = driverAttributeMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(driverAttribute)) {
            throw new NotFoundException();
        }
        return driverAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttribute> selectByDriverId(String driverId) {
        DriverAttributePageQuery driverAttributePageQuery = new DriverAttributePageQuery();
        driverAttributePageQuery.setDriverId(driverId);
        List<DriverAttribute> driverAttributes = driverAttributeMapper.selectList(fuzzyQuery(driverAttributePageQuery));
        if (ObjectUtil.isNull(driverAttributes) || driverAttributes.isEmpty()) {
            throw new NotFoundException();
        }
        return driverAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverAttribute> list(DriverAttributePageQuery driverAttributePageQuery) {
        if (ObjectUtil.isNull(driverAttributePageQuery.getPage())) {
            driverAttributePageQuery.setPage(new Pages());
        }
        return driverAttributeMapper.selectPage(driverAttributePageQuery.getPage().convert(), fuzzyQuery(driverAttributePageQuery));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<DriverAttribute> fuzzyQuery(DriverAttributePageQuery driverAttributePageQuery) {
        LambdaQueryWrapper<DriverAttribute> queryWrapper = Wrappers.<DriverAttribute>query().lambda();
        if (ObjectUtil.isNotNull(driverAttributePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(driverAttributePageQuery.getAttributeName()), DriverAttribute::getAttributeName, driverAttributePageQuery.getAttributeName());
            queryWrapper.like(CharSequenceUtil.isNotBlank(driverAttributePageQuery.getDisplayName()), DriverAttribute::getDisplayName, driverAttributePageQuery.getDisplayName());
            queryWrapper.eq(ObjectUtil.isNotNull(driverAttributePageQuery.getAttributeTypeFlag()), DriverAttribute::getAttributeTypeFlag, driverAttributePageQuery.getAttributeTypeFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(driverAttributePageQuery.getDriverId()), DriverAttribute::getDriverId, driverAttributePageQuery.getDriverId());
        }
        return queryWrapper;
    }

}

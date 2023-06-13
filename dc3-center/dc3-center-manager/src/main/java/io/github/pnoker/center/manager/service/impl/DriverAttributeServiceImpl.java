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
import io.github.pnoker.common.exception.*;
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
    public void add(DriverAttribute entityDO) {
        try {
            selectByNameAndDriverId(entityDO.getAttributeName(), entityDO.getDriverId());
            throw new DuplicateException("The driver attribute already exists");
        } catch (NotFoundException notFoundException) {
            if (driverAttributeMapper.insert(entityDO) < 1) {
                throw new AddException("The driver attribute {} add failed", entityDO.getDisplayName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id) {
        DriverAttribute driverAttribute = selectById(id);
        if (ObjectUtil.isNull(driverAttribute)) {
            throw new NotFoundException("The driver attribute does not exist");
        }

        if (driverAttributeMapper.deleteById(id) < 1) {
            throw new DeleteException("The driver attribute delete failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(DriverAttribute entityDO) {
        selectById(entityDO.getId());
        entityDO.setOperateTime(null);
        if (driverAttributeMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The driver attribute update failed");
        }
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
    public List<DriverAttribute> selectByDriverId(String driverId, boolean throwException) {
        DriverAttributePageQuery driverAttributePageQuery = new DriverAttributePageQuery();
        driverAttributePageQuery.setDriverId(driverId);
        List<DriverAttribute> driverAttributes = driverAttributeMapper.selectList(fuzzyQuery(driverAttributePageQuery));
        if (throwException) {
            if (ObjectUtil.isNull(driverAttributes) || driverAttributes.isEmpty()) {
                throw new NotFoundException();
            }
        }
        return driverAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverAttribute> list(DriverAttributePageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return driverAttributeMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    private LambdaQueryWrapper<DriverAttribute> fuzzyQuery(DriverAttributePageQuery query) {
        LambdaQueryWrapper<DriverAttribute> queryWrapper = Wrappers.<DriverAttribute>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getAttributeName()), DriverAttribute::getAttributeName, query.getAttributeName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getDisplayName()), DriverAttribute::getDisplayName, query.getDisplayName());
            queryWrapper.eq(ObjectUtil.isNotNull(query.getAttributeTypeFlag()), DriverAttribute::getAttributeTypeFlag, query.getAttributeTypeFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(query.getDriverId()), DriverAttribute::getDriverId, query.getDriverId());
        }
        return queryWrapper;
    }

}

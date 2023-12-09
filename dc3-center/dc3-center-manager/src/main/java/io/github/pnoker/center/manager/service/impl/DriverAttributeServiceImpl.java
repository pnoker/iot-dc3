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
import io.github.pnoker.center.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.center.manager.entity.query.DriverAttributeBOPageQuery;
import io.github.pnoker.center.manager.mapper.DriverAttributeMapper;
import io.github.pnoker.center.manager.service.DriverAttributeService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
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
    public void save(DriverAttributeBO entityBO) {
        try {
            selectByNameAndDriverId(entityBO.getAttributeName(), entityBO.getDriverId());
            throw new DuplicateException("The driver attribute already exists");
        } catch (NotFoundException notFoundException) {
            if (driverAttributeMapper.insert(entityBO) < 1) {
                throw new AddException("The driver attribute {} add failed", entityBO.getDisplayName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        DriverAttributeBO driverAttributeBO = selectById(id);
        if (ObjectUtil.isNull(driverAttributeBO)) {
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
    public void update(DriverAttributeBO entityBO) {
        selectById(entityBO.getId());
        entityBO.setOperateTime(null);
        if (driverAttributeMapper.updateById(entityBO) < 1) {
            throw new UpdateException("The driver attribute update failed");
        }
    }

    @Override
    public DriverAttributeBO selectById(Long id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttributeBO selectByNameAndDriverId(String name, Long driverId) {
        LambdaQueryWrapper<DriverAttributeBO> queryWrapper = Wrappers.<DriverAttributeBO>query().lambda();
        queryWrapper.eq(DriverAttributeBO::getAttributeName, name);
        queryWrapper.eq(DriverAttributeBO::getDriverId, driverId);
        queryWrapper.last("limit 1");
        DriverAttributeBO driverAttributeBO = driverAttributeMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(driverAttributeBO)) {
            throw new NotFoundException();
        }
        return driverAttributeBO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttributeBO> selectByDriverId(Long driverId, boolean throwException) {
        DriverAttributeBOPageQuery driverAttributePageQuery = new DriverAttributeBOPageQuery();
        driverAttributePageQuery.setDriverId(driverId);
        List<DriverAttributeBO> driverAttributeBOS = driverAttributeMapper.selectList(fuzzyQuery(driverAttributePageQuery));
        if (throwException) {
            if (ObjectUtil.isNull(driverAttributeBOS) || driverAttributeBOS.isEmpty()) {
                throw new NotFoundException();
            }
        }
        return driverAttributeBOS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverAttributeBO> selectByPage(DriverAttributeBOPageQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return driverAttributeMapper.selectPage(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    private LambdaQueryWrapper<DriverAttributeBO> fuzzyQuery(DriverAttributeBOPageQuery query) {
        LambdaQueryWrapper<DriverAttributeBO> queryWrapper = Wrappers.<DriverAttributeBO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getAttributeName()), DriverAttributeBO::getAttributeName, query.getAttributeName());
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getDisplayName()), DriverAttributeBO::getDisplayName, query.getDisplayName());
            queryWrapper.eq(ObjectUtil.isNotNull(query.getAttributeTypeFlag()), DriverAttributeBO::getAttributeTypeFlag, query.getAttributeTypeFlag());
            queryWrapper.eq(ObjectUtil.isNotEmpty(query.getDriverId()), DriverAttributeBO::getDriverId, query.getDriverId());
        }
        return queryWrapper;
    }

}

/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import io.github.pnoker.api.center.manager.dto.DriverAttributeDto;
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
        if (null == driverAttribute) {
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
        DriverAttribute driverAttribute = driverAttributeMapper.selectOne(queryWrapper);
        if (null == driverAttribute) {
            throw new NotFoundException();
        }
        return driverAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttribute> selectByDriverId(String driverId) {
        DriverAttributeDto driverAttributeDto = new DriverAttributeDto();
        driverAttributeDto.setDriverId(driverId);
        List<DriverAttribute> driverAttributes = driverAttributeMapper.selectList(fuzzyQuery(driverAttributeDto));
        if (null == driverAttributes || driverAttributes.isEmpty()) {
            throw new NotFoundException();
        }
        return driverAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverAttribute> list(DriverAttributeDto driverAttributeDto) {
        if (null == driverAttributeDto.getPage()) {
            driverAttributeDto.setPage(new Pages());
        }
        return driverAttributeMapper.selectPage(driverAttributeDto.getPage().convert(), fuzzyQuery(driverAttributeDto));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<DriverAttribute> fuzzyQuery(DriverAttributeDto driverAttributeDto) {
        LambdaQueryWrapper<DriverAttribute> queryWrapper = Wrappers.<DriverAttribute>query().lambda();
        if (ObjectUtil.isNotNull(driverAttributeDto)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(driverAttributeDto.getAttributeName()), DriverAttribute::getAttributeName, driverAttributeDto.getAttributeName());
            queryWrapper.like(CharSequenceUtil.isNotBlank(driverAttributeDto.getDisplayName()), DriverAttribute::getDisplayName, driverAttributeDto.getDisplayName());
            queryWrapper.eq(ObjectUtil.isNotNull(driverAttributeDto.getAttributeTypeFlag()), DriverAttribute::getAttributeTypeFlag, driverAttributeDto.getAttributeTypeFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(driverAttributeDto.getDriverId()), DriverAttribute::getDriverId, driverAttributeDto.getDriverId());
        }
        return queryWrapper;
    }

}

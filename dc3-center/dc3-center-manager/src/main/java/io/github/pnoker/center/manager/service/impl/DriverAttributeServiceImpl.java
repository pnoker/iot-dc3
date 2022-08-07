/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.mapper.DriverAttributeMapper;
import io.github.pnoker.center.manager.service.DriverAttributeService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.dto.DriverAttributeDto;
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
 */
@Slf4j
@Service
public class DriverAttributeServiceImpl implements DriverAttributeService {

    @Resource
    private DriverAttributeMapper driverAttributeMapper;

    @Override
    public DriverAttribute add(DriverAttribute driverAttribute) {
        try {
            selectByNameAndDriverId(driverAttribute.getName(), driverAttribute.getDriverId());
            throw new DuplicateException("The driver attribute already exists");
        } catch (NotFoundException notFoundException) {
            if (driverAttributeMapper.insert(driverAttribute) > 0) {
                return driverAttributeMapper.selectById(driverAttribute.getId());
            }
            throw new ServiceException("The driver attribute add failed");
        }
    }

    @Override
    public boolean delete(String id) {
        selectById(id);
        return driverAttributeMapper.deleteById(id) > 0;
    }

    @Override
    public DriverAttribute update(DriverAttribute driverAttribute) {
        selectById(driverAttribute.getId());
        driverAttribute.setUpdateTime(null);
        if (driverAttributeMapper.updateById(driverAttribute) > 0) {
            DriverAttribute select = driverAttributeMapper.selectById(driverAttribute.getId());
            driverAttribute.setName(select.getName()).setDriverId(select.getDriverId());
            return select;
        }
        throw new ServiceException("The driver attribute update failed");
    }

    @Override
    public DriverAttribute selectById(String id) {
        DriverAttribute driverAttribute = driverAttributeMapper.selectById(id);
        if (null == driverAttribute) {
            throw new NotFoundException("The driver attribute does not exist");
        }
        return driverAttribute;
    }

    @Override
    public DriverAttribute selectByNameAndDriverId(String name, String driverId) {
        LambdaQueryWrapper<DriverAttribute> queryWrapper = Wrappers.<DriverAttribute>query().lambda();
        queryWrapper.eq(DriverAttribute::getName, name);
        queryWrapper.eq(DriverAttribute::getDriverId, driverId);
        DriverAttribute driverAttribute = driverAttributeMapper.selectOne(queryWrapper);
        if (null == driverAttribute) {
            throw new NotFoundException("The driver attribute does not exist");
        }
        return driverAttribute;
    }

    @Override
    public List<DriverAttribute> selectByDriverId(String driverId) {
        DriverAttributeDto driverAttributeDto = new DriverAttributeDto();
        driverAttributeDto.setDriverId(driverId);
        List<DriverAttribute> driverAttributes = driverAttributeMapper.selectList(fuzzyQuery(driverAttributeDto));
        if (null == driverAttributes || driverAttributes.size() < 1) {
            throw new NotFoundException("The driver attributes does not exist");
        }
        return driverAttributes;
    }

    @Override
    public Page<DriverAttribute> list(DriverAttributeDto driverAttributeDto) {
        if (null == driverAttributeDto.getPage()) {
            driverAttributeDto.setPage(new Pages());
        }
        return driverAttributeMapper.selectPage(driverAttributeDto.getPage().convert(), fuzzyQuery(driverAttributeDto));
    }

    @Override
    public LambdaQueryWrapper<DriverAttribute> fuzzyQuery(DriverAttributeDto driverAttributeDto) {
        LambdaQueryWrapper<DriverAttribute> queryWrapper = Wrappers.<DriverAttribute>query().lambda();
        if (ObjectUtil.isNotNull(driverAttributeDto)) {
            queryWrapper.like(StrUtil.isNotEmpty(driverAttributeDto.getName()), DriverAttribute::getName, driverAttributeDto.getName());
            queryWrapper.like(StrUtil.isNotEmpty(driverAttributeDto.getDisplayName()), DriverAttribute::getDisplayName, driverAttributeDto.getDisplayName());
            queryWrapper.eq(StrUtil.isNotEmpty(driverAttributeDto.getType()), DriverAttribute::getType, driverAttributeDto.getType());
            queryWrapper.eq(StrUtil.isNotEmpty(driverAttributeDto.getDriverId()), DriverAttribute::getDriverId, driverAttributeDto.getDriverId());
        }
        return queryWrapper;
    }

}

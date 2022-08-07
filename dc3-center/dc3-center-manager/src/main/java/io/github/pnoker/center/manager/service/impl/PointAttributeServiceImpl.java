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
import io.github.pnoker.center.manager.mapper.PointAttributeMapper;
import io.github.pnoker.center.manager.service.PointAttributeService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.dto.PointAttributeDto;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.PointAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * PointAttributeService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class PointAttributeServiceImpl implements PointAttributeService {

    @Resource
    private PointAttributeMapper pointAttributeMapper;

    @Override
    public PointAttribute add(PointAttribute pointAttribute) {
        try {
            selectByNameAndDriverId(pointAttribute.getName(), pointAttribute.getDriverId());
            throw new DuplicateException("The point attribute already exists");
        } catch (NotFoundException notFoundException) {
            if (pointAttributeMapper.insert(pointAttribute) > 0) {
                return pointAttributeMapper.selectById(pointAttribute.getId());
            }
            throw new ServiceException("The point attribute add failed");
        }
    }

    @Override
    public boolean delete(String id) {
        selectById(id);
        return pointAttributeMapper.deleteById(id) > 0;
    }

    @Override
    public PointAttribute update(PointAttribute pointAttribute) {
        selectById(pointAttribute.getId());
        pointAttribute.setUpdateTime(null);
        if (pointAttributeMapper.updateById(pointAttribute) > 0) {
            PointAttribute select = pointAttributeMapper.selectById(pointAttribute.getId());
            pointAttribute.setName(select.getName()).setDriverId(select.getDriverId());
            return select;
        }
        throw new ServiceException("The point attribute update failed");
    }

    @Override
    public PointAttribute selectById(String id) {
        PointAttribute pointAttribute = pointAttributeMapper.selectById(id);
        if (null == pointAttribute) {
            throw new NotFoundException("The point attribute does not exist");
        }
        return pointAttribute;
    }

    @Override
    public PointAttribute selectByNameAndDriverId(String name, String driverId) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        queryWrapper.eq(PointAttribute::getName, name);
        queryWrapper.eq(PointAttribute::getDriverId, driverId);
        PointAttribute pointAttribute = pointAttributeMapper.selectOne(queryWrapper);
        if (null == pointAttribute) {
            throw new NotFoundException("The point attribute does not exist");
        }
        return pointAttribute;
    }

    @Override
    public List<PointAttribute> selectByDriverId(String driverId) {
        PointAttributeDto pointAttributeDto = new PointAttributeDto();
        pointAttributeDto.setDriverId(driverId);
        List<PointAttribute> pointAttributes = pointAttributeMapper.selectList(fuzzyQuery(pointAttributeDto));
        if (null == pointAttributes || pointAttributes.size() < 1) {
            throw new NotFoundException("The point attributes does not exist");
        }
        return pointAttributes;
    }

    @Override
    public Page<PointAttribute> list(PointAttributeDto pointAttributeDto) {
        if (ObjectUtil.isNull(pointAttributeDto.getPage())) {
            pointAttributeDto.setPage(new Pages());
        }
        return pointAttributeMapper.selectPage(pointAttributeDto.getPage().convert(), fuzzyQuery(pointAttributeDto));
    }

    @Override
    public LambdaQueryWrapper<PointAttribute> fuzzyQuery(PointAttributeDto pointAttributeDto) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        if (ObjectUtil.isNotNull(pointAttributeDto)) {
            queryWrapper.like(StrUtil.isNotEmpty(pointAttributeDto.getName()), PointAttribute::getName, pointAttributeDto.getName());
            queryWrapper.like(StrUtil.isNotEmpty(pointAttributeDto.getDisplayName()), PointAttribute::getDisplayName, pointAttributeDto.getDisplayName());
            queryWrapper.eq(StrUtil.isNotEmpty(pointAttributeDto.getType()), PointAttribute::getType, pointAttributeDto.getType());
            queryWrapper.eq(StrUtil.isNotEmpty(pointAttributeDto.getDriverId()), PointAttribute::getDriverId, pointAttributeDto.getDriverId());
        }
        return queryWrapper;
    }

}

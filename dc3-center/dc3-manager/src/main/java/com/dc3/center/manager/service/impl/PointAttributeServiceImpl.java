/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.PointAttributeMapper;
import com.dc3.center.manager.service.PointAttributeService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.PointAttributeDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.PointAttribute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>PointAttributeService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class PointAttributeServiceImpl implements PointAttributeService {
    @Resource
    private PointAttributeMapper pointAttributeMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.ID, key = "#pointAttribute.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.NAME, key = "#pointAttribute.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public PointAttribute add(PointAttribute pointAttribute) {
        PointAttribute select = selectByNameAndDriverId(pointAttribute.getName(), pointAttribute.getDriverId());
        Optional.ofNullable(select).ifPresent(s -> {
            throw new ServiceException("The point attribute already exists");
        });
        if (pointAttributeMapper.insert(pointAttribute) > 0) {
            return pointAttributeMapper.selectById(pointAttribute.getId());
        }
        throw new ServiceException("The point attribute add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        PointAttribute pointAttribute = selectById(id);
        if (null == pointAttribute) {
            throw new ServiceException("The point attribute does not exist");
        }
        return pointAttributeMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.ID, key = "#pointAttribute.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.NAME, key = "#pointAttribute.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public PointAttribute update(PointAttribute pointAttribute) {
        PointAttribute temp = selectById(pointAttribute.getId());
        if (null == temp) {
            throw new ServiceException("The point attribute does not exist");
        }
        pointAttribute.setUpdateTime(null);
        if (pointAttributeMapper.updateById(pointAttribute) > 0) {
            PointAttribute select = pointAttributeMapper.selectById(pointAttribute.getId());
            pointAttribute.setName(select.getName());
            return select;
        }
        throw new ServiceException("The point attribute update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.ID, key = "#id", unless = "#result==null")
    public PointAttribute selectById(Long id) {
        return pointAttributeMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.NAME, key = "#name", unless = "#result==null")
    public PointAttribute selectByNameAndDriverId(String name, Long driverId) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        queryWrapper.eq(PointAttribute::getName, name);
        queryWrapper.eq(PointAttribute::getDriverId, driverId);
        return pointAttributeMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ATTRIBUTE + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<PointAttribute> list(PointAttributeDto pointAttributeDto) {
        if (!Optional.ofNullable(pointAttributeDto.getPage()).isPresent()) {
            pointAttributeDto.setPage(new Pages());
        }
        return pointAttributeMapper.selectPage(pointAttributeDto.getPage().convert(), fuzzyQuery(pointAttributeDto));
    }

    @Override
    public LambdaQueryWrapper<PointAttribute> fuzzyQuery(PointAttributeDto pointAttributeDto) {
        LambdaQueryWrapper<PointAttribute> queryWrapper = Wrappers.<PointAttribute>query().lambda();
        Optional.ofNullable(pointAttributeDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getDisplayName())) {
                queryWrapper.like(PointAttribute::getDisplayName, dto.getDisplayName());
            }
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.eq(PointAttribute::getName, dto.getName());
            }
            if (StringUtils.isNotBlank(dto.getType())) {
                queryWrapper.eq(PointAttribute::getType, dto.getType());
            }
            Optional.ofNullable(dto.getDriverId()).ifPresent(driverId -> {
                queryWrapper.eq(PointAttribute::getDriverId, driverId);
            });
        });
        return queryWrapper;
    }

}

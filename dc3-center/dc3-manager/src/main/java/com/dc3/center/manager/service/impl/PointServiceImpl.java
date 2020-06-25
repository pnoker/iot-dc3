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
import com.dc3.center.manager.mapper.PointMapper;
import com.dc3.center.manager.service.PointService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.PointDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Point;
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
 * <p>PointService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class PointServiceImpl implements PointService {

    @Resource
    private PointMapper pointMapper;


    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT + Common.Cache.ID, key = "#point.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT + Common.Cache.NAME, key = "#point.name+'.'+#point.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Point add(Point point) {
        Point select = selectByNameAndProfile(point.getName(), point.getProfileId());
        if (null != select) {
            throw new ServiceException("The point already exists in the profile");
        }
        if (pointMapper.insert(point) > 0) {
            return pointMapper.selectById(point.getId());
        }
        throw new ServiceException("The point add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        Point point = selectById(id);
        if (null == point) {
            throw new ServiceException("The point does not exist");
        }
        return pointMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT + Common.Cache.ID, key = "#point.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT + Common.Cache.NAME, key = "#point.profileId+'.'+#point.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Point update(Point point) {
        Point temp = selectById(point.getId());
        if (null == temp) {
            throw new ServiceException("The point does not exist");
        }
        point.setUpdateTime(null);
        Point selectById = pointMapper.selectById(point.getId());
        if (!selectById.getProfileId().equals(point.getProfileId()) || !selectById.getName().equals(point.getName())) {
            Point select = selectByNameAndProfile(point.getName(), point.getProfileId());
            if (null != select) {
                throw new ServiceException("The point already exists");
            }
        }
        if (pointMapper.updateById(point) > 0) {
            Point select = pointMapper.selectById(point.getId());
            point.setName(select.getName());
            return select;
        }
        throw new ServiceException("The point update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.POINT + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Point selectById(Long id) {
        return pointMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT + Common.Cache.NAME, key = "#profileId+'.'+#name", unless = "#result==null")
    public Point selectByNameAndProfile(String name, Long profileId) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        queryWrapper.eq(Point::getName, name);
        queryWrapper.eq(Point::getProfileId, profileId);
        return pointMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Point> list(PointDto pointDto) {
        if (!Optional.ofNullable(pointDto.getPage()).isPresent()) {
            pointDto.setPage(new Pages());
        }
        return pointMapper.selectPage(pointDto.getPage().convert(), fuzzyQuery(pointDto));
    }

    @Override
    public LambdaQueryWrapper<Point> fuzzyQuery(PointDto pointDto) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        Optional.ofNullable(pointDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Point::getName, dto.getName());
            }
            if (StringUtils.isNotBlank(dto.getType())) {
                queryWrapper.eq(Point::getType, dto.getType());
            }
            if (null != dto.getRw()) {
                queryWrapper.eq(Point::getRw, dto.getRw());
            }
            if (null != dto.getAccrue()) {
                queryWrapper.eq(Point::getAccrue, dto.getAccrue());
            }
            if (null != dto.getProfileId()) {
                queryWrapper.eq(Point::getProfileId, dto.getProfileId());
            }
        });
        return queryWrapper;
    }

}

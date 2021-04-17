/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.PointMapper;
import com.dc3.center.manager.service.DeviceService;
import com.dc3.center.manager.service.PointService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.PointDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
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
    @Resource
    private DeviceService deviceService;


    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT + Common.Cache.ID, key = "#point.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT + Common.Cache.NAME + Common.Cache.PROFILE_ID, key = "#point.name+'.'+#point.profileId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.PROFILE_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Point add(Point point) {
        try {
            selectByNameAndProfileId(point.getName(), point.getProfileId());
            throw new ServiceException("The point already exists in the profile");
        } catch (NotFoundException notFoundException) {
            if (pointMapper.insert(point) > 0) {
                return pointMapper.selectById(point.getId());
            }
            throw new ServiceException("The point add failed");
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.NAME + Common.Cache.PROFILE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.PROFILE_ID + Common.Cache.LIST, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        selectById(id);
        return pointMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT + Common.Cache.ID, key = "#point.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT + Common.Cache.NAME + Common.Cache.PROFILE_ID, key = "#point.name+'.'+#point.profileId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.PROFILE_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.POINT + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Point update(Point point) {
        Point old = selectById(point.getId());
        point.setUpdateTime(null);
        if (!old.getProfileId().equals(point.getProfileId()) || !old.getName().equals(point.getName())) {
            try {
                selectByNameAndProfileId(point.getName(), point.getProfileId());
                throw new DuplicateException("The point already exists");
            } catch (NotFoundException ignored) {
            }
        }
        if (pointMapper.updateById(point) > 0) {
            Point select = pointMapper.selectById(point.getId());
            point.setName(select.getName()).setProfileId(select.getProfileId());
            return select;
        }
        throw new ServiceException("The point update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.POINT + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Point selectById(Long id) {
        Point point = pointMapper.selectById(id);
        if (null == point) {
            throw new NotFoundException("The point does not exist");
        }
        return point;
    }

    @Override
    @Cacheable(value = Common.Cache.POINT + Common.Cache.NAME + Common.Cache.PROFILE_ID, key = "#name+'.'+#profileId", unless = "#result==null")
    public Point selectByNameAndProfileId(String name, Long profileId) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        queryWrapper.eq(Point::getName, name);
        queryWrapper.eq(Point::getProfileId, profileId);
        Point point = pointMapper.selectOne(queryWrapper);
        if (null == point) {
            throw new NotFoundException("The point does not exist");
        }
        return point;
    }

    @Override
    @Cacheable(value = Common.Cache.POINT + Common.Cache.PROFILE_ID + Common.Cache.LIST, key = "#profileId", unless = "#result==null")
    public List<Point> selectByProfileId(Long profileId) {
        PointDto pointDto = new PointDto();
        pointDto.setProfileId(profileId);
        List<Point> points = pointMapper.selectList(fuzzyQuery(pointDto));
        if (null == points || points.size() < 1) {
            throw new NotFoundException("The points does not exist");
        }
        return points;
    }

    @Override
    public List<Point> selectByDeviceId(Long deviceId) {
        Device device = deviceService.selectById(deviceId);
        return selectByProfileId(device.getProfileId());
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
        if (null != pointDto) {
            if (StrUtil.isNotBlank(pointDto.getName())) {
                queryWrapper.like(Point::getName, pointDto.getName());
            }
            if (StrUtil.isNotBlank(pointDto.getType())) {
                queryWrapper.eq(Point::getType, pointDto.getType());
            }
            if (null != pointDto.getRw()) {
                queryWrapper.eq(Point::getRw, pointDto.getRw());
            }
            if (null != pointDto.getAccrue()) {
                queryWrapper.eq(Point::getAccrue, pointDto.getAccrue());
            }
            if (null != pointDto.getProfileId()) {
                queryWrapper.eq(Point::getProfileId, pointDto.getProfileId());
            }
        }
        return queryWrapper;
    }

}

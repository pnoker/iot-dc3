/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.device.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.PointDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Point;
import com.pnoker.device.manager.mapper.PointMapper;
import com.pnoker.device.manager.service.PointService;
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
 * <p>位号服务接口实现类
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
                    @CachePut(value = Common.Cache.POINT_ID, key = "#point.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_NAME, key = "#point.name+'.'+#point.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.POINT_LIST, allEntries = true, condition = "#result!=null")}
    )
    public Point add(Point point) {
        Point select = selectByProfileAndName(point.getProfileId(), point.getName());
        if (null != select) {
            throw new ServiceException("point already exists");
        }
        if (pointMapper.insert(point) > 0) {
            return pointMapper.selectById(point.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return pointMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_ID, key = "#point.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_NAME, key = "#point.profileId+'.'+#point.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.POINT_LIST, allEntries = true, condition = "#result!=null")}
    )
    public Point update(Point point) {
        point.setUpdateTime(null);
        Point selectById = pointMapper.selectById(point.getId());
        if (!selectById.getProfileId().equals(point.getProfileId()) || !selectById.getName().equals(point.getName())) {
            Point select = selectByProfileAndName(point.getProfileId(), point.getName());
            if (null != select) {
                throw new ServiceException("point already exists");
            }
        }
        if (pointMapper.updateById(point) > 0) {
            Point select = selectById(point.getId());
            point.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_ID, key = "#id", unless = "#result==null")
    public Point selectById(Long id) {
        return pointMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_NAME, key = "#profileId+'.'+#name", unless = "#result==null")
    public Point selectByProfileAndName(Long profileId, String name) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        queryWrapper.eq(Point::getProfileId, profileId);
        queryWrapper.eq(Point::getName, name);
        return pointMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Point> list(PointDto pointDto) {
        return pointMapper.selectPage(pointDto.getPage().convert(), fuzzyQuery(pointDto));
    }

    @Override
    public LambdaQueryWrapper<Point> fuzzyQuery(PointDto pointDto) {
        LambdaQueryWrapper<Point> queryWrapper = Wrappers.<Point>query().lambda();
        Optional.ofNullable(pointDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Point::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}

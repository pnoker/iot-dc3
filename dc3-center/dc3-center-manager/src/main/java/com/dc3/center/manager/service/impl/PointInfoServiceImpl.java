/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.PointInfoMapper;
import com.dc3.center.manager.service.PointInfoService;
import com.dc3.center.manager.service.PointService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.CacheConstant;
import com.dc3.common.dto.PointInfoDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Point;
import com.dc3.common.model.PointInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PointInfoService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class PointInfoServiceImpl implements PointInfoService {

    @Resource
    private PointService pointService;

    @Resource
    private PointInfoMapper pointInfoMapper;


    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ID, key = "#pointInfo.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ATTRIBUTE_ID + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.POINT_ID, key = "#pointInfo.pointAttributeId+'.'+#pointInfo.deviceId+'.'+#pointInfo.pointId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ATTRIBUTE_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.POINT_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public PointInfo add(PointInfo pointInfo) {
        try {
            selectByAttributeIdAndDeviceIdAndPointId(pointInfo.getPointAttributeId(), pointInfo.getDeviceId(), pointInfo.getPointId());
            throw new DuplicateException("The point info already exists");
        } catch (NotFoundException notFoundException) {
            if (pointInfoMapper.insert(pointInfo) > 0) {
                return pointInfoMapper.selectById(pointInfo.getId());
            }
            throw new ServiceException("The point info add failed");
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ATTRIBUTE_ID + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.POINT_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ATTRIBUTE_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.POINT_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        selectById(id);
        return pointInfoMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ID, key = "#pointInfo.id", condition = "#result!=null"),
                    @CachePut(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ATTRIBUTE_ID + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.POINT_ID, key = "#pointInfo.pointAttributeId+'.'+#pointInfo.deviceId+'.'+#pointInfo.pointId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ATTRIBUTE_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.POINT_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public PointInfo update(PointInfo pointInfo) {
        PointInfo old = selectById(pointInfo.getId());
        pointInfo.setUpdateTime(null);
        if (!old.getPointAttributeId().equals(pointInfo.getPointAttributeId()) || !old.getDeviceId().equals(pointInfo.getDeviceId()) || !old.getPointId().equals(pointInfo.getPointId())) {
            try {
                selectByAttributeIdAndDeviceIdAndPointId(pointInfo.getPointAttributeId(), pointInfo.getDeviceId(), pointInfo.getPointId());
                throw new DuplicateException("The point info already exists");
            } catch (NotFoundException ignored) {
            }
        }
        if (pointInfoMapper.updateById(pointInfo) > 0) {
            PointInfo select = pointInfoMapper.selectById(pointInfo.getId());
            pointInfo.setPointAttributeId(select.getPointAttributeId()).setDeviceId(select.getDeviceId()).setPointId(select.getPointId());
            return select;
        }
        throw new ServiceException("The point info update failed");
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ID, key = "#id", unless = "#result==null")
    public PointInfo selectById(Long id) {
        PointInfo pointInfo = pointInfoMapper.selectById(id);
        if (null == pointInfo) {
            throw new NotFoundException("The point info does not exist");
        }
        return pointInfo;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ATTRIBUTE_ID + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.POINT_ID, key = "#pointAttributeId+'.'+#deviceId+'.'+#pointId", unless = "#result==null")
    public PointInfo selectByAttributeIdAndDeviceIdAndPointId(Long pointAttributeId, Long deviceId, Long pointId) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        queryWrapper.eq(PointInfo::getPointAttributeId, pointAttributeId);
        queryWrapper.eq(PointInfo::getDeviceId, deviceId);
        queryWrapper.eq(PointInfo::getPointId, pointId);
        PointInfo pointInfo = pointInfoMapper.selectOne(queryWrapper);
        if (null == pointInfo) {
            throw new NotFoundException("The point info does not exist");
        }
        return pointInfo;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.ATTRIBUTE_ID + CacheConstant.Suffix.LIST, key = "#pointAttributeId", unless = "#result==null")
    public List<PointInfo> selectByAttributeId(Long pointAttributeId) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        queryWrapper.eq(PointInfo::getPointAttributeId, pointAttributeId);
        List<PointInfo> pointInfos = pointInfoMapper.selectList(queryWrapper);
        if (null == pointInfos || pointInfos.size() < 1) {
            throw new NotFoundException("The point infos does not exist");
        }
        return pointInfos;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.LIST, key = "#deviceId", unless = "#result==null")
    public List<PointInfo> selectByDeviceId(Long deviceId) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        List<Point> points = pointService.selectByDeviceId(deviceId);
        Set<Long> pointIds = points.stream().map(Point::getId).collect(Collectors.toSet());
        queryWrapper.eq(PointInfo::getDeviceId, deviceId);
        queryWrapper.in(PointInfo::getPointId, pointIds);
        List<PointInfo> pointInfos = pointInfoMapper.selectList(queryWrapper);
        if (null == pointInfos) {
            throw new NotFoundException("The point infos does not exist");
        }
        return pointInfos;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.DEVICE_ID + CacheConstant.Suffix.POINT_ID + CacheConstant.Suffix.LIST, key = "#deviceId+'.'+#pointId", unless = "#result==null")
    public List<PointInfo> selectByDeviceIdAndPointId(Long deviceId, Long pointId) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        queryWrapper.eq(PointInfo::getDeviceId, deviceId);
        queryWrapper.eq(PointInfo::getPointId, pointId);
        List<PointInfo> pointInfos = pointInfoMapper.selectList(queryWrapper);
        if (null == pointInfos) {
            throw new NotFoundException("The point infos does not exist");
        }
        return pointInfos;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.POINT_INFO + CacheConstant.Suffix.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<PointInfo> list(PointInfoDto pointInfoDto) {
        if (!Optional.ofNullable(pointInfoDto.getPage()).isPresent()) {
            pointInfoDto.setPage(new Pages());
        }
        return pointInfoMapper.selectPage(pointInfoDto.getPage().convert(), fuzzyQuery(pointInfoDto));
    }

    @Override
    public LambdaQueryWrapper<PointInfo> fuzzyQuery(PointInfoDto pointInfoDto) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        if (null != pointInfoDto) {
            if (null != pointInfoDto.getPointAttributeId()) {
                queryWrapper.eq(PointInfo::getPointAttributeId, pointInfoDto.getPointAttributeId());
            }
            if (null != pointInfoDto.getDeviceId()) {
                queryWrapper.eq(PointInfo::getDeviceId, pointInfoDto.getDeviceId());
            }
            if (null != pointInfoDto.getPointId()) {
                queryWrapper.eq(PointInfo::getPointId, pointInfoDto.getPointId());
            }
        }
        return queryWrapper;
    }

}

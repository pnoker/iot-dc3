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
import com.pnoker.common.dto.DeviceDto;
import com.pnoker.common.dto.PointDto;
import com.pnoker.common.dto.PointInfoDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Driver;
import com.pnoker.common.model.Point;
import com.pnoker.common.model.PointInfo;
import com.pnoker.device.manager.mapper.PointInfoMapper;
import com.pnoker.device.manager.service.DeviceService;
import com.pnoker.device.manager.service.DriverService;
import com.pnoker.device.manager.service.PointInfoService;
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
 * <p>位号配置信息服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class PointInfoServiceImpl implements PointInfoService {

    @Resource
    private PointInfoMapper pointInfoMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_INFO_ID, key = "#pointInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_INFO_NAME, key = "#pointInfo.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.POINT_INFO_LIST, allEntries = true, condition = "#result!=null")}
    )
    public PointInfo add(PointInfo pointInfo) {
        PointInfo select = selectByName(pointInfo.getName());
        if (null != select) {
            throw new ServiceException("point info already exists");
        }
        if (pointInfoMapper.insert(pointInfo) > 0) {
            return pointInfoMapper.selectById(pointInfo.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.POINT_INFO_ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_INFO_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.POINT_INFO_LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return pointInfoMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.POINT_INFO_ID, key = "#pointInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.POINT_INFO_NAME, key = "#pointInfo.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.POINT_INFO_LIST, allEntries = true, condition = "#result!=null")}
    )
    public PointInfo update(PointInfo pointInfo) {
        if (pointInfoMapper.updateById(pointInfo) > 0) {
            PointInfo select = selectById(pointInfo.getId());
            pointInfo.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_INFO_ID, key = "#id", unless = "#result==null")
    public PointInfo selectById(Long id) {
        return pointInfoMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_INFO_NAME, key = "#name", unless = "#result==null")
    public PointInfo selectByName(String name) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        queryWrapper.like(PointInfo::getName, name);
        return pointInfoMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.POINT_INFO_LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<PointInfo> list(PointInfoDto pointInfoDto) {
        return pointInfoMapper.selectPage(pointInfoDto.getPage().convert(), fuzzyQuery(pointInfoDto));
    }

    @Override
    public LambdaQueryWrapper<PointInfo> fuzzyQuery(PointInfoDto pointInfoDto) {
        LambdaQueryWrapper<PointInfo> queryWrapper = Wrappers.<PointInfo>query().lambda();
        Optional.ofNullable(pointInfoDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(PointInfo::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}

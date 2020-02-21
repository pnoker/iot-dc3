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

package com.pnoker.center.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.manager.mapper.DriverInfoMapper;
import com.pnoker.center.manager.service.DriverInfoService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DriverInfoDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.DriverInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>DriverInfoService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DriverInfoServiceImpl implements DriverInfoService {
    @Resource
    private DriverInfoMapper driverInfoMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#driverInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.DRIVER_INFO_ID, key = "#driverInfo.profileInfoId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public DriverInfo add(DriverInfo driverInfo) {
        DriverInfo select = selectByDriverAttributeId(driverInfo.getDriverAttributeId());
        if (null != select) {
            throw new ServiceException("driver info already exists");
        }
        if (driverInfoMapper.insert(driverInfo) > 0) {
            return driverInfoMapper.selectById(driverInfo.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DRIVER_INFO_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        return driverInfoMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#driverInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.DRIVER_INFO_ID, key = "#driverInfo.profileInfoId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public DriverInfo update(DriverInfo driverInfo) {
        driverInfo.setUpdateTime(null);
        if (driverInfoMapper.updateById(driverInfo) > 0) {
            DriverInfo select = selectById(driverInfo.getId());
            driverInfo.setDriverAttributeId(select.getDriverAttributeId());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#id", unless = "#result==null")
    public DriverInfo selectById(Long id) {
        return driverInfoMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.DRIVER_INFO_ID, key = "#id", unless = "#result==null")
    public DriverInfo selectByDriverAttributeId(Long id) {
        LambdaQueryWrapper<DriverInfo> queryWrapper = Wrappers.<DriverInfo>query().lambda();
        queryWrapper.like(DriverInfo::getDriverAttributeId, id);
        return driverInfoMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<DriverInfo> list(DriverInfoDto driverInfoDto) {
        if (!Optional.ofNullable(driverInfoDto.getPage()).isPresent()) {
            driverInfoDto.setPage(new Pages());
        }
        return driverInfoMapper.selectPage(driverInfoDto.getPage().convert(), fuzzyQuery(driverInfoDto));
    }

    @Override
    public LambdaQueryWrapper<DriverInfo> fuzzyQuery(DriverInfoDto driverInfoDto) {
        LambdaQueryWrapper<DriverInfo> queryWrapper = Wrappers.<DriverInfo>query().lambda();
        Optional.ofNullable(driverInfoDto).ifPresent(dto -> {
            if (null != dto.getDriverAttributeId()) {
                queryWrapper.eq(DriverInfo::getDriverAttributeId, dto.getDriverAttributeId());
            }
            if (null != dto.getProfileId()) {
                queryWrapper.eq(DriverInfo::getProfileId, dto.getProfileId());
            }
        });
        return queryWrapper;
    }

}

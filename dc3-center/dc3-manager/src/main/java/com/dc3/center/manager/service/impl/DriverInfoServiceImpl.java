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
import com.dc3.center.manager.mapper.DriverInfoMapper;
import com.dc3.center.manager.service.DriverInfoService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DriverInfoDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.DriverInfo;
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
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.DRIVER_INFO_ID, key = "#driverInfo.driverAttributeId+'.'+#driverInfo.profileId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public DriverInfo add(DriverInfo driverInfo) {
        DriverInfo select = selectByDriverAttributeId(driverInfo.getDriverAttributeId(), driverInfo.getProfileId());
        Optional.ofNullable(select).ifPresent(d -> {
            throw new ServiceException("The driver info already exists in the profile");
        });
        if (driverInfoMapper.insert(driverInfo) > 0) {
            return driverInfoMapper.selectById(driverInfo.getId());
        }
        throw new ServiceException("The driver info add failed");
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
        DriverInfo driverInfo = selectById(id);
        if (null == driverInfo) {
            throw new ServiceException("The driver info does not exist");
        }
        return driverInfoMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#driverInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.DRIVER_INFO_ID, key = "#driverInfo.driverAttributeId+'.'+#driverInfo.profileId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public DriverInfo update(DriverInfo driverInfo) {
        DriverInfo temp = selectById(driverInfo.getId());
        if (null == temp) {
            throw new ServiceException("The driver info does not exist");
        }
        driverInfo.setUpdateTime(null);
        DriverInfo select = selectByDriverAttributeId(driverInfo.getDriverAttributeId(), driverInfo.getProfileId());
        boolean update = null == select || (select.getDriverAttributeId().equals(driverInfo.getDriverAttributeId()) && select.getProfileId().equals(driverInfo.getProfileId()));
        if (!update) {
            throw new ServiceException("The driver info already exists");
        }
        if (driverInfoMapper.updateById(driverInfo) > 0) {
            return driverInfoMapper.selectById(driverInfo.getId());
        }
        throw new ServiceException("The driver info update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#id", unless = "#result==null")
    public DriverInfo selectById(Long id) {
        return driverInfoMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.DRIVER_INFO_ID, key = "#driverAttributeId+'.'+#profileId", unless = "#result==null")
    public DriverInfo selectByDriverAttributeId(Long driverAttributeId, Long profileId) {
        LambdaQueryWrapper<DriverInfo> queryWrapper = Wrappers.<DriverInfo>query().lambda();
        queryWrapper.eq(DriverInfo::getDriverAttributeId, driverAttributeId);
        queryWrapper.eq(DriverInfo::getProfileId, profileId);
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
            Optional.ofNullable(dto.getDriverAttributeId()).ifPresent(driverAttributeId ->
                    queryWrapper.eq(DriverInfo::getDriverAttributeId, driverAttributeId));
            Optional.ofNullable(dto.getProfileId()).ifPresent(profileId ->
                    queryWrapper.eq(DriverInfo::getProfileId, profileId));
        });
        return queryWrapper;
    }

}

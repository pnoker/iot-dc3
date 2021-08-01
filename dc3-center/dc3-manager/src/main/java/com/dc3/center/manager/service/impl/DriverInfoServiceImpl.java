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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.DriverInfoMapper;
import com.dc3.center.manager.service.DriverInfoService;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DriverInfoDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.DriverInfo;
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
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.ATTRIBUTE_ID + Common.Cache.DEVICE_ID, key = "#driverInfo.driverAttributeId+'.'+#driverInfo.deviceId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.ATTRIBUTE_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DEVICE_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public DriverInfo add(DriverInfo driverInfo) {
        try {
            selectByAttributeIdAndDeviceId(driverInfo.getDriverAttributeId(), driverInfo.getDeviceId());
            throw new ServiceException("The driver info already exists in the device");
        } catch (NotFoundException notFoundException) {
            if (driverInfoMapper.insert(driverInfo) > 0) {
                return driverInfoMapper.selectById(driverInfo.getId());
            }
            throw new ServiceException("The driver info add failed");
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.ATTRIBUTE_ID + Common.Cache.DEVICE_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.ATTRIBUTE_ID + Common.Cache.LIST, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DEVICE_ID + Common.Cache.LIST, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        selectById(id);
        return driverInfoMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#driverInfo.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DRIVER_INFO + Common.Cache.ATTRIBUTE_ID + Common.Cache.DEVICE_ID, key = "#driverInfo.driverAttributeId+'.'+#driverInfo.deviceId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.ATTRIBUTE_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.DEVICE_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DRIVER_INFO + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public DriverInfo update(DriverInfo driverInfo) {
        DriverInfo oldDriverInfo = selectById(driverInfo.getId());
        driverInfo.setUpdateTime(null);
        if (!oldDriverInfo.getDriverAttributeId().equals(driverInfo.getDriverAttributeId()) || !oldDriverInfo.getDeviceId().equals(driverInfo.getDeviceId())) {
            try {
                selectByAttributeIdAndDeviceId(driverInfo.getDriverAttributeId(), driverInfo.getDeviceId());
                throw new DuplicateException("The driver info already exists");
            } catch (NotFoundException ignored) {
            }
        }
        if (driverInfoMapper.updateById(driverInfo) > 0) {
            DriverInfo select = driverInfoMapper.selectById(driverInfo.getId());
            driverInfo.setDriverAttributeId(select.getDriverAttributeId()).setDeviceId(select.getDeviceId());
            return select;
        }
        throw new ServiceException("The driver info update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.ID, key = "#id", unless = "#result==null")
    public DriverInfo selectById(Long id) {
        DriverInfo driverInfo = driverInfoMapper.selectById(id);
        if (null == driverInfo) {
            throw new NotFoundException("The driver info does not exist");
        }
        return driverInfo;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.ATTRIBUTE_ID + Common.Cache.DEVICE_ID, key = "#driverAttributeId+'.'+#deviceId", unless = "#result==null")
    public DriverInfo selectByAttributeIdAndDeviceId(Long driverAttributeId, Long deviceId) {
        DriverInfoDto driverInfoDto = new DriverInfoDto();
        driverInfoDto.setDriverAttributeId(driverAttributeId);
        driverInfoDto.setDeviceId(deviceId);
        DriverInfo driverInfo = driverInfoMapper.selectOne(fuzzyQuery(driverInfoDto));
        if (null == driverInfo) {
            throw new NotFoundException("The driver info does not exist");
        }
        return driverInfo;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.ATTRIBUTE_ID + Common.Cache.LIST, key = "#driverAttributeId", unless = "#result==null")
    public List<DriverInfo> selectByAttributeId(Long driverAttributeId) {
        DriverInfoDto driverInfoDto = new DriverInfoDto();
        driverInfoDto.setDriverAttributeId(driverAttributeId);
        List<DriverInfo> driverInfos = driverInfoMapper.selectList(fuzzyQuery(driverInfoDto));
        if (null == driverInfos || driverInfos.size() < 1) {
            throw new NotFoundException("The driver infos does not exist");
        }
        return driverInfos;
    }

    @Override
    @Cacheable(value = Common.Cache.DRIVER_INFO + Common.Cache.DEVICE_ID + Common.Cache.LIST, key = "#deviceId", unless = "#result==null")
    public List<DriverInfo> selectByDeviceId(Long deviceId) {
        DriverInfoDto driverInfoDto = new DriverInfoDto();
        driverInfoDto.setDeviceId(deviceId);
        List<DriverInfo> driverInfos = driverInfoMapper.selectList(fuzzyQuery(driverInfoDto));
        if (null == driverInfos || driverInfos.size() < 1) {
            throw new NotFoundException("The driver infos does not exist");
        }
        return driverInfos;
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
        if (null != driverInfoDto) {
            if (null != driverInfoDto.getDriverAttributeId()) {
                queryWrapper.eq(DriverInfo::getDriverAttributeId, driverInfoDto.getDriverAttributeId());
            }
            if (null != driverInfoDto.getDeviceId()) {
                queryWrapper.eq(DriverInfo::getDeviceId, driverInfoDto.getDeviceId());
            }
        }
        return queryWrapper;
    }

}

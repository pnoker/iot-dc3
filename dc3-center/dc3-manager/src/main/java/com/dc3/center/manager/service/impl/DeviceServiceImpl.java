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
import com.dc3.api.center.data.feign.PointValueClient;
import com.dc3.center.manager.mapper.DeviceMapper;
import com.dc3.center.manager.service.DeviceService;
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>DeviceService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private PointValueClient pointValueClient;


    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#device.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.GROUP_NAME, key = "#device.groupId+'.'+#device.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Device add(Device device) {
        Device select = selectDeviceByNameAndGroup(device.getName(), device.getGroupId());
        Optional.ofNullable(select).ifPresent(d -> {
            throw new ServiceException("The device already exists in the group");
        });
        if (deviceMapper.insert(device) > 0) {
            return deviceMapper.selectById(device.getId());
        }
        throw new ServiceException("The device add failed");
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.GROUP_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        Device device = selectById(id);
        if (null == device) {
            throw new ServiceException("The device does not exist");
        }
        return deviceMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#device.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.GROUP_NAME, key = "#device.groupId+'.'+#device.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Device update(Device device) {
        Device temp = selectById(device.getId());
        if (null == temp) {
            throw new ServiceException("The device does not exist");
        }
        device.setUpdateTime(null);
        if (deviceMapper.updateById(device) > 0) {
            Device select = deviceMapper.selectById(device.getId());
            device.setGroupId(select.getGroupId()).setName(select.getName());
            return select;
        }
        throw new ServiceException("The device update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Device selectById(Long id) {
        return deviceMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.GROUP_NAME, key = "#groupId+'.'+#name", unless = "#result==null")
    public Device selectDeviceByNameAndGroup(String name, Long groupId) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.eq(Device::getGroupId, groupId);
        queryWrapper.eq(Device::getName, name);
        return deviceMapper.selectOne(queryWrapper);
    }

    @Override
    public Map<Long, String> deviceStatus(DeviceDto deviceDto) {
        Map<Long, String> deviceStatusMap = new HashMap<>(16);
        Page<Device> devicePage = list(deviceDto);
        if (devicePage.getRecords().size() > 0) {
            devicePage.getRecords().forEach(device -> {
                String status = Common.Device.Status.OFFLINE;
                R<String> rStatus = pointValueClient.status(device.getId());
                if (rStatus.isOk()) {
                    status = rStatus.getData();
                }
                deviceStatusMap.put(device.getId(), status);
            });
        }
        return deviceStatusMap;
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Device> list(DeviceDto deviceDto) {
        if (!Optional.ofNullable(deviceDto.getPage()).isPresent()) {
            deviceDto.setPage(new Pages());
        }
        return deviceMapper.selectPage(deviceDto.getPage().convert(), fuzzyQuery(deviceDto));
    }

    @Override
    public LambdaQueryWrapper<Device> fuzzyQuery(DeviceDto deviceDto) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        Optional.ofNullable(deviceDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Device::getName, dto.getName());
            }
            Optional.ofNullable(dto.getProfileId()).ifPresent(profileId -> queryWrapper.eq(Device::getProfileId, profileId));
            Optional.ofNullable(dto.getGroupId()).ifPresent(groupId -> queryWrapper.eq(Device::getGroupId, groupId));
        });
        return queryWrapper;
    }

}

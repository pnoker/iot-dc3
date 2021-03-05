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
import com.dc3.api.center.data.feign.DeviceEventClient;
import com.dc3.center.manager.mapper.DeviceMapper;
import com.dc3.center.manager.service.DeviceService;
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.exception.NotFoundException;
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
import java.util.List;
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
    private DeviceEventClient deviceEventClient;


    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#device.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.NAME + Common.Cache.GROUP_ID, key = "#device.name+'.'+#device.groupId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.PROFILE_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.GROUP_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Device add(Device device) {
        try {
            selectDeviceByNameAndGroupId(device.getName(), device.getGroupId());
            throw new ServiceException("The device already exists in the group");
        } catch (NotFoundException notFoundException) {
            if (deviceMapper.insert(device) > 0) {
                return deviceMapper.selectById(device.getId());
            }
            throw new ServiceException("The device add failed");
        }
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.NAME + Common.Cache.GROUP_ID, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.PROFILE_ID + Common.Cache.LIST, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.GROUP_ID + Common.Cache.LIST, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        selectById(id);
        return deviceMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#device.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.NAME + Common.Cache.GROUP_ID, key = "#device.name+'.'+#device.groupId", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.PROFILE_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.GROUP_ID + Common.Cache.LIST, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Device update(Device device) {
        selectById(device.getId());
        device.setUpdateTime(null);
        if (deviceMapper.updateById(device) > 0) {
            Device select = deviceMapper.selectById(device.getId());
            device.setName(select.getName()).setGroupId(select.getGroupId());
            return select;
        }
        throw new ServiceException("The device update failed");
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Device selectById(Long id) {
        Device device = deviceMapper.selectById(id);
        if (null == device) {
            throw new NotFoundException("The device does not exist");
        }
        return device;
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.NAME + Common.Cache.GROUP_ID, key = "#name+'.'+#groupId", unless = "#result==null")
    public Device selectDeviceByNameAndGroupId(String name, Long groupId) {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setName(name);
        deviceDto.setGroupId(groupId);
        Device device = deviceMapper.selectOne(fuzzyQuery(deviceDto));
        if (null == device) {
            throw new NotFoundException("The device does not exist");
        }
        return device;
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.PROFILE_ID + Common.Cache.LIST, key = "#profileId", unless = "#result==null")
    public List<Device> selectDeviceByProfileId(Long profileId) {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setProfileId(profileId);
        List<Device> devices = deviceMapper.selectList(fuzzyQuery(deviceDto));
        if (null == devices || devices.size() < 1) {
            throw new NotFoundException("The devices does not exist");
        }
        return devices;
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.GROUP_ID + Common.Cache.LIST, key = "#groupId", unless = "#result==null")
    public List<Device> selectDeviceByGroupId(Long groupId) {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setGroupId(groupId);
        List<Device> devices = deviceMapper.selectList(fuzzyQuery(deviceDto));
        if (null == devices || devices.size() < 1) {
            throw new NotFoundException("The devices does not exist");
        }
        return devices;
    }

    //TODO 合并到list中
    @Override
    public Map<Long, String> deviceStatus(DeviceDto deviceDto) {
        Map<Long, String> deviceStatusMap = new HashMap<>(16);
        Page<Device> devicePage = list(deviceDto);
        if (devicePage.getRecords().size() > 0) {
            devicePage.getRecords().forEach(device -> {
                String status = Common.Device.Status.OFFLINE;
                try {
                    R<String> rStatus = deviceEventClient.deviceStatus(device.getId());
                    if (rStatus.isOk()) {
                        status = rStatus.getData();
                    }
                } catch (Exception ignored) {
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
            if (null != dto.getProfileId()) {
                queryWrapper.eq(Device::getProfileId, dto.getProfileId());
            }
            if (null != dto.getGroupId()) {
                queryWrapper.eq(Device::getGroupId, dto.getGroupId());
            }
        });
        return queryWrapper;
    }

}

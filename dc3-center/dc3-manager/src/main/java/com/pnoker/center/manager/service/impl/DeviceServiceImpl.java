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

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.manager.mapper.DeviceMapper;
import com.pnoker.center.manager.service.DeviceService;
import com.pnoker.center.manager.service.GroupService;
import com.pnoker.center.manager.service.ScheduleService;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DeviceDto;
import com.pnoker.common.dto.ScheduleDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>设备服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    @Resource
    private GroupService groupService;

    @Resource
    private ScheduleService scheduleService;

    @Resource
    private DeviceMapper deviceMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#device.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.CODE, key = "#device.code", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.GROUP_NAME, key = "#device.groupId+'.'+#device.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Device add(Device device) {
        Device select = selectDeviceByNameAndGroup(device.getGroupId(), device.getName());
        if (null != select) {
            throw new ServiceException("device already exists in the group");
        }
        if (deviceMapper.insert(device.setCode(generateDeviceCode())) > 0) {
            createSchedule(device);
            return deviceMapper.selectById(device.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.CODE, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.GROUP_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        boolean delete = deviceMapper.deleteById(id) > 0;
        if (delete) {
            removeSchedule(id);
        }
        return delete;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#device.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.CODE, key = "#device.code", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.DEVICE + Common.Cache.GROUP_NAME, key = "#device.groupId+'.'+#device.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.DIC, allEntries = true, condition = "#result!=null"),
                    @CacheEvict(value = Common.Cache.DEVICE + Common.Cache.LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Device update(Device device) {
        device.setCode(null);
        device.setUpdateTime(null);
        if (deviceMapper.updateById(device) > 0) {
            Device select = selectById(device.getId());
            device.setCode(select.getCode()).setGroupId(select.getGroupId()).setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.ID, key = "#id", unless = "#result==null")
    public Device selectById(Long id) {
        return deviceMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.CODE, key = "#code", unless = "#result==null")
    public Device selectByCode(String code) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.eq(Device::getCode, code);
        return deviceMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.GROUP_NAME, key = "#groupId+'.'+#name", unless = "#result==null")
    public Device selectDeviceByNameAndGroup(long groupId, String name) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.eq(Device::getGroupId, groupId);
        queryWrapper.eq(Device::getName, name);
        return deviceMapper.selectOne(queryWrapper);
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
    @Cacheable(value = Common.Cache.DEVICE + Common.Cache.DIC, key = "'device_dic'", unless = "#result==null")
    public List<Dic> dictionary() {
        List<Dic> groupDicList = groupService.dictionary();
        for (Dic groupDic : groupDicList) {
            List<Dic> dicList = new ArrayList<>();
            LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
            queryWrapper.eq(Device::getGroupId, groupDic.getValue());
            List<Device> deviceList = deviceMapper.selectList(queryWrapper);
            groupDic.setDisabled(true);
            groupDic.setValue(RandomUtil.randomLong());
            for (Device device : deviceList) {
                Dic deviceDic = new Dic().setLabel(device.getName()).setValue(device.getId());
                dicList.add(deviceDic);
            }
            groupDic.setChildren(dicList);
        }
        return groupDicList;
    }

    @Override
    public LambdaQueryWrapper<Device> fuzzyQuery(DeviceDto deviceDto) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        Optional.ofNullable(deviceDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Device::getName, dto.getName());
            }
            if (StringUtils.isNotBlank(dto.getCode())) {
                queryWrapper.eq(Device::getCode, dto.getCode());
            }
            if (null != dto.getStatus()) {
                queryWrapper.eq(Device::getStatus, dto.getStatus());
            }
            if (null != dto.getGroupId()) {
                queryWrapper.eq(Device::getGroupId, dto.getGroupId());
            }
            if (null != dto.getProfileId()) {
                queryWrapper.eq(Device::getProfileId, dto.getProfileId());
            }
        });
        return queryWrapper;
    }

    /**
     * 获取全局唯一的设备CODE编码
     *
     * @return
     */
    public String generateDeviceCode() {
        String code = IdUtil.fastSimpleUUID().toUpperCase();
        if (null != selectByCode(code)) {
            return generateDeviceCode();
        }
        return code;
    }

    public void createSchedule(Device device) {
        Schedule schedule = new Schedule();
        schedule.setDeviceId(device.getId()).setName(Common.SDK_READ_JOB).setCornExpression("*/15 * * * * ?").setBeanName(Common.SDK_READ_JOB).setDescription("Automatically create by default");
        scheduleService.add(schedule);
        schedule.setDeviceId(device.getId()).setName(Common.SDK_WRITE_JOB).setCornExpression("*/15 * * * * ?").setBeanName(Common.SDK_WRITE_JOB).setDescription("Automatically create by default");
        scheduleService.add(schedule);
        schedule.setDeviceId(device.getId()).setName(Common.SDK_CUSTOMIZER_JOB).setCornExpression("*/15 * * * * ?").setBeanName(Common.SDK_CUSTOMIZER_JOB).setDescription("Automatically create by default");
        scheduleService.add(schedule);
    }

    public void removeSchedule(long deviceId) {
        ScheduleDto scheduleDto = new ScheduleDto();
        scheduleDto.setDeviceId(deviceId);
        scheduleDto.setPage(new Pages().setSize(-1L));
        Page<Schedule> page = scheduleService.list(scheduleDto);
        for (Schedule schedule : page.getRecords()) {
            scheduleService.delete(schedule.getId());
        }
    }
}

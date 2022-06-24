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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.center.manager.mapper.DeviceMapper;
import com.dc3.center.manager.service.*;
import com.dc3.common.bean.Pages;
import com.dc3.common.constant.CommonConstant;
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.model.ProfileBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * DeviceService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private ProfileService profileService;
    @Resource
    private PointService pointService;
    @Resource
    private ProfileBindService profileBindService;

    @Resource
    private NotifyService notifyService;

    // 2022-06-23 检查：通过
    @Override
    public Device add(Device device) {
        try {
            selectByName(device.getName(), device.getTenantId());
            throw new DuplicateException("The device already exists");
        } catch (NotFoundException notFoundException) {
            if (deviceMapper.insert(device) > 0) {
                addProfileBind(device.getId(), device.getProfileIds());
                Device select = deviceMapper.selectById(device.getId());
                select.setProfileIds(device.getProfileIds());
                return select;
            }
            throw new ServiceException("The device add failed");
        }
    }

    // 2022-06-23 检查：通过
    @Override
    public boolean delete(String id) {
        selectById(id);
        profileBindService.deleteByDeviceId(id);
        return deviceMapper.deleteById(id) > 0;
    }

    // 2022-06-23 检查：通过
    @Override
    public Device update(Device device) {
        selectById(device.getId());

        Set<String> newProfileIds = null != device.getProfileIds() ? device.getProfileIds() : new HashSet<>();
        Set<String> oldProfileIds = profileBindService.selectProfileIdsByDeviceId(device.getId());

        // 新增的模板
        Set<String> add = new HashSet<>(newProfileIds);
        add.removeAll(oldProfileIds);

        // 删除的模板
        Set<String> delete = new HashSet<>(oldProfileIds);
        delete.removeAll(newProfileIds);

        addProfileBind(device.getId(), add);
        delete.forEach(profileId -> profileBindService.deleteByDeviceIdAndProfileId(device.getId(), profileId));

        device.setUpdateTime(null);
        if (deviceMapper.updateById(device) > 0) {
            Device select = deviceMapper.selectById(device.getId());
            select.setProfileIds(newProfileIds);
            device.setName(select.getName());
            return select;
        }
        throw new ServiceException("The device update failed");
    }

    @Override
    public Device selectById(String id) {
        Device device = deviceMapper.selectById(id);
        if (null == device) {
            throw new NotFoundException("The device does not exist");
        }
        return device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(id));
    }

    @Override
    public Device selectByName(String name, String tenantId) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.eq(Device::getName, name);
        queryWrapper.eq(Device::getTenantId, tenantId);
        Device device = deviceMapper.selectOne(queryWrapper);
        if (null == device) {
            throw new NotFoundException("The device does not exist");
        }
        return device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId()));
    }

    @Override
    public List<Device> selectByDriverId(String driverId) {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setDriverId(driverId);
        List<Device> devices = deviceMapper.selectList(fuzzyQuery(deviceDto));
        if (null == devices || devices.size() < 1) {
            throw new NotFoundException("The devices does not exist");
        }
        devices.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return devices;
    }

    @Override
    public List<Device> selectByProfileId(String profileId) {
        return selectByIds(profileBindService.selectDeviceIdsByProfileId(profileId));
    }

    @Override
    public List<Device> selectByIds(Set<String> ids) {
        List<Device> devices = deviceMapper.selectBatchIds(ids);
        if (CollectionUtil.isEmpty(devices)) {
            throw new NotFoundException("The devices does not exist");
        }
        devices.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return devices;
    }

    @Override
    public Page<Device> list(DeviceDto deviceDto) {
        if (!Optional.ofNullable(deviceDto.getPage()).isPresent()) {
            deviceDto.setPage(new Pages());
        }
        Page<Device> page = deviceMapper.selectPage(deviceDto.getPage().convert(), fuzzyQuery(deviceDto));
        page.getRecords().forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return page;
    }

    @Override
    public LambdaQueryWrapper<Device> fuzzyQuery(DeviceDto deviceDto) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        if (null != deviceDto) {
            if (StrUtil.isNotBlank(deviceDto.getName())) {
                queryWrapper.like(Device::getName, deviceDto.getName());
            }
            if (null != deviceDto.getDriverId()) {
                queryWrapper.eq(Device::getDriverId, deviceDto.getDriverId());
            }
            if (null != deviceDto.getEnable()) {
                queryWrapper.eq(Device::getEnable, deviceDto.getEnable());
            }
            if (null != deviceDto.getTenantId()) {
                queryWrapper.eq(Device::getTenantId, deviceDto.getTenantId());
            }
        }
        return queryWrapper;
    }

    private void addProfileBind(String deviceId, Set<String> profileIds) {
        if (null != profileIds) {
            profileIds.forEach(profileId -> {
                try {
                    profileService.selectById(profileId);
                    profileBindService.add(new ProfileBind(profileId, deviceId));

                    List<Point> points = pointService.selectByProfileId(profileId);
                    // 通知驱动新增位号
                    points.forEach(point -> notifyService.notifyDriverPoint(CommonConstant.Driver.Point.ADD, point));
                } catch (Exception ignored) {
                }
            });
        }
    }

}

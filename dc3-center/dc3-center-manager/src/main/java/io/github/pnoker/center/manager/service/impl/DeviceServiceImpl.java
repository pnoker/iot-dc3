/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.common.constant.MetadataConstant;
import io.github.pnoker.common.dto.DeviceDto;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.model.ProfileBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DeviceService Impl
 *
 * @author pnoker
 * @since 2022.1.0
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        selectById(id);
        profileBindService.deleteByDeviceId(id);
        return deviceMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Device selectById(String id) {
        Device device = deviceMapper.selectById(id);
        if (null == device) {
            throw new NotFoundException();
        }
        return device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Device selectByName(String name, String tenantId) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.eq(Device::getName, name);
        queryWrapper.eq(Device::getTenantId, tenantId);
        Device device = deviceMapper.selectOne(queryWrapper);
        if (null == device) {
            throw new NotFoundException();
        }
        return device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Device> selectByDriverId(String driverId) {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setDriverId(driverId);
        List<Device> devices = deviceMapper.selectList(fuzzyQuery(deviceDto));
        if (null == devices || devices.isEmpty()) {
            throw new NotFoundException();
        }
        devices.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return devices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Device> selectByProfileId(String profileId) {
        return selectByIds(profileBindService.selectDeviceIdsByProfileId(profileId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Device> selectByIds(Set<String> ids) {
        List<Device> devices = deviceMapper.selectBatchIds(ids);
        if (CollUtil.isEmpty(devices)) {
            throw new NotFoundException();
        }
        devices.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return devices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Device> list(DeviceDto deviceDto) {
        if (ObjectUtil.isNull(deviceDto.getPage())) {
            deviceDto.setPage(new Pages());
        }
        return deviceMapper.selectPageWithProfile(deviceDto.getPage().convert(), customFuzzyQuery(deviceDto), deviceDto.getProfileId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<Device> fuzzyQuery(DeviceDto deviceDto) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        if (ObjectUtil.isNotEmpty(deviceDto)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(deviceDto.getName()), Device::getName, deviceDto.getName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(deviceDto.getDriverId()), Device::getDriverId, deviceDto.getDriverId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(deviceDto.getEnable()), Device::getEnable, deviceDto.getEnable());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(deviceDto.getTenantId()), Device::getTenantId, deviceDto.getTenantId());
        }
        return queryWrapper;
    }

    public LambdaQueryWrapper<Device> customFuzzyQuery(DeviceDto deviceDto) {
        QueryWrapper<Device> queryWrapper = Wrappers.query();
        queryWrapper.eq("dd.deleted", 0);
        if (ObjectUtil.isNotNull(deviceDto)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(deviceDto.getName()), "dd.name", deviceDto.getName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(deviceDto.getDriverId()), "dd.driver_id", deviceDto.getDriverId());
            queryWrapper.eq(ObjectUtil.isNotNull(deviceDto.getEnable()), "dd.enable", deviceDto.getEnable());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(deviceDto.getTenantId()), "dd.tenant_id", deviceDto.getTenantId());
        }
        return queryWrapper.lambda();
    }

    private void addProfileBind(String deviceId, Set<String> profileIds) {
        if (null != profileIds) {
            profileIds.forEach(profileId -> {
                try {
                    profileService.selectById(profileId);
                    profileBindService.add(new ProfileBind(profileId, deviceId));

                    List<Point> points = pointService.selectByProfileId(profileId);
                    // 通知驱动新增位号
                    points.forEach(point -> notifyService.notifyDriverPoint(MetadataConstant.Point.ADD, point));
                } catch (Exception ignored) {
                    // nothing to do
                }
            });
        }
    }

}

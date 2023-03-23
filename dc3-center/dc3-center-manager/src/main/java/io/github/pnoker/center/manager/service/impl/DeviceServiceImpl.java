/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
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
import io.github.pnoker.center.manager.entity.query.DevicePageQuery;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.driver.MetadataConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
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
            selectByName(device.getDeviceName(), device.getTenantId());
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

        Set<String> newProfileIds = ObjectUtil.isNotNull(device.getProfileIds()) ? device.getProfileIds() : new HashSet<>();
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
            device.setDeviceName(select.getDeviceName());
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
        if (ObjectUtil.isNull(device)) {
            throw new NotFoundException();
        }
        device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(id));
        return device;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Device selectByName(String name, String tenantId) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        queryWrapper.eq(Device::getDeviceName, name);
        queryWrapper.eq(Device::getTenantId, tenantId);
        queryWrapper.last("limit 1");
        Device device = deviceMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(device)) {
            throw new NotFoundException();
        }
        device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId()));
        return device;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Device> selectByDriverId(String driverId) {
        DevicePageQuery devicePageQuery = new DevicePageQuery();
        devicePageQuery.setDriverId(driverId);
        List<Device> devices = deviceMapper.selectList(fuzzyQuery(devicePageQuery));
        if (ObjectUtil.isNull(devices) || devices.isEmpty()) {
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
    public Page<Device> list(DevicePageQuery devicePageQuery) {
        if (ObjectUtil.isNull(devicePageQuery.getPage())) {
            devicePageQuery.setPage(new Pages());
        }
        return deviceMapper.selectPageWithProfile(devicePageQuery.getPage().convert(), customFuzzyQuery(devicePageQuery), devicePageQuery.getProfileId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<Device> fuzzyQuery(DevicePageQuery devicePageQuery) {
        LambdaQueryWrapper<Device> queryWrapper = Wrappers.<Device>query().lambda();
        if (ObjectUtil.isNotEmpty(devicePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(devicePageQuery.getDeviceName()), Device::getDeviceName, devicePageQuery.getDeviceName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getDeviceCode()), Device::getDeviceCode, devicePageQuery.getDeviceCode());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getDriverId()), Device::getDriverId, devicePageQuery.getDriverId());
            queryWrapper.eq(ObjectUtil.isNotEmpty(devicePageQuery.getEnableFlag()), Device::getEnableFlag, devicePageQuery.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getTenantId()), Device::getTenantId, devicePageQuery.getTenantId());
        }
        return queryWrapper;
    }

    public LambdaQueryWrapper<Device> customFuzzyQuery(DevicePageQuery devicePageQuery) {
        QueryWrapper<Device> queryWrapper = Wrappers.query();
        queryWrapper.eq("dd.deleted", 0);
        if (ObjectUtil.isNotNull(devicePageQuery)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(devicePageQuery.getDeviceName()), "dd.device_name", devicePageQuery.getDeviceName());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getDeviceCode()), "dd.device_code", devicePageQuery.getDeviceCode());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getDriverId()), "dd.driver_id", devicePageQuery.getDriverId());
            queryWrapper.eq(ObjectUtil.isNotNull(devicePageQuery.getEnableFlag()), "dd.enable_flag", devicePageQuery.getEnableFlag());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(devicePageQuery.getTenantId()), "dd.tenant_id", devicePageQuery.getTenantId());
        }
        return queryWrapper.lambda();
    }

    private void addProfileBind(String deviceId, Set<String> profileIds) {
        if (ObjectUtil.isNotNull(profileIds)) {
            profileIds.forEach(profileId -> {
                try {
                    profileService.selectById(profileId);
                    profileBindService.add(new ProfileBind(profileId, deviceId));

                    List<Point> points = pointService.selectByProfileId(profileId);
                    // 通知驱动新增位号
                    points.forEach(point -> notifyService.notifyDriverPoint(MetadataCommandTypeEnum.ADD, point));
                } catch (Exception ignored) {
                    // nothing to do
                }
            });
        }
    }

}

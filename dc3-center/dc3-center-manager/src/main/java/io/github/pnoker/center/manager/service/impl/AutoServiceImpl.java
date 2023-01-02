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

import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.bean.point.PointDetail;
import io.github.pnoker.common.constant.driver.MetadataConstant;
import io.github.pnoker.common.entity.Device;
import io.github.pnoker.common.entity.Point;
import io.github.pnoker.common.entity.Profile;
import io.github.pnoker.common.entity.ProfileBind;
import io.github.pnoker.common.enums.ProfileShareFlagEnum;
import io.github.pnoker.common.enums.ProfileTypeFlagEnum;
import io.github.pnoker.common.exception.DuplicateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * AutoService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class AutoServiceImpl implements AutoService {

    @Resource
    private DeviceService deviceService;
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
    public PointDetail autoCreateDeviceAndPoint(String deviceName, String pointName, String driverId, String tenantId) {
        // 新增设备
        Device device = new Device();
        device.setDeviceName(deviceName);
        device.setDriverId(driverId);
        device.setTenantId(tenantId);
        device.setRemark("auto create by driver");
        try {
            device = deviceService.add(device);

            // 通知驱动新增设备
            notifyService.notifyDriverDevice(MetadataConstant.Device.ADD, device);
        } catch (DuplicateException duplicateException) {
            device = deviceService.selectByName(deviceName, tenantId);
        } catch (Exception ignored) {
            // nothing to do
        }

        // 新增私有模板
        Profile profile = new Profile();
        profile.setProfileName(deviceName);
        profile.setProfileShareFlag(ProfileShareFlagEnum.DRIVER);
        profile.setProfileTypeFlag(ProfileTypeFlagEnum.DRIVER);
        profile.setTenantId(tenantId);
        try {
            profile = profileService.add(profile);
        } catch (DuplicateException duplicateException) {
            profile = profileService.selectByNameAndType(deviceName, ProfileTypeFlagEnum.DRIVER, tenantId);
        } catch (Exception ignored) {
            // nothing to do
        }

        // 绑定模板
        if (CharSequenceUtil.isAllNotEmpty(device.getId(), profile.getId())) {
            try {
                ProfileBind profileBind = new ProfileBind();
                profileBind.setDeviceId(device.getId());
                profileBind.setProfileId(profile.getId());
                profileBindService.add(profileBind);
            } catch (Exception ignored) {
                // nothing to do
            }

            // 新增位号
            Point point = new Point();
            point.setPointName(pointName);
            point.setProfileId(profile.getId());
            point.setTenantId(tenantId);
            point.setDefault();
            try {
                point = pointService.add(point);

                // 同时驱动新增位号
                notifyService.notifyDriverPoint(MetadataConstant.Point.ADD, point);
            } catch (DuplicateException duplicateException) {
                point = pointService.selectByNameAndProfileId(pointName, profile.getId());
            } catch (Exception ignored) {
                // nothing to do
            }

            return new PointDetail(device.getId(), point.getId());
        }
        return null;
    }
}

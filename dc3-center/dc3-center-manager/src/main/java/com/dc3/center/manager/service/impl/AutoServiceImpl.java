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

import com.dc3.center.manager.service.*;
import com.dc3.common.bean.point.PointDetail;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.DuplicateException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.model.Profile;
import com.dc3.common.model.ProfileBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * AutoService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class AutoServiceImpl implements AutoService {

    @Resource
    private DeviceService deviceService;
    @Resource
    private ProfileService profileService;
    @Resource
    private ProfileBindService profileBindService;
    @Resource
    private PointService pointService;

    @Resource
    private NotifyService notifyService;


    @Override
    public PointDetail autoCreateDeviceAndPoint(String deviceName, String pointName, Long driverId, Long tenantId) {
        // add device
        Device device = new Device();
        device.setName(deviceName).setDriverId(driverId).setTenantId(tenantId).setDescription("auto create by driver");
        try {
            device = deviceService.add(device);

            // notify driver add device
            notifyService.notifyDriverDevice(Common.Driver.Device.ADD, device);
        } catch (DuplicateException duplicateException) {
            device = deviceService.selectByName(deviceName, tenantId);
        } catch (Exception ignored) {
        }

        // add private profile for device
        Profile profile = new Profile();
        profile.setName(deviceName).setShare(false).setType((short) 2).setTenantId(tenantId);
        try {
            profile = profileService.add(profile);
        } catch (DuplicateException duplicateException) {
            profile = profileService.selectByNameAndType(deviceName, (short) 2, tenantId);
        } catch (Exception ignored) {
        }

        // add profile bind
        if (null != device.getId() && null != profile.getId()) {
            try {
                ProfileBind profileBind = new ProfileBind();
                profileBind.setDeviceId(device.getId()).setProfileId(profile.getId());
                profileBindService.add(profileBind);
            } catch (Exception ignored) {
            }

            // add point
            Point point = new Point();
            point.setName(pointName).setProfileId(profile.getId()).setTenantId(tenantId).setDefault();
            try {
                point = pointService.add(point);

                // notify driver add point
                notifyService.notifyDriverPoint(Common.Driver.Point.ADD, point);
            } catch (DuplicateException duplicateException) {
                point = pointService.selectByNameAndProfileId(pointName, profile.getId());
            } catch (Exception ignored) {
            }

            return new PointDetail(device.getId(), point.getId());
        }
        return null;
    }
}

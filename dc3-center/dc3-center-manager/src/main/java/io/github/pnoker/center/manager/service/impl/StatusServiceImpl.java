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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.service.DeviceService;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.center.manager.service.StatusService;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.dto.DeviceDto;
import io.github.pnoker.common.dto.DriverDto;
import io.github.pnoker.common.enums.StatusEnum;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Driver;
import io.github.pnoker.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * DeviceService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class StatusServiceImpl implements StatusService {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private DriverService driverService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private ProfileBindService profileBindService;

    @Override
    public String driver(String serviceName) {
        String key = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + serviceName;
        String status = redisUtil.getKey(key);
        status = null != status ? status : StatusEnum.OFFLINE.getCode();
        return status;
    }

    @Override
    public Map<String, String> driver(DriverDto driverDto) {
        Map<String, String> statusMap = new HashMap<>(16);

        Page<Driver> page = driverService.list(driverDto);
        page.getRecords().forEach(driver -> {
            String key = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + driver.getServiceName();
            String status = redisUtil.getKey(key);
            status = null != status ? status : StatusEnum.OFFLINE.getCode();
            statusMap.put(driver.getId(), status);
        });
        return statusMap;
    }

    @Override
    public String device(String id) {
        String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
        String status = redisUtil.getKey(key);
        status = null != status ? status : StatusEnum.OFFLINE.getCode();
        return status;
    }

    @Override
    public Map<String, String> device(DeviceDto deviceDto) {
        Map<String, String> statusMap = new HashMap<>(16);

        Page<Device> page = deviceService.list(deviceDto);
        page.getRecords().forEach(device -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + device.getId();
            String status = redisUtil.getKey(key);
            status = null != status ? status : StatusEnum.OFFLINE.getCode();
            statusMap.put(device.getId(), status);
        });
        return statusMap;
    }

    @Override
    public Map<String, String> deviceByProfileId(String profileId) {
        Map<String, String> statusMap = new HashMap<>(16);

        profileBindService.selectDeviceIdsByProfileId(profileId).forEach(id -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
            String status = redisUtil.getKey(key);
            status = null != status ? status : StatusEnum.OFFLINE.getCode();
            statusMap.put(id, status);
        });
        return statusMap;
    }

}

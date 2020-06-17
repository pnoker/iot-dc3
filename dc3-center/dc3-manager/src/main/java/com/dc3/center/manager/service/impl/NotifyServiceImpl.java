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

import com.baomidou.mybatisplus.extension.api.R;
import com.dc3.center.manager.service.DeviceService;
import com.dc3.center.manager.service.DriverService;
import com.dc3.center.manager.service.NotifyService;
import com.dc3.center.manager.service.ProfileService;
import com.dc3.common.bean.driver.DriverOperation;
import com.dc3.common.constant.Operation;
import com.dc3.common.model.Device;
import com.dc3.common.model.Driver;
import com.dc3.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * NotifyService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
    @Resource
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private DriverService driverService;
    @Resource
    private ProfileService profileService;
    @Resource
    private DeviceService deviceService;

    @Override
    public boolean notifyDriverAddProfile(Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Profile.ADD).setId(profileId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver add profile failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverDeleteProfile(Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Profile.DELETE).setId(profileId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver delete profile failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverAddDevice(Long deviceId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Device.ADD).setId(deviceId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver add device failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverDeleteDevice(Long deviceId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Device.DELETE).setId(deviceId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver delete device failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverUpdateDevice(Long deviceId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Device.UPDATE).setId(deviceId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver update device failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverAddPoint(Long pointId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Point.ADD).setId(pointId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver add point failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverDeletePoint(Long pointId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Point.DELETE).setId(pointId).setParentId(profileId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver delete point failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverUpdatePoint(Long pointId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.Point.UPDATE).setId(pointId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver update point failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverAddDriverInfo(Long driverInfoId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.DriverInfo.ADD).setId(driverInfoId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver add driver info failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverDeleteDriverInfo(Long driverInfoId, Long attributeId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.DriverInfo.DELETE).setParentId(profileId).setAttributeId(attributeId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver delete driver info failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverUpdateDriverInfo(Long driverInfoId, Long profileId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByProfileId(profileId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.PointInfo.ADD).setId(driverInfoId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver update driver info failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverAddPointInfo(Long pointInfoId, Long deviceId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByDeviceId(deviceId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.PointInfo.ADD).setId(pointInfoId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver add point info failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverDeletePointInfo(Long pointId, Long attributeId, Long deviceId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByDeviceId(deviceId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.PointInfo.DELETE).setId(pointId).setParentId(deviceId).setAttributeId(attributeId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver delete point info failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    @Override
    public boolean notifyDriverUpdatePointInfo(Long pointInfoId, Long deviceId) {
        scheduledThreadPoolExecutor.schedule(() -> {
            Driver driver = getDriverByDeviceId(deviceId);
            DriverOperation operation = new DriverOperation().setCommand(Operation.PointInfo.UPDATE).setId(pointInfoId);
            try {
                restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
            } catch (Exception e) {
                log.warn("notification driver update point info failed {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        return false;
    }

    /**
     * 获取设备所属驱动
     *
     * @param deviceId
     * @return
     */
    public Driver getDriverByDeviceId(Long deviceId) {
        Device device = deviceService.selectById(deviceId);
        if (null != device) {
            Profile profile = profileService.selectById(device.getProfileId());
            if (null != profile) {
                return driverService.selectById(profile.getDriverId());
            }
        }
        return null;
    }

    /**
     * 获取模板所属驱动
     *
     * @param profileId
     * @return
     */
    public Driver getDriverByProfileId(Long profileId) {
        Profile profile = profileService.selectById(profileId);
        if (null != profile) {
            return driverService.selectById(profile.getDriverId());
        }
        return null;
    }
}

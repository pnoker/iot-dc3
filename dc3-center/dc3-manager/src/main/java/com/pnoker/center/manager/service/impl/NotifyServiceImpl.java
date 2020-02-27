package com.pnoker.center.manager.service.impl;

import com.baomidou.mybatisplus.extension.api.R;
import com.pnoker.center.manager.service.DeviceService;
import com.pnoker.center.manager.service.DriverService;
import com.pnoker.center.manager.service.NotifyService;
import com.pnoker.center.manager.service.ProfileService;
import com.pnoker.common.bean.driver.DriverOperation;
import com.pnoker.common.constant.Operation;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Driver;
import com.pnoker.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * NotifyService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private DriverService driverService;
    @Resource
    private ProfileService profileService;
    @Resource
    private DeviceService deviceService;

    @Override
    public void notifyDriverAddDevice(Long deviceId, Long profileId) {
        Driver driver = getDriverByProfile(profileId);
        DriverOperation operation = new DriverOperation(Operation.Device.ADD, deviceId);
        restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
    }

    @Override
    public void notifyDriverDelDevice(Long deviceId, Long profileId) {
        Driver driver = getDriverByProfile(profileId);
        DriverOperation operation = new DriverOperation(Operation.Device.DELETE, deviceId);
        restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
    }

    @Override
    public void notifyDriverAddProfile(Long profileId) {
        Driver driver = getDriverByProfile(profileId);
        DriverOperation operation = new DriverOperation(Operation.Profile.ADD, profileId);
        restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
    }

    @Override
    public void notifyDriverDelProfile(Long profileId) {
        Driver driver = getDriverByProfile(profileId);
        DriverOperation operation = new DriverOperation(Operation.Profile.DELETE, profileId);
        restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
    }

    @Override
    public void notifyDriverUpdateDriverInfo(Long profileId) {
        Driver driver = getDriverByProfile(profileId);
        DriverOperation operation = new DriverOperation(Operation.Profile.UPDATE, profileId);
        restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
    }

    @Override
    public void notifyDriverUpdatePointInfo(Long deviceId) {
        Driver driver = getDriverByDevice(deviceId);
        DriverOperation operation = new DriverOperation(Operation.Device.UPDATE, deviceId);
        restTemplate.postForObject(String.format("http://%s/driver/memory", driver.getServiceName().toUpperCase()), operation, R.class);
    }

    public Driver getDriverByDevice(Long id) {
        Device device = deviceService.selectById(id);
        if (null != device) {
            Profile profile = profileService.selectById(device.getProfileId());
            if (null != profile) {
                return driverService.selectById(profile.getDriverId());
            }
        }
        return null;
    }

    public Driver getDriverByProfile(Long id) {
        Profile profile = profileService.selectById(id);
        if (null != profile) {
            return driverService.selectById(profile.getDriverId());
        }
        return null;
    }
}

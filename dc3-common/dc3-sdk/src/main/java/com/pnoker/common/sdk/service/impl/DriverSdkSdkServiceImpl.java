package com.pnoker.common.sdk.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.manager.feign.DeviceClient;
import com.pnoker.api.center.manager.feign.DriverClient;
import com.pnoker.api.center.manager.feign.ProfileClient;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.bean.R;
import com.pnoker.common.dto.DeviceDto;
import com.pnoker.common.dto.ProfileDto;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Driver;
import com.pnoker.common.model.Profile;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.DriverSdkService;
import com.pnoker.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverSdkSdkServiceImpl implements DriverSdkService {

    @Resource
    private DriverClient driverClient;
    @Resource
    private ProfileClient profileClient;
    @Resource
    private DeviceClient deviceClient;

    @Override
    public boolean initial(ApplicationContext context, DeviceDriver deviceDriver) {
        boolean register = register(deviceDriver);
        if (!register) {
            ((ConfigurableApplicationContext) context).close();
        }
        deviceDriver.setProfiles(profileList(deviceDriver.getId()));
        deviceDriver.setDevices(deviceList(deviceDriver.getProfiles()));
        return false;
    }

    @Override
    public boolean register(DeviceDriver deviceDriver) {
        if (!Dc3Util.isDriverPort(deviceDriver.getPort())) {
            log.error("invalid driver port,port range is 8600-8799");
            return false;
        }
        if (!Dc3Util.isName(deviceDriver.getName()) || !Dc3Util.isName(deviceDriver.getServiceName()) || !Dc3Util.isHost(deviceDriver.getHost())) {
            log.error("driver name || driver service name || driver host is invalid");
            return false;
        }
        Driver driver = new Driver();
        driver.setName(deviceDriver.getName())
                .setServiceName(deviceDriver.getServiceName())
                .setHost(deviceDriver.getHost())
                .setPort(deviceDriver.getPort())
                .setDescription(deviceDriver.getDescription());
        R<Driver> select = driverClient.selectByServiceName(driver.getServiceName());
        if (select.isOk()) {
            driver.setId(select.getData().getId());
            deviceDriver.setId(driver.getId());
            return driverClient.update(driver).isOk();
        } else {
            R<Driver> hostPort = driverClient.selectByHostPort(deviceDriver.getHost(), deviceDriver.getPort());
            if (!hostPort.isOk()) {
                R<Driver> add = driverClient.add(driver);
                if (add.isOk()) {
                    deviceDriver.setId(driver.getId());
                }
                return add.isOk();
            }
            log.error("the port({}) is already occupied by driver({}/{})", deviceDriver.getPort(), hostPort.getData().getName(), hostPort.getData().getServiceName());
            return false;
        }
    }

    public List<Profile> profileList(long driverId) {
        ProfileDto profileDto = new ProfileDto();
        profileDto.setDriverId(driverId);
        profileDto.setPage(new Pages().setSize(-1L));
        R<Page<Profile>> r = profileClient.list(profileDto);
        return r.isOk() ? r.getData().getRecords() : new ArrayList<>();
    }

    public List<Device> deviceList(List<Profile> profiles) {
        List<Device> devices = new ArrayList<>();
        for (Profile profile : profiles) {
            DeviceDto deviceDto = new DeviceDto();
            deviceDto.setProfileId(profile.getId());
            deviceDto.setPage(new Pages().setSize(-1L));
            R<Page<Device>> r = deviceClient.list(deviceDto);
            if (r.isOk()) {
                devices.addAll(r.getData().getRecords());
            }
        }
        return devices;
    }

}

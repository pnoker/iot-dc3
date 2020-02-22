package com.pnoker.common.sdk.service.impl;

import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.CustomizersService;
import com.pnoker.common.sdk.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {
    @Resource
    private DeviceDriver deviceDriver;
    @Resource
    private CustomizersService customizersService;

    @Override
    public void read(Long deviceId, Long pointId) {
        Map<String, AttributeInfo> driverInfoMap = deviceDriver.getDriverInfoMap().get(deviceDriver.getDeviceMap().get(deviceId).getProfileId());
        Map<String, AttributeInfo> pointInfoMap = deviceDriver.getPointInfoMap().get(deviceId).get(pointId);
        customizersService.read(driverInfoMap, pointInfoMap);
    }
}

package com.github.pnoker.common.sdk.service.impl;

import com.github.pnoker.common.bean.driver.PointValue;
import com.github.pnoker.common.model.Device;
import com.github.pnoker.common.sdk.bean.AttributeInfo;
import com.github.pnoker.common.sdk.bean.DriverContext;
import com.github.pnoker.common.sdk.service.DriverCommandService;
import com.github.pnoker.common.sdk.service.DriverService;
import com.github.pnoker.common.sdk.service.rabbit.PointValueService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverCommandServiceImpl implements DriverCommandService {
    @Resource
    private PointValueService pointValueService;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;

    @Override
    @SneakyThrows
    public PointValue read(Long deviceId, Long pointId) {
        Device device = driverContext.getDevice(deviceId);
        String rawValue = driverService.read(driverContext.getProfileDriverInfo(device.getProfileId()), driverContext.getDevicePointInfo(deviceId, pointId), device, driverContext.getDevicePoint(deviceId, pointId));
        PointValue pointValue = pointValueService.convertValue(deviceId, pointId, rawValue);
        pointValueService.pointValueSender(pointValue);
        return pointValue;
    }

    @Override
    @SneakyThrows
    public Boolean write(Long deviceId, Long pointId, String value) {
        Device device = driverContext.getDevice(deviceId);
        return driverService.write(driverContext.getProfileDriverInfo(device.getProfileId()), driverContext.getDevicePointInfo(deviceId, pointId),
                new AttributeInfo(value, driverContext.getDevicePoint(deviceId, pointId).getType()));
    }

}

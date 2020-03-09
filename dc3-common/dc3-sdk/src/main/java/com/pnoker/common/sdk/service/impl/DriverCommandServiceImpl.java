package com.pnoker.common.sdk.service.impl;

import com.pnoker.common.bean.driver.PointValue;
import com.pnoker.common.model.Device;
import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.bean.DriverContext;
import com.pnoker.common.sdk.service.DriverCommandService;
import com.pnoker.common.sdk.service.DriverService;
import com.pnoker.common.sdk.service.rabbit.PointValueService;
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
        String rawValue = driverService.read(driverContext.getDriverInfo(device), driverContext.getPointInfo(deviceId, pointId), device, driverContext.getPoint(device, pointId));
        PointValue pointValue = pointValueService.convertValue(deviceId, pointId, rawValue);
        pointValueService.pointValueSender(pointValue);
        return pointValue;
    }

    @Override
    @SneakyThrows
    public Boolean write(Long deviceId, Long pointId, String value) {
        Device device = driverContext.getDevice(deviceId);
        return driverService.write(driverContext.getDriverInfo(device), driverContext.getPointInfo(deviceId, pointId),
                new AttributeInfo(value, driverContext.getPoint(device, pointId).getType()));
    }

}

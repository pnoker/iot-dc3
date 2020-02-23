package com.pnoker.common.sdk.service.impl;

import com.pnoker.common.model.Device;
import com.pnoker.common.model.Point;
import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.CustomizersService;
import com.pnoker.common.sdk.service.DriverService;
import lombok.SneakyThrows;
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
    @SneakyThrows
    public String read(Long deviceId, Long pointId) {
        return customizersService.read(getDriverInfo(deviceId), getPointInfo(deviceId, pointId));
    }

    @Override
    @SneakyThrows
    public Boolean write(Long deviceId, Long pointId, String value) {
        return customizersService.write(getDriverInfo(deviceId), getPointInfo(deviceId, pointId),
                new AttributeInfo(value, getPoint(deviceId, pointId).getType()));
    }

    /**
     * 获取 驱动信息
     *
     * @param deviceId
     * @return
     */
    private Map<String, AttributeInfo> getDriverInfo(Long deviceId) {
        return deviceDriver.getDriverInfoMap().get(deviceDriver.getDeviceMap().get(deviceId).getProfileId());
    }

    /**
     * 获取位号信息
     *
     * @param deviceId
     * @param pointId
     * @return
     */
    private Map<String, AttributeInfo> getPointInfo(Long deviceId, Long pointId) {
        return deviceDriver.getPointInfoMap().get(deviceId).get(pointId);
    }

    /**
     * 获取设备
     *
     * @param deviceId
     * @return
     */
    private Device getDevice(Long deviceId) {
        return deviceDriver.getDeviceMap().get(deviceId);
    }

    /**
     * 获取位号
     *
     * @param deviceId
     * @param pointId
     * @return
     */
    private Point getPoint(Long deviceId, Long pointId) {
        return deviceDriver.getPointMap().get(getDevice(deviceId).getProfileId()).get(pointId);
    }
}

package com.pnoker.common.sdk.service.impl;

import com.pnoker.common.bean.driver.PointValue;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Point;
import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.bean.DriverContext;
import com.pnoker.common.sdk.service.DriverCommandService;
import com.pnoker.common.sdk.service.DriverService;
import com.pnoker.common.sdk.service.message.DriverMessageSender;
import com.pnoker.common.sdk.util.DriverUtils;
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
public class DriverCommandServiceImpl implements DriverCommandService {
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;
    @Resource
    private DriverMessageSender driverMessageSender;

    @Override
    @SneakyThrows
    public PointValue read(Long deviceId, Long pointId) {
        String rawValue = driverService.read(getDriverInfo(deviceId), getPointInfo(deviceId, pointId));
        Point point = getPoint(deviceId, pointId);
        PointValue pointValue = new PointValue(deviceId, pointId, rawValue, DriverUtils.processValue(rawValue, point));
        driverMessageSender.driverSender(pointValue);
        return pointValue;
    }

    @Override
    @SneakyThrows
    public Boolean write(Long deviceId, Long pointId, String value) {
        return driverService.write(getDriverInfo(deviceId), getPointInfo(deviceId, pointId),
                new AttributeInfo(value, getPoint(deviceId, pointId).getType()));
    }

    /**
     * 获取 驱动信息
     *
     * @param deviceId
     * @return
     */
    private Map<String, AttributeInfo> getDriverInfo(Long deviceId) {
        Device device = driverContext.getDeviceIdMap().get(deviceId);
        if (null == device) {
            throw new ServiceException("device does not exist");
        }
        Map<String, AttributeInfo> infoMap = driverContext.getDriverInfoMap().get(device.getProfileId());
        if (null == infoMap) {
            throw new ServiceException("device driver info does not exist");
        }
        return infoMap;
    }

    /**
     * 获取位号信息
     *
     * @param deviceId
     * @param pointId
     * @return
     */
    private Map<String, AttributeInfo> getPointInfo(Long deviceId, Long pointId) {
        Map<String, AttributeInfo> infoMap = driverContext.getPointInfoMap().get(deviceId).get(pointId);
        if (null == infoMap) {
            throw new ServiceException("device point info does not exist");
        }
        return infoMap;
    }

    /**
     * 获取设备
     *
     * @param deviceId
     * @return
     */
    private Device getDevice(Long deviceId) {
        return driverContext.getDeviceIdMap().get(deviceId);
    }

    /**
     * 获取位号
     *
     * @param deviceId
     * @param pointId
     * @return
     */
    private Point getPoint(Long deviceId, Long pointId) {
        return driverContext.getPointMap().get(getDevice(deviceId).getProfileId()).get(pointId);
    }
}

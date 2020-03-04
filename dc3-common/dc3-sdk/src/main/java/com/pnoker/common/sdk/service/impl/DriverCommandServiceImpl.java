package com.pnoker.common.sdk.service.impl;

import cn.hutool.core.convert.Convert;
import com.pnoker.common.bean.driver.PointValue;
import com.pnoker.common.constant.Common;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Point;
import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.bean.DriverContext;
import com.pnoker.common.sdk.service.DriverCommandService;
import com.pnoker.common.sdk.service.DriverService;
import com.pnoker.common.sdk.service.rabbit.PointValueService;
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
    private PointValueService pointValueService;
    @Resource
    private DriverContext driverContext;
    @Resource
    private DriverService driverService;

    @Override
    @SneakyThrows
    public PointValue read(Long deviceId, Long pointId) {
        Device device = getDevice(deviceId);
        String rawValue = driverService.read(getDriverInfo(device), getPointInfo(deviceId, pointId), device, getPoint(device, pointId));
        PointValue pointValue = new PointValue(deviceId, pointId, rawValue, processValue(rawValue, getPoint(device, pointId)));
        pointValueService.pointValueSender(pointValue);
        return pointValue;
    }

    @Override
    @SneakyThrows
    public Boolean write(Long deviceId, Long pointId, String value) {
        Device device = getDevice(deviceId);
        return driverService.write(getDriverInfo(device), getPointInfo(deviceId, pointId),
                new AttributeInfo(value, getPoint(device, pointId).getType()));
    }

    /**
     * 获取设备
     *
     * @param deviceId
     * @return
     */
    private Device getDevice(Long deviceId) {
        Device device = driverContext.getDeviceIdMap().get(deviceId);
        if (null == device) {
            throw new ServiceException("device does not exist");
        }
        return device;
    }

    /**
     * 获取 驱动信息
     *
     * @param device
     * @return
     */
    private Map<String, AttributeInfo> getDriverInfo(Device device) {
        Map<String, AttributeInfo> infoMap = driverContext.getDriverInfoMap().get(device.getProfileId());
        if (null == infoMap) {
            throw new ServiceException("device profile driver info does not exist");
        }
        return infoMap;
    }

    /**
     * 获取位号
     *
     * @param device
     * @param pointId
     * @return
     */
    private Point getPoint(Device device, Long pointId) {
        Map<Long, Point> pointMap = driverContext.getPointMap().get(device.getProfileId());
        if (null == pointMap) {
            throw new ServiceException("device profile does not exist");
        }
        Point point = pointMap.get(pointId);
        if (null == point) {
            throw new ServiceException("device point does not exist");
        }
        return point;
    }

    /**
     * 获取位号信息
     *
     * @param deviceId
     * @param pointId
     * @return
     */
    private Map<String, AttributeInfo> getPointInfo(Long deviceId, Long pointId) {
        Map<Long, Map<String, AttributeInfo>> tmpMap = driverContext.getPointInfoMap().get(deviceId);
        if (null == tmpMap) {
            throw new ServiceException("device point info does not exist");
        }
        Map<String, AttributeInfo> infoMap = tmpMap.get(pointId);
        if (null == infoMap) {
            throw new ServiceException("device point info does not exist");
        }
        return infoMap;
    }


    /**
     * 处理数值
     *
     * @param value
     * @param point point.type : string/int/double/float/long/boolean
     * @return
     */
    public String processValue(String value, Point point) {
        value = value.trim();
        switch (point.getType()) {
            case Common.ValueType.STRING:
                break;
            case Common.ValueType.INT:
            case Common.ValueType.LONG:
                try {
                    value = String.format("%.0f",
                            (Convert.convert(Double.class, value) + point.getBase()) * point.getMultiple());
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
                break;
            case Common.ValueType.DOUBLE:
            case Common.ValueType.FLOAT:
                try {
                    value = String.format(point.getFormat(),
                            (Convert.convert(Double.class, value) + point.getBase()) * point.getMultiple());
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
                break;
            case Common.ValueType.BOOLEAN:
                try {
                    value = String.valueOf(Boolean.parseBoolean(value));
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
                break;
            default:
                throw new ServiceException("invalid device point value type");
        }
        return value;
    }
}

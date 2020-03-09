package com.pnoker.common.sdk.bean;

import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Point;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Getter
@Component
public class DriverContext {

    private volatile long driverId;

    /**
     * profileId(driverAttribute.name,(drverInfo.value,driverAttribute.type))
     */
    private volatile Map<Long, Map<String, AttributeInfo>> driverInfoMap;

    /**
     * deviceId,device
     */
    private volatile Map<Long, Device> deviceIdMap;

    /**
     * deviceCode,deviceId
     */
    private volatile Map<String, Long> deviceCodeMap;

    /**
     * deviceName,deviceId
     */
    private volatile Map<String, Long> deviceNameMap;

    /**
     * profileId,(pointId,point)
     */
    private volatile Map<Long, Map<Long, Point>> pointMap;

    /**
     * deviceId(pointId(pointAttribute.name,(pointInfo.value,pointAttribute.type)))
     */
    private volatile Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap;

    /**
     * 获取设备
     *
     * @param deviceId
     * @return
     */
    public Device getDevice(Long deviceId) {
        Device device = deviceIdMap.get(deviceId);
        if (null == device) {
            throw new ServiceException("device does not exist");
        }
        return device;
    }

    /**
     * 通过设备 Code 获取设备
     *
     * @param deviceCode
     * @return
     */
    public Device getDeviceByCode(String deviceCode) {
        Device device = deviceIdMap.get(deviceCodeMap.get(deviceCode));
        if (null == device) {
            throw new ServiceException("device does not exist");
        }
        return device;
    }

    /**
     * 通过设备 Name 获取设备
     *
     * @param deviceName
     * @return
     */
    public Device getDeviceByName(String deviceName) {
        Device device = deviceIdMap.get(deviceNameMap.get(deviceName));
        if (null == device) {
            throw new ServiceException("device does not exist");
        }
        return device;
    }

    /**
     * 获取位号
     *
     * @param device
     * @param pointId
     * @return
     */
    public Point getPoint(Device device, Long pointId) {
        Map<Long, Point> map = pointMap.get(device.getProfileId());
        if (null == map) {
            throw new ServiceException("device profile does not exist");
        }
        Point point = map.get(pointId);
        if (null == point) {
            throw new ServiceException("device point does not exist");
        }
        return point;
    }

    /**
     * 获取 驱动信息
     *
     * @param device
     * @return
     */
    public Map<String, AttributeInfo> getDriverInfo(Device device) {
        Map<String, AttributeInfo> infoMap = driverInfoMap.get(device.getProfileId());
        if (null == infoMap) {
            throw new ServiceException("device profile driver info does not exist");
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
    public Map<String, AttributeInfo> getPointInfo(Long deviceId, Long pointId) {
        Map<Long, Map<String, AttributeInfo>> tmpMap = pointInfoMap.get(deviceId);
        if (null == tmpMap) {
            throw new ServiceException("device point info does not exist");
        }
        Map<String, AttributeInfo> infoMap = tmpMap.get(pointId);
        if (null == infoMap) {
            throw new ServiceException("device point info does not exist");
        }
        return infoMap;
    }

    public synchronized void setDriverId(long driverId) {
        this.driverId = driverId;
    }

    public synchronized void setDriverInfoMap(Map<Long, Map<String, AttributeInfo>> driverInfoMap) {
        this.driverInfoMap = driverInfoMap;
    }

    public synchronized void setDeviceIdMap(Map<Long, Device> deviceIdMap) {
        this.deviceIdMap = deviceIdMap;
    }

    public synchronized void setDeviceCodeMap(Map<String, Long> deviceCodeMap) {
        this.deviceCodeMap = deviceCodeMap;
    }

    public synchronized void setDeviceNameMap(Map<String, Long> deviceNameMap) {
        this.deviceNameMap = deviceNameMap;
    }

    public synchronized void setPointMap(Map<Long, Map<Long, Point>> pointMap) {
        this.pointMap = pointMap;
    }

    public synchronized void setPointInfoMap(Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap) {
        this.pointInfoMap = pointInfoMap;
    }
}

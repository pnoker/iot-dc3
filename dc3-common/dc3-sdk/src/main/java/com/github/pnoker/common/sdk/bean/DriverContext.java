package com.github.pnoker.common.sdk.bean;

import com.github.pnoker.common.exception.ServiceException;
import com.github.pnoker.common.model.Device;
import com.github.pnoker.common.model.Point;
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
    private volatile Map<Long, Device> deviceMap;

    /**
     * deviceName,deviceId
     */
    private volatile Map<String, Long> deviceNameMap;

    /**
     * profileId,(pointId,point)
     */
    private volatile Map<Long, Map<Long, Point>> profilePointMap;

    /**
     * deviceId(pointId(pointAttribute.name,(pointInfo.value,pointAttribute.type)))
     */
    private volatile Map<Long, Map<Long, Map<String, AttributeInfo>>> devicePointInfoMap;

    /**
     * deviceId(pointName,pointId)
     */
    private volatile Map<Long, Map<String, Long>> devicePointNameMap;

    /**
     * 获取设备
     *
     * @param deviceId
     * @return
     */
    public Device getDevice(Long deviceId) {
        Device device = deviceMap.get(deviceId);
        if (null == device) {
            throw new ServiceException("device(" + deviceId + ") does not exist");
        }
        return device;
    }

    /**
     * 通过 Device Name 获取设备 ID
     *
     * @param deviceName
     * @return
     */
    public Long getDeviceIdByName(String deviceName) {
        Long deviceId = deviceNameMap.get(deviceName);
        if (null == deviceId) {
            throw new ServiceException("device(" + deviceId + ") does not exist");
        }
        return deviceId;
    }

    /**
     * 获取设备位号
     *
     * @param deviceId
     * @param pointId
     * @return
     */
    public Point getDevicePoint(Long deviceId, Long pointId) {
        Map<Long, Point> map = profilePointMap.get(getDevice(deviceId).getProfileId());
        if (null == map) {
            throw new ServiceException("device(" + deviceId + ") profile does not exist");
        }
        Point point = map.get(pointId);
        if (null == point) {
            throw new ServiceException("device(" + pointId + ") point does not exist");
        }
        return point;
    }

    /**
     * 通过 Device ID & Point Name 获取位号 ID
     *
     * @param deviceId
     * @param pointName
     * @return
     */
    public Long getDevicePointIdByName(Long deviceId, String pointName) {
        Map<String, Long> map = devicePointNameMap.get(deviceId);
        if (null == map) {
            throw new ServiceException("device(" + deviceId + ") does not exist");
        }
        Long pointId = map.get(pointName);
        if (null == pointId) {
            throw new ServiceException("point(" + pointId + ") does not exist");
        }
        return pointId;
    }

    /**
     * 获取 驱动信息
     *
     * @param profileId
     * @return
     */
    public Map<String, AttributeInfo> getProfileDriverInfo(Long profileId) {
        Map<String, AttributeInfo> infoMap = driverInfoMap.get(profileId);
        if (null == infoMap) {
            throw new ServiceException("profile(" + profileId + ") driver info does not exist");
        }
        return infoMap;
    }

    /**
     * 通过 Device Id & Point Id 获取设备位号配置信息
     *
     * @param deviceId
     * @param pointId
     * @return
     */
    public Map<String, AttributeInfo> getDevicePointInfo(Long deviceId, Long pointId) {
        Map<Long, Map<String, AttributeInfo>> tmpMap = devicePointInfoMap.get(deviceId);
        if (null == tmpMap) {
            throw new ServiceException("device(" + deviceId + ") does not exist");
        }
        Map<String, AttributeInfo> infoMap = tmpMap.get(pointId);
        if (null == infoMap) {
            throw new ServiceException("point(" + pointId + ") info does not exist");
        }
        return infoMap;
    }

    public synchronized void setDriverId(long driverId) {
        this.driverId = driverId;
    }

    public synchronized void setDriverInfoMap(Map<Long, Map<String, AttributeInfo>> driverInfoMap) {
        this.driverInfoMap = driverInfoMap;
    }

    public synchronized void setDeviceMap(Map<Long, Device> deviceMap) {
        this.deviceMap = deviceMap;
    }

    public synchronized void setDeviceNameMap(Map<String, Long> deviceNameMap) {
        this.deviceNameMap = deviceNameMap;
    }

    public synchronized void setProfilePointMap(Map<Long, Map<Long, Point>> profilePointMap) {
        this.profilePointMap = profilePointMap;
    }

    public synchronized void setDevicePointInfoMap(Map<Long, Map<Long, Map<String, AttributeInfo>>> devicePointInfoMap) {
        this.devicePointInfoMap = devicePointInfoMap;
    }

    public synchronized void setDevicePointNameMap(Map<Long, Map<String, Long>> devicePointNameMap) {
        this.devicePointNameMap = devicePointNameMap;
    }
}

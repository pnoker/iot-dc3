package com.pnoker.common.sdk.bean;

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
     * profileId,(pointId,point)
     */
    private volatile Map<Long, Map<Long, Point>> pointMap;

    /**
     * deviceId(pointId(pointAttribute.name,(pointInfo.value,pointAttribute.type)))
     */
    private volatile Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap;

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

    public synchronized void setPointMap(Map<Long, Map<Long, Point>> pointMap) {
        this.pointMap = pointMap;
    }

    public synchronized void setPointInfoMap(Map<Long, Map<Long, Map<String, AttributeInfo>>> pointInfoMap) {
        this.pointInfoMap = pointInfoMap;
    }
}

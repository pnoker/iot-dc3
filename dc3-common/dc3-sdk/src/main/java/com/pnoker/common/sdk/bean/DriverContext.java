package com.pnoker.common.sdk.bean;

import com.pnoker.common.model.Device;
import com.pnoker.common.model.Point;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Setter
@Getter
@Component
public class DriverContext {

    private long driverId;

    /**
     * profileId(driverAttribute.name,(drverInfo.value,driverAttribute.type))
     */
    private volatile Map<Long, Map<String, AttributeInfo>> driverInfoMap;

    /**
     * deviceId,device
     */
    private volatile Map<Long, Device> deviceIdMap;

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

}

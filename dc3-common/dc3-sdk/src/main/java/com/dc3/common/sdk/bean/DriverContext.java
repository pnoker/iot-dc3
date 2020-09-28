/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.sdk.bean;

import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author pnoker
 */
@Data
@Slf4j
@Component
public class DriverContext {

    private volatile long driverId;

    /**
     * profileId(driverAttribute.name,(drverInfo.value,driverAttribute.type))
     */
    private Map<Long, Map<String, AttributeInfo>> driverInfoMap = new ConcurrentHashMap<>(16);

    /**
     * deviceId,device
     */
    private Map<Long, Device> deviceMap = new ConcurrentHashMap<>(16);

    /**
     * deviceName,deviceId
     */
    private Map<String, Long> deviceNameMap = new ConcurrentHashMap<>(16);

    /**
     * profileId,(pointId,point)
     */
    private Map<Long, Map<Long, Point>> profilePointMap = new ConcurrentHashMap<>(16);

    /**
     * deviceId(pointId(pointAttribute.name,(pointInfo.value,pointAttribute.type)))
     */
    private Map<Long, Map<Long, Map<String, AttributeInfo>>> devicePointInfoMap = new ConcurrentHashMap<>(16);

    /**
     * deviceId(pointName,pointId)
     */
    private Map<Long, Map<String, Long>> devicePointNameMap = new ConcurrentHashMap<>(16);

    /**
     * 获取设备
     *
     * @param deviceId Device ID
     * @return Device
     */
    public Device getDevice(Long deviceId) {
        Device device = deviceMap.get(deviceId);
        if (null == device) {
            throw new ServiceException("Device(" + deviceId + ") does not exist");
        }
        return device;
    }

    /**
     * 通过 Device Name 获取设备 ID
     *
     * @param deviceName Device Name
     * @return Device ID
     */
    public Long getDeviceIdByName(String deviceName) {
        Long deviceId = deviceNameMap.get(deviceName);
        if (null == deviceId) {
            throw new ServiceException("Device(" + deviceName + ") does not exist");
        }
        return deviceId;
    }

    /**
     * 获取设备位号
     *
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @return Point
     */
    public Point getDevicePoint(Long deviceId, Long pointId) {
        Map<Long, Point> map = profilePointMap.get(getDevice(deviceId).getProfileId());
        if (null == map) {
            throw new ServiceException("Device(" + deviceId + ") profile does not exist");
        }
        Point point = map.get(pointId);
        if (null == point) {
            throw new ServiceException("Point(" + pointId + ") point does not exist");
        }
        return point;
    }

    /**
     * 通过 Device ID & Point Name 获取位号 ID
     *
     * @param deviceId  Device ID
     * @param pointName Point Name
     * @return Device Point ID
     */
    public Long getDevicePointIdByName(Long deviceId, String pointName) {
        Map<String, Long> map = devicePointNameMap.get(deviceId);
        if (null == map) {
            throw new ServiceException("Device(" + deviceId + ") does not exist");
        }
        Long pointId = map.get(pointName);
        if (null == pointId) {
            throw new ServiceException("Point(" + pointName + ") does not exist");
        }
        return pointId;
    }

    /**
     * 获取 驱动信息
     *
     * @param profileId Profile ID
     * @return Map<String, AttributeInfo>
     */
    public Map<String, AttributeInfo> getProfileDriverInfo(Long profileId) {
        return driverInfoMap.get(profileId);
    }

    /**
     * 通过 Device Id & Point Id 获取设备位号配置信息
     *
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @return Map<String, AttributeInfo>
     */
    public Map<String, AttributeInfo> getDevicePointInfo(Long deviceId, Long pointId) {
        Map<Long, Map<String, AttributeInfo>> tmpMap = devicePointInfoMap.get(deviceId);
        if (null == tmpMap) {
            throw new ServiceException("Device(" + deviceId + ") does not exist");
        }
        Map<String, AttributeInfo> infoMap = tmpMap.get(pointId);
        if (null == infoMap) {
            throw new ServiceException("Point(" + pointId + ") info does not exist");
        }
        return infoMap;
    }
}

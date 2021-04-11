/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.sdk.bean;

import com.dc3.common.bean.driver.AttributeInfo;
import com.dc3.common.bean.driver.DriverMetadata;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author pnoker
 */
@Data
@Slf4j
@Component
public class DriverContext {

    /**
     * 驱动 元数据，当且仅当驱动注册成功之后由 Manager 返回
     */
    private DriverMetadata driverMetadata = new DriverMetadata();

    /**
     * 驱动 状态，默认为 未注册 状态
     */
    private String driverStatus = Common.Driver.Status.UNREGISTERED;

    public synchronized void setDriverStatus(String driverStatus) {
        this.driverStatus = driverStatus;
    }

    /**
     * 通过模版 ID 获取驱动配置信息
     *
     * @param profileId Profile ID
     * @return Map<String, AttributeInfo>
     */
    public Map<String, AttributeInfo> getProfileDriverInfoByProfileId(Long profileId) {
        return this.driverMetadata.getProfileDriverInfoMap().get(profileId);
    }

    /**
     * 通过 Device Name 获取设备 ID
     *
     * @param deviceName Device Name
     * @return Device ID
     */
    public Long getDeviceIdByDeviceName(String deviceName) {
        Long deviceId = driverMetadata.getDeviceNameMap().get(deviceName);
        if (null == deviceId) {
            throw new NotFoundException("Device(" + deviceName + ") does not exist");
        }
        return deviceId;
    }

    /**
     * 通过设备 ID 获取设备
     *
     * @param deviceId Device ID
     * @return Device
     */
    public Device getDeviceByDeviceId(Long deviceId) {
        Device device = this.driverMetadata.getDeviceMap().get(deviceId);
        if (null == device) {
            throw new NotFoundException("Device(" + deviceId + ") does not exist");
        }
        return device;
    }

    /**
     * 通过 Device ID & Point ID 获取位号
     *
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @return Point
     */
    public Point getDevicePointByDeviceIdAndPointId(Long deviceId, Long pointId) {
        Map<Long, Point> map = driverMetadata.getProfilePointMap().get(getDeviceByDeviceId(deviceId).getProfileId());
        if (null == map) {
            throw new NotFoundException("Device(" + deviceId + ") profile does not exist");
        }
        Point point = map.get(pointId);
        if (null == point) {
            throw new NotFoundException("Point(" + pointId + ") point does not exist");
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
    public Long getDevicePointIdByDeviceIdAndPointName(Long deviceId, String pointName) {
        Map<String, Long> map = driverMetadata.getDevicePointNameMap().get(deviceId);
        if (null == map) {
            throw new NotFoundException("Device(" + deviceId + ") does not exist");
        }
        Long pointId = map.get(pointName);
        if (null == pointId) {
            throw new NotFoundException("Point(" + pointName + ") does not exist");
        }
        return pointId;
    }

    /**
     * 通过 Device Id & Point Id 获取位号配置信息
     *
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @return Map<String, AttributeInfo>
     */
    public Map<String, AttributeInfo> getDevicePointInfoByDeviceIdAndPointId(Long deviceId, Long pointId) {
        Map<Long, Map<String, AttributeInfo>> tmpMap = driverMetadata.getDevicePointInfoMap().get(deviceId);
        if (null == tmpMap) {
            throw new NotFoundException("Device(" + deviceId + ") does not exist");
        }
        Map<String, AttributeInfo> infoMap = tmpMap.get(pointId);
        if (null == infoMap) {
            throw new NotFoundException("Point(" + pointId + ") info does not exist");
        }
        return infoMap;
    }
}

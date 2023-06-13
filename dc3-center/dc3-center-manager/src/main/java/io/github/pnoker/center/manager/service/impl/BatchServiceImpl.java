/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service.impl;

import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.entity.base.Base;
import io.github.pnoker.common.entity.driver.AttributeInfo;
import io.github.pnoker.common.entity.driver.DriverMetadata;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * BatchService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class BatchServiceImpl implements BatchService {

    @Resource
    private DriverService driverService;
    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverAttributeConfigService driverAttributeConfigService;
    @Resource
    private ProfileBindService profileBindService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private PointService pointService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointAttributeConfigService pointAttributeConfigService;

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverMetadata batchDriverMetadata(String serviceName, String tenantId) {
        DriverMetadata driverMetadata = new DriverMetadata();
        DriverDO entityDO = driverService.selectByServiceName(serviceName, tenantId, true);
        driverMetadata.setDriverId(entityDO.getId());
        driverMetadata.setTenantId(entityDO.getTenantId());

        try {
            Map<String, DriverAttribute> driverAttributeMap = getDriverAttributeMap(entityDO.getId());
            driverMetadata.setDriverAttributeMap(driverAttributeMap);

            Map<String, PointAttribute> pointAttributeMap = getPointAttributeMap(entityDO.getId());
            driverMetadata.setPointAttributeMap(pointAttributeMap);

            List<Device> devices = deviceService.selectByDriverId(entityDO.getId());
            Set<String> deviceIds = devices.stream().map(Base::getId).collect(Collectors.toSet());

            Map<String, Map<String, AttributeInfo>> driverInfoMap = getDriverInfoMap(deviceIds, driverAttributeMap);
            driverMetadata.setDriverInfoMap(driverInfoMap);

            Map<String, Device> deviceMap = getDeviceMap(devices);
            driverMetadata.setDeviceMap(deviceMap);

            Map<String, Map<String, Point>> profilePointMap = getProfilePointMap(deviceIds);
            driverMetadata.setProfilePointMap(profilePointMap);

            Map<String, Map<String, Map<String, AttributeInfo>>> devicePointInfoMap = getPointInfoMap(devices, profilePointMap, pointAttributeMap);
            driverMetadata.setPointInfoMap(devicePointInfoMap);

            return driverMetadata;
        } catch (NotFoundException ignored) {
            // nothing to do
        }

        return driverMetadata;
    }

    /**
     * Get driver attribute map
     *
     * @param driverId Driver ID
     * @return map(driverAttributeId, driverAttribute)
     */
    public Map<String, DriverAttribute> getDriverAttributeMap(String driverId) {
        Map<String, DriverAttribute> driverAttributeMap = new ConcurrentHashMap<>(16);
        try {
            List<DriverAttribute> driverAttributes = driverAttributeService.selectByDriverId(driverId, true);
            driverAttributes.forEach(driverAttribute -> driverAttributeMap.put(driverAttribute.getId(), driverAttribute));
        } catch (NotFoundException ignored) {
            // nothing to do
        }
        return driverAttributeMap;
    }

    /**
     * Get point attribute map
     *
     * @param driverId Driver ID
     * @return map(pointAttributeId, pointAttribute)
     */
    public Map<String, PointAttribute> getPointAttributeMap(String driverId) {
        Map<String, PointAttribute> pointAttributeMap = new ConcurrentHashMap<>(16);
        try {
            List<PointAttribute> pointAttributes = pointAttributeService.selectByDriverId(driverId, true);
            pointAttributes.forEach(pointAttribute -> pointAttributeMap.put(pointAttribute.getId(), pointAttribute));
        } catch (NotFoundException ignored) {
            // nothing to do
        }
        return pointAttributeMap;
    }

    /**
     * Get driver attribute config map
     *
     * @param deviceList         Device Set
     * @param driverAttributeMap Driver Attribute Map
     * @return map(deviceId ( driverAttribute.name, ( drverInfo.value, driverAttribute.type)))
     */
    public Map<String, Map<String, AttributeInfo>> getDriverInfoMap(Set<String> deviceList, Map<String, DriverAttribute> driverAttributeMap) {
        Map<String, Map<String, AttributeInfo>> driverInfoMap = new ConcurrentHashMap<>(16);
        deviceList.forEach(deviceId -> {
            Map<String, AttributeInfo> infoMap = getDriverInfoMap(deviceId, driverAttributeMap);
            if (infoMap.size() > 0) {
                driverInfoMap.put(deviceId, infoMap);
            }
        });
        return driverInfoMap;
    }

    /**
     * Get driver attribute config map
     *
     * @param deviceId           设备ID
     * @param driverAttributeMap Driver Attribute Map
     * @return map(attributeName, attributeInfo ( value, type))
     */
    public Map<String, AttributeInfo> getDriverInfoMap(String deviceId, Map<String, DriverAttribute> driverAttributeMap) {
        Map<String, AttributeInfo> attributeInfoMap = new ConcurrentHashMap<>(16);
        try {
            List<DriverAttributeConfig> driverAttributeConfigs = driverAttributeConfigService.selectByDeviceId(deviceId);
            driverAttributeConfigs.forEach(driverInfo -> {
                DriverAttribute attribute = driverAttributeMap.get(driverInfo.getDriverAttributeId());
                attributeInfoMap.put(attribute.getAttributeName(), new AttributeInfo(driverInfo.getConfigValue(), attribute.getAttributeTypeFlag()));
            });
        } catch (NotFoundException ignored) {
            // nothing to do
        }
        return attributeInfoMap;
    }

    /**
     * Get point attribute config map
     *
     * @param devices           Device Array
     * @param profilePointMap   Profile Point Map
     * @param pointAttributeMap Point Attribute Map
     * @return map(deviceId ( pointId, attribute ( attributeName, attributeInfo ( value, type))))
     */
    public Map<String, Map<String, Map<String, AttributeInfo>>> getPointInfoMap(List<Device> devices, Map<String, Map<String, Point>> profilePointMap, Map<String, PointAttribute> pointAttributeMap) {
        Map<String, Map<String, Map<String, AttributeInfo>>> devicePointInfoMap = new ConcurrentHashMap<>(16);
        devices.forEach(device -> {
            Map<String, Map<String, AttributeInfo>> infoMap = getPointInfoMap(device, profilePointMap, pointAttributeMap);
            if (infoMap.size() > 0) {
                devicePointInfoMap.put(device.getId(), infoMap);
            }
        });
        return devicePointInfoMap;
    }

    /**
     * Get point attribute config map
     *
     * @param device            Device
     * @param profilePointMap   Profile Point Map
     * @param pointAttributeMap Point Attribute Map
     * @return map(pointId, attribute ( attributeName, attributeInfo ( value, type)))
     */
    public Map<String, Map<String, AttributeInfo>> getPointInfoMap(Device device, Map<String, Map<String, Point>> profilePointMap, Map<String, PointAttribute> pointAttributeMap) {
        Map<String, Map<String, AttributeInfo>> attributeInfoMap = new ConcurrentHashMap<>(16);
        device.getProfileIds().forEach(profileId -> profilePointMap.get(profileId).keySet()
                .forEach(pointId -> {
                    try {
                        List<PointAttributeConfig> pointAttributeConfigs = pointAttributeConfigService.selectByDeviceIdAndPointId(device.getId(), pointId);
                        Map<String, AttributeInfo> infoMap = new ConcurrentHashMap<>(16);
                        pointAttributeConfigs.forEach(pointInfo -> {
                            PointAttribute attribute = pointAttributeMap.get(pointInfo.getPointAttributeId());
                            infoMap.put(attribute.getAttributeName(), new AttributeInfo(pointInfo.getConfigValue(), attribute.getAttributeTypeFlag()));
                        });

                        if (infoMap.size() > 0) {
                            attributeInfoMap.put(pointId, infoMap);
                        }
                    } catch (NotFoundException ignored) {
                        // nothing to do
                    }
                }));
        return attributeInfoMap;
    }

    /**
     * Get device map
     *
     * @param devices Device Array
     * @return map(pointId, point)
     */
    public Map<String, Device> getDeviceMap(List<Device> devices) {
        Map<String, Device> deviceMap = new ConcurrentHashMap<>(16);
        devices.forEach(device -> deviceMap.put(device.getId(), device));
        return deviceMap;
    }

    /**
     * Get profile  map
     *
     * @param deviceIds 设备ID Set
     * @return map(profileId ( pointId, point))
     */
    public Map<String, Map<String, Point>> getProfilePointMap(Set<String> deviceIds) {
        Map<String, Map<String, Point>> profilePointMap = new ConcurrentHashMap<>(16);
        deviceIds.forEach(deviceId -> {
            Set<String> profileIds = profileBindService.selectProfileIdsByDeviceId(deviceId);
            profileIds.forEach(profileId -> profilePointMap.put(profileId, getPointMap(profileId)));
        });
        return profilePointMap;
    }

    /**
     * Get point map
     *
     * @param profileId Profile ID
     * @return map(pointId, point)
     */
    public Map<String, Point> getPointMap(String profileId) {
        Map<String, Point> pointMap = new ConcurrentHashMap<>(16);
        try {
            pointService.selectByProfileId(profileId).forEach(point -> pointMap.put(point.getId(), point));
        } catch (NotFoundException ignored) {
            // nothing to do
        }
        return pointMap;
    }

}

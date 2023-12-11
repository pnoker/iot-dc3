/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

import io.github.pnoker.center.manager.entity.bo.*;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.base.Base;
import io.github.pnoker.common.entity.dto.AttributeInfoDTO;
import io.github.pnoker.common.entity.dto.DriverMetadataDTO;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.center.manager.entity.bo.PointBO;
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
    public DriverMetadataDTO batchDriverMetadata(String serviceName, Long tenantId) {
        DriverMetadataDTO driverMetadataDTO = new DriverMetadataDTO();
        DriverBO entityDO = driverService.selectByServiceName(serviceName, tenantId, true);
        driverMetadataDTO.setDriverId(entityDO.getId());
        driverMetadataDTO.setTenantId(entityDO.getTenantId());

        try {
            Map<Long, DriverAttributeBO> driverAttributeMap = getDriverAttributeMap(entityDO.getId());
            driverMetadataDTO.setDriverAttributeMap(driverAttributeMap);

            Map<Long, PointAttributeBO> pointAttributeMap = getPointAttributeMap(entityDO.getId());
            driverMetadataDTO.setPointAttributeMap(pointAttributeMap);

            List<DeviceBO> deviceBOS = deviceService.selectByDriverId(entityDO.getId());
            Set<Long> deviceIds = deviceBOS.stream().map(Base::getId).collect(Collectors.toSet());

            Map<Long, Map<String, AttributeInfoDTO>> driverInfoMap = getDriverInfoMap(deviceIds, driverAttributeMap);
            driverMetadataDTO.setDriverInfoMap(driverInfoMap);

            Map<Long, DeviceBO> deviceMap = getDeviceMap(deviceBOS);
            driverMetadataDTO.setDeviceMap(deviceMap);

            Map<Long, Map<Long, PointBO>> profilePointMap = getProfilePointMap(deviceIds);
            driverMetadataDTO.setProfilePointMap(profilePointMap);

            Map<Long, Map<Long, Map<String, AttributeInfoDTO>>> devicePointInfoMap = getPointInfoMap(deviceBOS, profilePointMap, pointAttributeMap);
            driverMetadataDTO.setPointInfoMap(devicePointInfoMap);

            return driverMetadataDTO;
        } catch (NotFoundException ignored) {
            // nothing to do
        }

        return driverMetadataDTO;
    }

    /**
     * Get driver attribute map
     *
     * @param driverId Driver ID
     * @return map(driverAttributeId, driverAttribute)
     */
    public Map<Long, DriverAttributeBO> getDriverAttributeMap(Long driverId) {
        Map<Long, DriverAttributeBO> driverAttributeMap = new ConcurrentHashMap<>(16);
        try {
            List<DriverAttributeBO> driverAttributeBOS = driverAttributeService.selectByDriverId(driverId);
            driverAttributeBOS.forEach(driverAttribute -> driverAttributeMap.put(driverAttribute.getId(), driverAttribute));
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
    public Map<Long, PointAttributeBO> getPointAttributeMap(Long driverId) {
        Map<Long, PointAttributeBO> pointAttributeMap = new ConcurrentHashMap<>(16);
        try {
            List<PointAttributeBO> pointAttributeBOS = pointAttributeService.selectByDriverId(driverId, true);
            pointAttributeBOS.forEach(pointAttribute -> pointAttributeMap.put(pointAttribute.getId(), pointAttribute));
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
    public Map<Long, Map<String, AttributeInfoDTO>> getDriverInfoMap(Set<Long> deviceList, Map<Long, DriverAttributeBO> driverAttributeMap) {
        Map<Long, Map<String, AttributeInfoDTO>> driverInfoMap = new ConcurrentHashMap<>(16);
        deviceList.forEach(deviceId -> {
            Map<String, AttributeInfoDTO> infoMap = getDriverInfoMap(deviceId, driverAttributeMap);
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
    public Map<String, AttributeInfoDTO> getDriverInfoMap(Long deviceId, Map<Long, DriverAttributeBO> driverAttributeMap) {
        Map<String, AttributeInfoDTO> attributeInfoMap = new ConcurrentHashMap<>(16);
        try {
            List<DriverAttributeConfigBO> driverAttributeConfigBOS = driverAttributeConfigService.selectByDeviceId(deviceId);
            driverAttributeConfigBOS.forEach(driverInfo -> {
                DriverAttributeBO attribute = driverAttributeMap.get(driverInfo.getDriverAttributeId());
                attributeInfoMap.put(attribute.getAttributeName(), new AttributeInfoDTO(driverInfo.getConfigValue(), attribute.getAttributeTypeFlag()));
            });
        } catch (NotFoundException ignored) {
            // nothing to do
        }
        return attributeInfoMap;
    }

    /**
     * Get point attribute config map
     *
     * @param deviceBOS         Device Array
     * @param profilePointMap   Profile Point Map
     * @param pointAttributeMap Point Attribute Map
     * @return map(deviceId ( pointId, attribute ( attributeName, attributeInfo ( value, type))))
     */
    public Map<Long, Map<Long, Map<String, AttributeInfoDTO>>> getPointInfoMap(List<DeviceBO> deviceBOS, Map<Long, Map<Long, PointBO>> profilePointMap, Map<Long, PointAttributeBO> pointAttributeMap) {
        Map<Long, Map<Long, Map<String, AttributeInfoDTO>>> devicePointInfoMap = new ConcurrentHashMap<>(16);
        deviceBOS.forEach(device -> {
            Map<Long, Map<String, AttributeInfoDTO>> infoMap = getPointInfoMap(device, profilePointMap, pointAttributeMap);
            if (infoMap.size() > 0) {
                devicePointInfoMap.put(device.getId(), infoMap);
            }
        });
        return devicePointInfoMap;
    }

    /**
     * Get point attribute config map
     *
     * @param deviceBO          Device
     * @param profilePointMap   Profile Point Map
     * @param pointAttributeMap Point Attribute Map
     * @return map(pointId, attribute ( attributeName, attributeInfo ( value, type)))
     */
    public Map<Long, Map<String, AttributeInfoDTO>> getPointInfoMap(DeviceBO deviceBO, Map<Long, Map<Long, PointBO>> profilePointMap, Map<Long, PointAttributeBO> pointAttributeMap) {
        Map<Long, Map<String, AttributeInfoDTO>> attributeInfoMap = new ConcurrentHashMap<>(16);
        deviceBO.getProfileIds().forEach(profileId -> profilePointMap.get(profileId).keySet()
                .forEach(pointId -> {
                    try {
                        List<PointAttributeConfigBO> pointAttributeConfigBOS = pointAttributeConfigService.selectByDeviceIdAndPointId(deviceBO.getId(), pointId);
                        Map<String, AttributeInfoDTO> infoMap = new ConcurrentHashMap<>(16);
                        pointAttributeConfigBOS.forEach(pointInfo -> {
                            PointAttributeBO attribute = pointAttributeMap.get(pointInfo.getPointAttributeId());
                            infoMap.put(attribute.getAttributeName(), new AttributeInfoDTO(pointInfo.getConfigValue(), attribute.getAttributeTypeFlag()));
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
     * @param deviceBOS Device Array
     * @return map(pointId, point)
     */
    public Map<Long, DeviceBO> getDeviceMap(List<DeviceBO> deviceBOS) {
        Map<Long, DeviceBO> deviceMap = new ConcurrentHashMap<>(16);
        deviceBOS.forEach(device -> deviceMap.put(device.getId(), device));
        return deviceMap;
    }

    /**
     * Get profile  map
     *
     * @param deviceIds 设备ID集
     * @return map(profileId ( pointId, point))
     */
    public Map<Long, Map<Long, PointBO>> getProfilePointMap(Set<Long> deviceIds) {
        Map<Long, Map<Long, PointBO>> profilePointMap = new ConcurrentHashMap<>(16);
        deviceIds.forEach(deviceId -> {
            Set<Long> profileIds = profileBindService.selectProfileIdsByDeviceId(deviceId);
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
    public Map<Long, PointBO> getPointMap(Long profileId) {
        Map<Long, PointBO> pointMap = new ConcurrentHashMap<>(16);
        try {
            pointService.selectByProfileId(profileId).forEach(point -> pointMap.put(point.getId(), point));
        } catch (NotFoundException ignored) {
            // nothing to do
        }
        return pointMap;
    }

}

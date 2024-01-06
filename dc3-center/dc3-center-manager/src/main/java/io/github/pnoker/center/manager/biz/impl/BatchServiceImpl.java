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

package io.github.pnoker.center.manager.biz.impl;

import io.github.pnoker.center.manager.biz.BatchService;
import io.github.pnoker.center.manager.entity.bo.*;
import io.github.pnoker.center.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.center.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.center.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.center.manager.entity.builder.PointBuilder;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
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
    private DriverAttributeBuilder driverAttributeBuilder;
    @Resource
    private PointAttributeBuilder pointAttributeBuilder;
    @Resource
    private DeviceBuilder deviceBuilder;
    @Resource
    private PointBuilder pointBuilder;

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

    @Override
    public DriverMetadataDTO batchDriverMetadata(String serviceName, Long tenantId) {
        DriverMetadataDTO driverMetadataDTO = new DriverMetadataDTO();
        DriverBO entityDO = driverService.selectByServiceName(serviceName, tenantId);
        driverMetadataDTO.setDriverId(entityDO.getId());
        driverMetadataDTO.setTenantId(entityDO.getTenantId());

        Map<Long, DriverAttributeDTO> driverAttributeMap = getDriverAttributeMap(entityDO.getId());
        driverMetadataDTO.setDriverAttributeMap(driverAttributeMap);

        Map<Long, PointAttributeDTO> pointAttributeMap = getPointAttributeMap(entityDO.getId());
        driverMetadataDTO.setPointAttributeMap(pointAttributeMap);

        List<DeviceBO> deviceBOS = deviceService.selectByDriverId(entityDO.getId());
        Set<Long> deviceIds = deviceBOS.stream().map(BaseBO::getId).collect(Collectors.toSet());

        Map<Long, Map<String, AttributeConfigDTO>> driverInfoMap = getDriverInfoMap(deviceIds, driverAttributeMap);
        driverMetadataDTO.setDriverInfoMap(driverInfoMap);

        Map<Long, DeviceDTO> deviceMap = getDeviceMap(deviceBOS);
        driverMetadataDTO.setDeviceMap(deviceMap);

        Map<Long, Map<Long, PointDTO>> profilePointMap = getProfilePointMap(deviceIds);
        driverMetadataDTO.setProfilePointMap(profilePointMap);

        Map<Long, Map<Long, Map<String, AttributeConfigDTO>>> devicePointInfoMap = getPointInfoMap(deviceBOS, profilePointMap, pointAttributeMap);
        driverMetadataDTO.setPointInfoMap(devicePointInfoMap);

        return driverMetadataDTO;
    }

    /**
     * 获取驱动属性Map
     *
     * @param driverId 驱动ID
     * @return map(driverAttributeId, driverAttribute)
     */
    public Map<Long, DriverAttributeDTO> getDriverAttributeMap(Long driverId) {
        List<DriverAttributeBO> entityBOS = driverAttributeService.selectByDriverId(driverId);
        List<DriverAttributeDTO> entityDTOS = driverAttributeBuilder.buildDTOListByBOList(entityBOS);
        return entityDTOS.stream().collect(Collectors.toMap(DriverAttributeDTO::getId, Function.identity()));
    }

    /**
     * 获取位号属性Map
     *
     * @param driverId 驱动ID
     * @return map(pointAttributeId, pointAttribute)
     */
    public Map<Long, PointAttributeDTO> getPointAttributeMap(Long driverId) {
        List<PointAttributeBO> entityBOS = pointAttributeService.selectByDriverId(driverId, true);
        List<PointAttributeDTO> entityDTOS = pointAttributeBuilder.buildDTOListByBOList(entityBOS);
        return entityDTOS.stream().collect(Collectors.toMap(PointAttributeDTO::getId, Function.identity()));
    }

    /**
     * 获取驱动属性配置Map
     *
     * @param deviceIds          设备ID集
     * @param driverAttributeMap 驱动属性Map
     * @return map(deviceId[driverAttribute.name[attributeInfo]]) 设备ID，属性名称，属性配置
     */
    public Map<Long, Map<String, AttributeConfigDTO>> getDriverInfoMap(Set<Long> deviceIds, Map<Long, DriverAttributeDTO> driverAttributeMap) {
        Map<Long, Map<String, AttributeConfigDTO>> driverInfoMap = new ConcurrentHashMap<>(16);
        deviceIds.forEach(deviceId -> {
            Map<String, AttributeConfigDTO> infoMap = getDriverInfoMap(deviceId, driverAttributeMap);
            if (infoMap.size() > 0) {
                driverInfoMap.put(deviceId, infoMap);
            }
        });
        return driverInfoMap;
    }

    /**
     * 获取驱动属性配置Map
     *
     * @param deviceId           设备ID
     * @param driverAttributeMap 驱动属性Map
     * @return map(attributeName, attributeInfo)
     */
    public Map<String, AttributeConfigDTO> getDriverInfoMap(Long deviceId, Map<Long, DriverAttributeDTO> driverAttributeMap) {
        Map<String, AttributeConfigDTO> attributeInfoMap = new ConcurrentHashMap<>(16);
        List<DriverAttributeConfigBO> driverAttributeConfigBOS = driverAttributeConfigService.selectByDeviceId(deviceId);
        driverAttributeConfigBOS.forEach(driverInfo -> {
            DriverAttributeDTO attribute = driverAttributeMap.get(driverInfo.getDriverAttributeId());
            attributeInfoMap.put(attribute.getAttributeName(), AttributeConfigDTO.builder().type(attribute.getAttributeTypeFlag()).value(driverInfo.getConfigValue()).build());
        });
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
    public Map<Long, Map<Long, Map<String, AttributeConfigDTO>>> getPointInfoMap(List<DeviceBO> deviceBOS, Map<Long, Map<Long, PointDTO>> profilePointMap, Map<Long, PointAttributeDTO> pointAttributeMap) {
        Map<Long, Map<Long, Map<String, AttributeConfigDTO>>> devicePointInfoMap = new ConcurrentHashMap<>(16);
        deviceBOS.forEach(device -> {
            Map<Long, Map<String, AttributeConfigDTO>> infoMap = getPointInfoMap(device, profilePointMap, pointAttributeMap);
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
    public Map<Long, Map<String, AttributeConfigDTO>> getPointInfoMap(DeviceBO deviceBO, Map<Long, Map<Long, PointDTO>> profilePointMap, Map<Long, PointAttributeDTO> pointAttributeMap) {
        Map<Long, Map<String, AttributeConfigDTO>> attributeInfoMap = new ConcurrentHashMap<>(16);
        deviceBO.getProfileIds().forEach(profileId -> profilePointMap.get(profileId).keySet()
                .forEach(pointId -> {
                    List<PointAttributeConfigBO> pointAttributeConfigBOS = pointAttributeConfigService.selectByDeviceIdAndPointId(deviceBO.getId(), pointId);
                    Map<String, AttributeConfigDTO> infoMap = new ConcurrentHashMap<>(16);
                    pointAttributeConfigBOS.forEach(pointInfo -> {
                        PointAttributeDTO attribute = pointAttributeMap.get(pointInfo.getPointAttributeId());
                        infoMap.put(attribute.getAttributeName(), AttributeConfigDTO.builder().type(attribute.getAttributeTypeFlag()).value(pointInfo.getConfigValue()).build());
                    });

                    if (infoMap.size() > 0) {
                        attributeInfoMap.put(pointId, infoMap);
                    }
                }));
        return attributeInfoMap;
    }

    /**
     * 获取设备Map
     *
     * @param entityBOS DeviceBO Array
     * @return map(deviceId, device)
     */
    public Map<Long, DeviceDTO> getDeviceMap(List<DeviceBO> entityBOS) {
        List<DeviceDTO> entityDTOS = deviceBuilder.buildDTOListByBOList(entityBOS);
        return entityDTOS.stream().collect(Collectors.toMap(DeviceDTO::getId, Function.identity()));
    }

    /**
     * 获取模版Map
     *
     * @param deviceIds 设备ID集
     * @return map(profileId[pointId, point])
     */
    public Map<Long, Map<Long, PointDTO>> getProfilePointMap(Set<Long> deviceIds) {
        Map<Long, Map<Long, PointDTO>> profilePointMap = new ConcurrentHashMap<>(16);
        deviceIds.forEach(deviceId -> {
            Set<Long> profileIds = profileBindService.selectProfileIdsByDeviceId(deviceId);
            profileIds.forEach(profileId -> profilePointMap.put(profileId, getPointMap(profileId)));
        });
        return profilePointMap;
    }

    /**
     * 获取位号Map
     *
     * @param profileId 位号ID
     * @return map(pointId, point)
     */
    public Map<Long, PointDTO> getPointMap(Long profileId) {
        List<PointBO> entityBOS = pointService.selectByProfileId(profileId);
        List<PointDTO> entityDTOS = pointBuilder.buildDTOListByBOList(entityBOS);
        return entityDTOS.stream().collect(Collectors.toMap(PointDTO::getId, Function.identity()));
    }

}

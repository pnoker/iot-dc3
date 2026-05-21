/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.driver.metadata;

import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.grpc.client.DeviceClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Device metadata cache that loads device definitions and resolves driver and point
 * configuration views for the driver runtime.
 *
 * <p>Cache freshness is event-driven: RabbitMQ metadata events call
 * {@link #loadCache(long)} or {@link #removeCache(long)}. There is no TTL — the
 * {@code maximumSize} bound only caps memory.
 *
 * <p>When the upstream loader returns {@code null} (the device has been removed at
 * the manager center but the DELETE event was missed or delayed), the cache also
 * drops the orphan id from {@link DriverMetadata#getDeviceIds()} so the periodic
 * read scan stops re-fetching a non-existent device.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
public final class DeviceMetadata extends AbstractMetadataCache<DeviceBO> {

    private final DriverMetadata driverMetadata;

    public DeviceMetadata(DriverProperties driverProperties,
                          DriverMetadata driverMetadata,
                          DeviceClient deviceClient) {
        super(driverProperties.getMetadata().getCache(), "device", deviceClient::getById);
        this.driverMetadata = driverMetadata;
    }

    @Override
    protected void postLoad(long id, DeviceBO value) {
        if (value == null) {
            // Manager has dropped this device; drop the orphan id so the Quartz read
            // scan stops attempting to read a record that no longer exists.
            if (driverMetadata.getDeviceIds().remove(id)) {
                log.info("Drop orphan device id={} after upstream returned null", id);
            }
        }
    }

    /**
     * Resolves driver attribute configuration for the specified device and verifies that
     * all required attributes are present. Returns an empty map when the configuration
     * is incomplete or the device cannot be resolved — never throws, so callers can
     * uniformly short-circuit on {@link Map#isEmpty()}.
     *
     * @param deviceId device identifier
     * @return driver configuration map keyed by attribute code, or an empty map
     */
    public Map<String, AttributeBO> getDriverConfig(long deviceId) {
        Map<Long, DriverAttributeDTO> attributeMap = driverMetadata.getDriverAttributeIdMap();
        if (MapUtils.isEmpty(attributeMap)) {
            return Map.of();
        }

        DeviceBO device = getCache(deviceId);
        if (device == null) {
            log.warn("Driver config unavailable, deviceId={}, reason=device cache miss", deviceId);
            return Map.of();
        }

        Map<Long, DriverAttributeConfigDTO> attributeConfigMap = device.getDriverAttributeConfigIdMap();
        if (MapUtils.isEmpty(attributeConfigMap)
                || !attributeConfigMap.keySet().containsAll(attributeMap.keySet())) {
            log.warn("Driver config incomplete, deviceId={}, required={}, configured={}",
                    deviceId, attributeMap.keySet(),
                    attributeConfigMap == null ? "[]" : attributeConfigMap.keySet());
            return Map.of();
        }

        return attributeMap.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getAttributeCode(),
                        entry -> AttributeBO.builder()
                                .type(entry.getValue().getAttributeTypeFlag())
                                .value(attributeConfigMap.get(entry.getKey()).getConfigValue())
                                .build()));
    }

    /**
     * Resolves point attribute configuration for all points of the specified device.
     * Points with incomplete configuration are filtered out silently; an empty map is
     * returned when the device or its point-config map is unavailable.
     *
     * @param deviceId device identifier
     * @return point configuration map keyed by point identifier and attribute code
     */
    public Map<Long, Map<String, AttributeBO>> getPointConfig(long deviceId) {
        Map<Long, PointAttributeDTO> attributeMap = driverMetadata.getPointAttributeIdMap();
        if (MapUtils.isEmpty(attributeMap)) {
            return Map.of();
        }

        DeviceBO device = getCache(deviceId);
        if (device == null) {
            log.warn("Point config unavailable, deviceId={}, reason=device cache miss", deviceId);
            return Map.of();
        }

        Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = device.getPointAttributeConfigIdMap();
        if (MapUtils.isEmpty(pointAttributeConfigMap)) {
            log.warn("Point config unavailable, deviceId={}, reason=empty point-attribute-config map", deviceId);
            return Map.of();
        }

        return pointAttributeConfigMap.entrySet()
                .stream()
                .filter(entry -> MapUtils.isNotEmpty(entry.getValue())
                        && entry.getValue().keySet().containsAll(attributeMap.keySet()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entryMap -> attributeMap.entrySet()
                                .stream()
                                .collect(Collectors.toMap(entry -> entry.getValue().getAttributeCode(),
                                        entry -> AttributeBO.builder()
                                                .type(entry.getValue().getAttributeTypeFlag())
                                                .value(entryMap.getValue().get(entry.getKey()).getConfigValue())
                                                .build()))));
    }

    /**
     * Resolves point attribute configuration for a single point of the specified device.
     * Returns an empty map when any required piece of context is missing — same
     * short-circuit semantics as {@link #getDriverConfig(long)}.
     *
     * @param deviceId device identifier
     * @param pointId  point identifier
     * @return point configuration map keyed by attribute code, or an empty map
     */
    public Map<String, AttributeBO> getPointConfig(long deviceId, long pointId) {
        Map<Long, PointAttributeDTO> attributeMap = driverMetadata.getPointAttributeIdMap();
        if (MapUtils.isEmpty(attributeMap)) {
            return Map.of();
        }

        DeviceBO device = getCache(deviceId);
        if (device == null) {
            log.warn("Point config unavailable, deviceId={}, pointId={}, reason=device cache miss", deviceId, pointId);
            return Map.of();
        }

        Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = device.getPointAttributeConfigIdMap();
        if (MapUtils.isEmpty(pointAttributeConfigMap)) {
            log.warn("Point config unavailable, deviceId={}, pointId={}, reason=empty point-attribute-config map",
                    deviceId, pointId);
            return Map.of();
        }

        Map<Long, PointAttributeConfigDTO> attributeConfigMap = pointAttributeConfigMap.get(pointId);
        if (MapUtils.isEmpty(attributeConfigMap)
                || !attributeConfigMap.keySet().containsAll(attributeMap.keySet())) {
            log.warn("Point config incomplete, deviceId={}, pointId={}, required={}, configured={}",
                    deviceId, pointId, attributeMap.keySet(),
                    attributeConfigMap == null ? "[]" : attributeConfigMap.keySet());
            return Map.of();
        }

        return attributeMap.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getAttributeCode(),
                        entry -> AttributeBO.builder()
                                .type(entry.getValue().getAttributeTypeFlag())
                                .value(attributeConfigMap.get(entry.getKey()).getConfigValue())
                                .build()));
    }

}

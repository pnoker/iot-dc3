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


import cn.hutool.core.map.MapUtil;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.driver.grpc.client.DeviceClient;
import io.github.pnoker.common.exception.ConfigException;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 设备元数据
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public final class DeviceMetadata {

    /**
     * 设备元数据缓存
     * <p>
     * deviceId, deviceDTO
     */
    private final AsyncLoadingCache<Long, DeviceBO> cache;

    @Resource
    private DriverMetadata driverMetadata;

    @Resource
    private DeviceClient deviceClient;

    private DeviceMetadata() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .removalListener((key, value, cause) -> log.info("Remove key={}, value={} cache, reason is: {}", key, value, cause))
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> {
                    log.info("Load device metadata by id: {}", key);
                    DeviceBO deviceBO = deviceClient.selectById(key);
                    log.info("Cache device metadata: {}", JsonUtil.toJsonString(deviceBO));
                    return deviceBO;
                }, executor));
    }

    /**
     * 获取缓存, 指定设备
     * Get cache for specified device
     *
     * @param id 设备ID Device ID
     * @return DeviceBO Device business object
     */
    public DeviceBO getCache(long id) {
        try {
            CompletableFuture<DeviceBO> future = cache.get(id);
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to get the device cache: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 重新加载缓存, 指定设备
     * Reload cache for specified device
     *
     * @param id 设备ID Device ID
     */
    public void loadCache(long id) {
        CompletableFuture<DeviceBO> future = CompletableFuture.supplyAsync(() -> deviceClient.selectById(id));
        cache.put(id, future);
    }

    /**
     * 删除缓存, 指定设备
     * Remove cache for specified device
     *
     * @param id 设备ID Device ID
     */
    public void removeCache(long id) {
        cache.put(id, CompletableFuture.completedFuture(null));
    }

    /**
     * 获取驱动属性配置
     * Get driver attribute configuration
     * <p>
     * 会校验是否完整
     * Will verify if configuration is complete
     *
     * @param deviceId 设备ID Device ID
     * @return 属性配置Map Attribute configuration map
     */
    public Map<String, AttributeBO> getDriverConfig(long deviceId) {
        Map<Long, DriverAttributeDTO> attributeMap = driverMetadata.getDriverAttributeIdMap();
        if (MapUtil.isEmpty(attributeMap)) {
            return MapUtil.empty();
        }

        DeviceBO device = getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ConfigException("Failed to get driver config, the device is empty");
        }

        Map<Long, DriverAttributeConfigDTO> attributeConfigMap = device.getDriverAttributeConfigIdMap();
        if (MapUtil.isEmpty(attributeConfigMap)) {
            throw new ConfigException("Failed to get driver config, the driver attribute config is empty");
        }
        if (!attributeConfigMap.keySet().containsAll(attributeMap.keySet())) {
            throw new ConfigException("Failed to get driver config, the driver attribute config is incomplete");
        }

        return attributeMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getValue().getAttributeCode(),
                        entry -> AttributeBO.builder()
                                .type(entry.getValue().getAttributeTypeFlag())
                                .value(attributeConfigMap.get(entry.getKey()).getConfigValue())
                                .build()
                ));
    }

    /**
     * 获取设备下全部位号属性配置
     * Get all point attribute configurations for device
     * <p>
     * 会校验是否完整
     * Will verify if configuration is complete
     *
     * @param deviceId 设备ID Device ID
     * @return 属性配置Map Attribute configuration map
     */
    public Map<Long, Map<String, AttributeBO>> getPointConfig(long deviceId) {
        Map<Long, PointAttributeDTO> attributeMap = driverMetadata.getPointAttributeIdMap();
        if (MapUtil.isEmpty(attributeMap)) {
            return MapUtil.empty();
        }

        DeviceBO device = getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ConfigException("Failed to get point config[{}], the device is empty", deviceId);
        }

        Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = device.getPointAttributeConfigIdMap();
        if (Objects.isNull(pointAttributeConfigMap)) {
            throw new ConfigException("Failed to get point config[{}], the device point attribute config is empty", deviceId);
        }

        return pointAttributeConfigMap.entrySet().stream()
                .filter(entry -> MapUtil.isEmpty(entry.getValue()) && entry.getValue().keySet().containsAll(attributeMap.keySet()))
                .collect(Collectors.toMap(Map.Entry::getKey, entryMap -> attributeMap.entrySet().stream().collect(Collectors.toMap(
                                entry -> entry.getValue().getAttributeCode(),
                                entry -> AttributeBO.builder()
                                        .type(entry.getValue().getAttributeTypeFlag())
                                        .value(entryMap.getValue().get(entry.getKey()).getConfigValue())
                                        .build())
                        )
                ));
    }

    /**
     * 获取设备下指定位号属性配置
     * Get specific point attribute configuration for device
     * <p>
     * 会校验是否完整
     * Will verify if configuration is complete
     *
     * @param deviceId 设备ID Device ID
     * @param pointId  位号ID Point ID
     * @return 属性配置Map Attribute configuration map
     */
    public Map<String, AttributeBO> getPointConfig(long deviceId, long pointId) {
        Map<Long, PointAttributeDTO> attributeMap = driverMetadata.getPointAttributeIdMap();
        if (MapUtil.isEmpty(attributeMap)) {
            return MapUtil.empty();
        }

        DeviceBO device = getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ConfigException("Failed to get point config[{}:{}], the device is empty", deviceId, pointId);
        }

        Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = device.getPointAttributeConfigIdMap();
        if (Objects.isNull(pointAttributeConfigMap)) {
            throw new ConfigException("Failed to get point config[{}:{}], the device point attribute config is empty", deviceId, pointId);
        }

        Map<Long, PointAttributeConfigDTO> attributeConfigMap = pointAttributeConfigMap.get(pointId);
        if (MapUtil.isEmpty(attributeConfigMap)) {
            throw new ConfigException("Failed to get point config[{}:{}], the point attribute config is empty", deviceId, pointId);
        }
        if (!attributeConfigMap.keySet().containsAll(attributeMap.keySet())) {
            throw new ConfigException("Failed to get point config[{}:{}], the point attribute config is incomplete", deviceId, pointId);
        }

        return attributeMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getValue().getAttributeCode(),
                        entry -> AttributeBO.builder()
                                .type(entry.getValue().getAttributeTypeFlag())
                                .value(attributeConfigMap.get(entry.getKey()).getConfigValue())
                                .build()
                ));
    }
}

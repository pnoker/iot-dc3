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

    public DeviceMetadata() {
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
     *
     * @param id 设备ID
     * @return DeviceBO
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
     *
     * @param id 设备ID
     */
    public void loadCache(long id) {
        CompletableFuture<DeviceBO> future = CompletableFuture.supplyAsync(() -> deviceClient.selectById(id));
        cache.put(id, future);
    }

    /**
     * 删除缓存, 指定设备
     *
     * @param id 设备ID
     */
    public void removeCache(long id) {
        cache.put(id, CompletableFuture.completedFuture(null));
    }

    /**
     * 获取驱动属性配置
     * <p>
     * 会校验是否完整
     *
     * @param deviceId 设备ID
     * @return 属性配置Map
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
                        entry -> entry.getValue().getAttributeName(),
                        entry -> AttributeBO.builder()
                                .type(entry.getValue().getAttributeTypeFlag())
                                .value(attributeConfigMap.get(entry.getKey()).getConfigValue())
                                .build()
                ));
    }

    /**
     * 获取设备下指定位号属性配置
     * <p>
     * 会校验是否完整
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return 属性配置Map
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
                        entry -> entry.getValue().getAttributeName(),
                        entry -> AttributeBO.builder()
                                .type(entry.getValue().getAttributeTypeFlag())
                                .value(attributeConfigMap.get(entry.getKey()).getConfigValue())
                                .build()
                ));
    }

    /**
     * 获取设备下全部位号属性配置
     * <p>
     * 会校验是否完整
     *
     * @param deviceId 设备ID
     * @return 属性配置Map
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
                                entry -> entry.getValue().getAttributeName(),
                                entry -> AttributeBO.builder()
                                        .type(entry.getValue().getAttributeTypeFlag())
                                        .value(entryMap.getValue().get(entry.getKey()).getConfigValue())
                                        .build())
                        )
                ));
    }
}

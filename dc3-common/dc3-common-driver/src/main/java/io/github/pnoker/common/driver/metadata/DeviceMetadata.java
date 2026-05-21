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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Device metadata cache that loads device definitions and resolves driver and point
 * configuration views for the driver runtime.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
public final class DeviceMetadata {

    /**
     * Upper bound on a single cache lookup so a stuck manager center cannot pin
     * driver worker threads forever; expired waits return {@code null} and let
     * callers move on instead of holding the Quartz / RabbitMQ thread hostage.
     */
    private static final long CACHE_LOAD_TIMEOUT_SECONDS = 5L;

    /**
     * Asynchronous cache keyed by device identifier. No time-based expiration —
     * cache contents are kept current by RabbitMQ metadata events that call
     * {@link #loadCache(long)} or {@link #removeCache(long)}; the
     * {@code maximumSize} bound caps memory if the driver attaches an
     * unexpectedly large device fleet.
     */
    private final AsyncLoadingCache<Long, DeviceBO> cache;

    private final DriverMetadata driverMetadata;

    private final DeviceClient deviceClient;

    public DeviceMetadata(DriverMetadata driverMetadata, DeviceClient deviceClient) {
        this.driverMetadata = driverMetadata;
        this.deviceClient = deviceClient;
        this.cache = Caffeine.newBuilder()
                .maximumSize(5000)
                .removalListener(
                        (key, value, cause) -> log.info("Remove key={}, value={} cache, reason is: {}", key, value, cause))
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> {
                    log.info("Load device metadata by id: {}", key);
                    DeviceBO deviceBO = deviceClient.getById(key);
                    log.info("Cache device metadata: {}", JsonUtil.toJsonString(deviceBO));
                    return deviceBO;
                }, executor));
    }

    /**
     * Returns the cached device metadata for the specified device identifier.
     *
     * @param id device identifier
     * @return cached device business object
     */
    public DeviceBO getCache(long id) {
        try {
            CompletableFuture<DeviceBO> future = cache.get(id);
            return future.get(CACHE_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while loading device cache, deviceId={}", id, e);
            return null;
        } catch (TimeoutException e) {
            log.warn("Timed out loading device cache after {}s, deviceId={}", CACHE_LOAD_TIMEOUT_SECONDS, id);
            return null;
        } catch (ExecutionException e) {
            log.error("Failed to load device cache, deviceId={}", id, e);
            return null;
        }
    }

    /**
     * Reloads the cache entry for the specified device identifier.
     *
     * @param id device identifier
     */
    public void loadCache(long id) {
        CompletableFuture.supplyAsync(() -> deviceClient.getById(id))
                .whenComplete((device, throwable) -> {
                    if (Objects.nonNull(throwable)) {
                        log.error("Failed to reload device metadata, deviceId={}", id, throwable);
                        return;
                    }
                    if (Objects.isNull(device)) {
                        cache.synchronous().invalidate(id);
                        return;
                    }
                    cache.put(id, CompletableFuture.completedFuture(device));
                });
    }

    /**
     * Removes the cache entry for the specified device identifier.
     *
     * @param id device identifier
     */
    public void removeCache(long id) {
        cache.synchronous().invalidate(id);
    }

    /**
     * Clears all cached device metadata.
     */
    public void clearCache() {
        cache.synchronous().invalidateAll();
    }

    /**
     * Resolves driver attribute configuration for the specified device and verifies that
     * all required attributes are present.
     *
     * @param deviceId device identifier
     * @return driver configuration map keyed by attribute code
     */
    public Map<String, AttributeBO> getDriverConfig(long deviceId) {
        Map<Long, DriverAttributeDTO> attributeMap = driverMetadata.getDriverAttributeIdMap();
        if (MapUtils.isEmpty(attributeMap)) {
            return Map.of();
        }

        DeviceBO device = getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ConfigException("Failed to get driver config, the device is empty");
        }

        Map<Long, DriverAttributeConfigDTO> attributeConfigMap = device.getDriverAttributeConfigIdMap();
        if (MapUtils.isEmpty(attributeConfigMap)
                || !attributeConfigMap.keySet().containsAll(attributeMap.keySet())) {
            log.warn("Driver attribute config incomplete for device[{}], required={}, configured={}",
                    deviceId, attributeMap.keySet(), attributeConfigMap == null ? "[]" : attributeConfigMap.keySet());
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
        if (Objects.isNull(device)) {
            throw new ConfigException("Failed to get point config[{}], the device is empty", deviceId);
        }

        Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = device.getPointAttributeConfigIdMap();
        if (Objects.isNull(pointAttributeConfigMap)) {
            throw new ConfigException("Failed to get point config[{}], the device point attribute config is empty",
                    deviceId);
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
     *
     * @param deviceId device identifier
     * @param pointId  point identifier
     * @return point configuration map keyed by attribute code
     */
    public Map<String, AttributeBO> getPointConfig(long deviceId, long pointId) {
        Map<Long, PointAttributeDTO> attributeMap = driverMetadata.getPointAttributeIdMap();
        if (MapUtils.isEmpty(attributeMap)) {
            return Map.of();
        }

        DeviceBO device = getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ConfigException("Failed to get point config[{}:{}], the device is empty", deviceId, pointId);
        }

        Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = device.getPointAttributeConfigIdMap();
        if (Objects.isNull(pointAttributeConfigMap)) {
            throw new ConfigException("Failed to get point config[{}:{}], the device point attribute config is empty",
                    deviceId, pointId);
        }

        Map<Long, PointAttributeConfigDTO> attributeConfigMap = pointAttributeConfigMap.get(pointId);
        if (MapUtils.isEmpty(attributeConfigMap)) {
            throw new ConfigException("Failed to get point config[{}:{}], the point attribute config is empty",
                    deviceId, pointId);
        }
        if (!attributeConfigMap.keySet().containsAll(attributeMap.keySet())) {
            throw new ConfigException("Failed to get point config[{}:{}], the point attribute config is incomplete",
                    deviceId, pointId);
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

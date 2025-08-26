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
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.grpc.client.PointClient;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 位号元数据
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public final class PointMetadata {

    /**
     * 位号元数据缓存
     * <p>
     * pointId,pointDTO
     */
    private final AsyncLoadingCache<Long, PointBO> cache;

    private final PointClient pointClient;

    public PointMetadata(PointClient pointClient) {
        this.pointClient = pointClient;
        this.cache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .removalListener((key, value, cause) -> log.info("Remove key={}, value={} cache, reason is: {}", key, value, cause))
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> {
                    log.info("Load point metadata by id: {}", key);
                    PointBO pointBO = this.pointClient.selectById(key);
                    log.info("Cache point metadata: {}", JsonUtil.toJsonString(pointBO));
                    return pointBO;
                }, executor));
    }

    /**
     * 获取缓存, 指定位号
     *
     * @param id 位号ID
     * @return PointBO
     */
    public PointBO getCache(long id) {
        try {
            CompletableFuture<PointBO> future = cache.get(id);
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to get the point cache: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 重新加载缓存, 指定位号
     *
     * @param id 位号ID
     */
    public void loadCache(long id) {
        CompletableFuture<PointBO> future = CompletableFuture.supplyAsync(() -> pointClient.selectById(id));
        cache.put(id, future);
    }

    /**
     * 删除缓存, 指定位号
     *
     * @param id 位号ID
     */
    public void removeCache(long id) {
        cache.put(id, CompletableFuture.completedFuture(null));
    }
}

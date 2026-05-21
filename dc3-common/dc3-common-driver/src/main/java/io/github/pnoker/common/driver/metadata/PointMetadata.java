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
import java.util.concurrent.TimeoutException;

/**
 * Point metadata cache used to lazily load and refresh point definitions referenced by
 * the driver.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
public final class PointMetadata {

    /**
     * Upper bound on a single cache lookup so a stuck manager center cannot pin
     * driver worker threads forever; expired waits return {@code null} and let
     * callers move on instead of holding the Quartz / RabbitMQ thread hostage.
     */
    private static final long CACHE_LOAD_TIMEOUT_SECONDS = 5L;

    /**
     * Asynchronous cache keyed by point identifier. No time-based expiration —
     * cache contents are kept current by RabbitMQ metadata events that call
     * {@link #loadCache(long)} or {@link #removeCache(long)}; the
     * {@code maximumSize} bound caps memory if the driver attaches an
     * unexpectedly large point set.
     */
    private final AsyncLoadingCache<Long, PointBO> cache;

    private final PointClient pointClient;

    public PointMetadata(PointClient pointClient) {
        this.pointClient = pointClient;
        this.cache = Caffeine.newBuilder()
                .maximumSize(5000)
                .removalListener(
                        (key, value, cause) -> log.info("Remove key={}, value={} cache, reason is: {}", key, value, cause))
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> {
                    log.info("Load point metadata by id: {}", key);
                    PointBO pointBO = this.pointClient.getById(key);
                    log.info("Cache point metadata: {}", JsonUtil.toJsonString(pointBO));
                    return pointBO;
                }, executor));
    }

    /**
     * Returns the cached point metadata for the specified point identifier.
     *
     * @param id point identifier
     * @return cached point business object
     */
    public PointBO getCache(long id) {
        try {
            CompletableFuture<PointBO> future = cache.get(id);
            return future.get(CACHE_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while loading point cache, pointId={}", id, e);
            return null;
        } catch (TimeoutException e) {
            log.warn("Timed out loading point cache after {}s, pointId={}", CACHE_LOAD_TIMEOUT_SECONDS, id);
            return null;
        } catch (ExecutionException e) {
            log.error("Failed to load point cache, pointId={}", id, e);
            return null;
        }
    }

    /**
     * Reloads the cache entry for the specified point identifier.
     *
     * @param id point identifier
     */
    public void loadCache(long id) {
        CompletableFuture.supplyAsync(() -> pointClient.getById(id))
                .whenComplete((point, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to reload point metadata, pointId={}", id, throwable);
                        return;
                    }
                    if (point == null) {
                        cache.synchronous().invalidate(id);
                        return;
                    }
                    cache.put(id, CompletableFuture.completedFuture(point));
                });
    }

    /**
     * Removes the cache entry for the specified point identifier.
     *
     * @param id point identifier
     */
    public void removeCache(long id) {
        cache.synchronous().invalidate(id);
    }

    /**
     * Clears all cached point metadata.
     */
    public void clearCache() {
        cache.synchronous().invalidateAll();
    }

}

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
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Shared scaffolding for the driver-side metadata caches ({@link DeviceMetadata},
 * {@link PointMetadata}). Wraps a Caffeine {@link AsyncLoadingCache} keyed by id and
 * exposes the load / refresh / invalidate primitives used by the RabbitMQ metadata
 * consumer.
 *
 * <p>Cache freshness comes from RabbitMQ events, not TTL. {@code maximumSize} caps
 * memory only. {@link #loadCache(long)} is intentionally synchronous: it lets the
 * MetadataReceiver surface gRPC failures so the message can be nack-requeued instead
 * of silently dropped.
 *
 * @param <V> cached value type
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public abstract class AbstractMetadataCache<V> {

    private final AsyncLoadingCache<Long, V> cache;

    private final long loadTimeoutSeconds;

    private final String name;

    protected AbstractMetadataCache(DriverProperties.MetadataProperties.CacheProperties cacheProps,
                                    String name,
                                    Function<Long, V> loader) {
        this.name = name;
        this.loadTimeoutSeconds = cacheProps.getLoadTimeoutSeconds();

        RemovalListener<Long, V> removalListener = (key, value, cause) ->
                log.debug("Evict {} cache, id={}, cause={}", name, key, cause);

        Caffeine<Long, V> builder = Caffeine.newBuilder()
                .maximumSize(cacheProps.getMaxSize())
                .<Long, V>removalListener(removalListener);
        if (cacheProps.isRecordStats()) {
            builder.recordStats();
        }

        this.cache = builder.buildAsync((id, executor) -> CompletableFuture.supplyAsync(() -> {
            log.debug("Load {} metadata, id={}", name, id);
            V value = loader.apply(id);
            postLoad(id, value);
            return value;
        }, executor));
    }

    /**
     * Hook invoked after the underlying loader returns (including {@code null}). The
     * default implementation is a no-op. Subclasses override this to keep secondary
     * indexes (e.g. driver device-id set) in sync with the cache.
     *
     * @param id    cache key just loaded
     * @param value loaded value, may be {@code null} when the upstream record is gone
     */
    protected void postLoad(long id, V value) {
        // no-op
    }

    /**
     * Returns the cached value for {@code id}, triggering the loader on miss. Bounded
     * by {@link DriverProperties.MetadataProperties.CacheProperties#getLoadTimeoutSeconds()}.
     *
     * @param id cache key
     * @return cached value, or {@code null} if loading timed out, was interrupted, or
     * failed; callers are expected to treat {@code null} as "not available now"
     */
    public V getCache(long id) {
        try {
            return cache.get(id).get(loadTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while loading {} cache, id={}", name, id, e);
            return null;
        } catch (TimeoutException ignored) {
            log.warn("Timed out loading {} cache after {}s, id={}", name, loadTimeoutSeconds, id);
            return null;
        } catch (ExecutionException e) {
            log.error("Failed to load {} cache, id={}", name, id, e);
            return null;
        }
    }

    /**
     * Refreshes the cache entry for {@code id}, blocking until the load completes or
     * times out so the caller (typically the RabbitMQ metadata consumer) can decide
     * whether to ack or nack the triggering event.
     *
     * <p>Behavior:
     * <ul>
     *   <li>Loader returns a non-null value: cache is updated and the value is returned.</li>
     *   <li>Loader returns {@code null}: cache stays empty for this key (Caffeine
     *       drops null loads); {@link #postLoad(long, Object)} still fires so the
     *       subclass can clean up secondary state. Returns {@code null}.</li>
     *   <li>Loader throws: a {@link ServiceException} is raised wrapping the cause so
     *       the caller can react. Cache is left unchanged.</li>
     * </ul>
     *
     * @param id cache key
     * @return loaded value, or {@code null} if upstream reports the record is gone
     * @throws ServiceException when the loader fails or the wait is interrupted/times out
     */
    public V loadCache(long id) {
        try {
            return cache.synchronous().refresh(id).get(loadTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Interrupted while refreshing {} cache, id={}", name, id, e);
        } catch (TimeoutException e) {
            throw new ServiceException("Timed out refreshing {} cache after {}s, id={}",
                    name, loadTimeoutSeconds, id, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ServiceException("Failed to refresh {} cache, id={}", name, id, cause);
        }
    }

    /**
     * Removes the cache entry for {@code id}.
     */
    public void removeCache(long id) {
        cache.synchronous().invalidate(id);
    }

    /**
     * Clears all cached entries.
     */
    public void clearCache() {
        cache.synchronous().invalidateAll();
    }

    /**
     * Snapshot of Caffeine statistics (only meaningful when {@code recordStats} is on).
     * Exposed for diagnostic endpoints; production code should not branch on it.
     */
    public CacheStats stats() {
        return cache.synchronous().stats();
    }

}

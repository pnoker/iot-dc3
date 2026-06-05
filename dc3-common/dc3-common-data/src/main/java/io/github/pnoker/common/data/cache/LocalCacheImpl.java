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

package io.github.pnoker.common.data.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Thin Caffeine wrapper with a Redis-like surface (setKey/getKey with optional TTL, batch
 * variants) so call sites only need to swap the injected type. Variable per-entry TTL is
 * honored via a custom {@link Expiry}.
 *
 * <p>
 * Exposes {@link #onExpire(ExpireListener)} so callers can react when an entry is evicted
 * because its TTL elapsed — e.g. the driver / device status keys use this to synthesise
 * OFFLINE alarm rows without a scanning thread.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
public class LocalCacheImpl {

    /**
     * Hooks fired exclusively on TTL-driven evictions. Copy-on-write list so registration
     * from different modules (data biz, dashboards, …) is safe without locking.
     */
    private final List<ExpireListener> expireListeners = new CopyOnWriteArrayList<>();

    private Cache<String, Entry> cache;

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder().maximumSize(200_000L).expireAfter(new Expiry<String, Entry>() {
            @Override
            @SuppressWarnings("NullableProblems")
            public long expireAfterCreate(String key, Entry value, long currentTime) {
                return value.ttlNanos;
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public long expireAfterUpdate(String key, Entry value, long currentTime, long currentDuration) {
                return value.ttlNanos;
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public long expireAfterRead(String key, Entry value, long currentTime, long currentDuration) {
                return currentDuration;
            }
        }).removalListener((String key, Entry value, RemovalCause cause) -> {
            // Only fire listeners for natural TTL expiries. Explicit
            // cache.invalidate() / size-based eviction aren't
            // "the key went offline" semantically.
            if (cause != RemovalCause.EXPIRED || Objects.isNull(key) || Objects.isNull(value))
                return;
            for (ExpireListener listener : expireListeners) {
                try {
                    listener.onExpire(key, value.value);
                } catch (Exception e) {
                    log.warn("Expire listener failed, key={}", key, e);
                }
            }
        }).build();
    }

    public <T> void setKey(String key, T value) {
        cache.put(key, new Entry(value, Long.MAX_VALUE));
    }

    public <T> void setKey(String key, T value, long time, TimeUnit unit) {
        long ttl = time > 0 ? unit.toNanos(time) : Long.MAX_VALUE;
        cache.put(key, new Entry(value, ttl));
    }

    public <T> void setKey(Map<String, T> valuesMap) {
        if (Objects.isNull(valuesMap) || valuesMap.isEmpty()) {
            return;
        }
        valuesMap.forEach(this::setKey);
    }

    @SuppressWarnings("unchecked")
    public <T> T getKey(String key) {
        Entry entry = cache.getIfPresent(key);
        return Objects.isNull(entry) ? null : (T) entry.value;
    }

    public <T> List<T> getKey(List<String> keys) {
        if (Objects.isNull(keys) || keys.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>(keys.size());
        for (String key : keys) {
            result.add(getKey(key));
        }
        return result;
    }

    /**
     * Register a callback that fires when an entry expires due to its TTL. The callback
     * runs on Caffeine's removal executor (default: same thread that triggered the expiry
     * read / write); keep the handler short.
     */
    public void onExpire(ExpireListener listener) {
        if (Objects.nonNull(listener))
            expireListeners.add(listener);
    }

    @FunctionalInterface
    public interface ExpireListener {

        void onExpire(String key, Object lastValue);

    }

    private record Entry(Object value, long ttlNanos) {
    }

}

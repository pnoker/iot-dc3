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
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * In-process key-value cache backed by Caffeine.
 * <p>
 * Drop-in replacement for the legacy Redis-based KV service: preserves the same method
 * surface (setKey/getKey with optional TTL, batch variants) so call sites only need to
 * swap the injected type. Variable per-entry TTL is honored via a custom {@link Expiry}.
 * </p>
 */
@Component
public class LocalCacheService {

    private Cache<String, Entry> cache;

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(200_000L)
                .expireAfter(new Expiry<String, Entry>() {
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
                })
                .build();
    }

    public <T> void setKey(String key, T value) {
        cache.put(key, new Entry(value, Long.MAX_VALUE));
    }

    public <T> void setKey(String key, T value, long time, TimeUnit unit) {
        long ttl = time > 0 ? unit.toNanos(time) : Long.MAX_VALUE;
        cache.put(key, new Entry(value, ttl));
    }

    public <T> void setKey(Map<String, T> valuesMap) {
        if (valuesMap == null || valuesMap.isEmpty()) {
            return;
        }
        valuesMap.forEach(this::setKey);
    }

    @SuppressWarnings("unchecked")
    public <T> T getKey(String key) {
        Entry entry = cache.getIfPresent(key);
        return entry == null ? null : (T) entry.value;
    }

    public <T> List<T> getKey(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>(keys.size());
        for (String key : keys) {
            result.add(getKey(key));
        }
        return result;
    }

    private record Entry(Object value, long ttlNanos) {
    }
}

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

package io.github.pnoker.common.redis.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Service Utility Class
 * <p>
 * Service class providing comprehensive Redis operations for caching and data storage.
 * Supports key-value operations, batch operations, expiration management,
 * and pattern-based key searches with generic type support.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
@SuppressWarnings({"rawtypes", "unchecked"})
public class RedisService {

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * Add key cache
     *
     * @param key   String key
     * @param value Object
     * @param <T>   Value Type
     */
    public <T> void setKey(String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Add key cache with expiration time
     *
     * @param key   String key
     * @param value Object
     * @param time  Time
     * @param unit  TimeUnit
     * @param <T>   Value Type
     */
    public <T> void setKey(String key, final T value, long time, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, time, unit);
    }

    /**
     * Batch add key cache
     *
     * @param valuesMap Map String:Object
     * @param <T>       Value Type
     */
    public <T> void setKey(Map<String, T> valuesMap) {
        redisTemplate.opsForValue().multiSet(valuesMap);
    }

    /**
     * Batch add key cache with expiration time
     *
     * @param valueMap     Map String:Object
     * @param expireMillis Map String:Long
     * @param <T>          Value Type
     */
    public <T> void setKey(Map<String, T> valueMap, Map<String, Long> expireMillis) {
        redisTemplate.opsForValue().multiSet(valueMap);
        setExpire(expireMillis);
    }

    /**
     * Get key cache
     *
     * @param key String key
     * @param <T> Value Type
     * @return T
     */
    public <T> T getKey(final String key) {
        ValueOperations<String, T> operations = redisTemplate.opsForValue();
        return operations.get(key);
    }

    /**
     * Batch get key cache values
     *
     * @param keys String key array
     * @param <T>  Value Type
     * @return T Array
     */
    public <T> List<T> getKey(List<String> keys) {
        ValueOperations<String, T> operations = redisTemplate.opsForValue();
        return operations.multiGet(keys);
    }

    /**
     * Check if key exists
     *
     * @param key String key
     * @return boolean
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * Get keys by pattern
     *
     * @param pattern Key pattern
     * @return Key Set
     */
    public Set<String> getKeys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * Delete key cache
     *
     * @param key Key
     */
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Batch delete key cache
     *
     * @param keys Key Array
     */
    public void deleteKey(List<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * Set key expiration time
     *
     * @param key  String key
     * @param time Time
     * @param unit TimeUnit
     */
    public void setExpire(String key, long time, TimeUnit unit) {
        if (time > 0) {
            redisTemplate.expire(key, time, unit);
        }
    }

    /**
     * Batch set key expiration time
     *
     * @param expireMillis Map String:Long
     */
    public void setExpire(Map<String, Long> expireMillis) {
        if (null != expireMillis && !expireMillis.isEmpty()) {
            StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                expireMillis.forEach((key, expire) -> {
                    byte[] serialize = stringRedisSerializer.serialize(key);
                    if (null != serialize) {
                        connection.commands().pExpire(serialize, expire);
                    }
                });
                return null;
            });
        }
    }

    /**
     * Set key to expire at specific date
     *
     * @param key  String key
     * @param date Date
     */
    public void setExpireAt(String key, Date date) {
        Date current = new Date();
        if (date.getTime() >= current.getTime()) {
            redisTemplate.expireAt(key, date);
        }
    }

    /**
     * Get key expiration time
     *
     * @param key  String key
     * @param unit TimeUnit
     * @return Remaining expiration time
     */
    public long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }
}

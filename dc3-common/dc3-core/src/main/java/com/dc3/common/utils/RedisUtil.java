/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * @author pnoker
 */
@Slf4j
@Component
public class RedisUtil {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 判断 Key 是否存在
     *
     * @param key
     * @return
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 添加 Key 缓存
     *
     * @param key
     * @param value
     */
    public void setKey(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 添加 Key 缓存,并设置失效时间
     *
     * @param key
     * @param value
     * @param time
     * @param unit
     */
    public void setKey(String key, Object value, long time, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, time, unit);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 获取 Key 缓存
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getKey(String key) {
        try {
            Object object = redisTemplate.opsForValue().get(key);
            return (T) object;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除 Keys 缓存
     *
     * @param keys
     */
    public void removeKey(String... keys) {
        if (null != keys && keys.length > 0) {
            try {
                if (keys.length == 1) {
                    redisTemplate.delete(keys[0]);
                } else {
                    redisTemplate.delete(CollectionUtils.arrayToList(keys));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 指定键值失效时间
     *
     * @param key
     * @param time
     * @param unit
     */
    public void expire(String key, long time, TimeUnit unit) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, unit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 获取 Key 失效时间
     *
     * @param key
     * @param unit
     * @return
     */
    public long expire(String key, TimeUnit unit) {
        try {
            return redisTemplate.getExpire(key, unit);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }
}

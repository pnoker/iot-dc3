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

package com.pnoker.common.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Component
public class RedisTools {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public RedisTools(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 指定缓存失效时间
     *
     * @param key
     * @param time 单位：秒
     * @return
     */
    public boolean setExpire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 获取缓存失效时间
     *
     * @param key
     * @return
     */
    public long getExpire(String key) {
        long expire = 0;
        try {
            expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return expire;
    }

    /**
     * 判断是否存在Key
     *
     * @param key
     * @return
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}

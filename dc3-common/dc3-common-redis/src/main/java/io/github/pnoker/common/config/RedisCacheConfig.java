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

package io.github.pnoker.common.config;

import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Configuration
@ConfigurationProperties(prefix = "spring.cache.redis")
public class RedisCacheConfig implements CachingConfigurer {

    @Resource
    private RedisConnectionFactory factory;

    @Setter
    private Duration timeToLive;

    /**
     * 自定义缓存 Key 生成策略
     *
     * @return KeyGenerator
     */
    @Bean
    public KeyGenerator firstKeyGenerator() {
        return (target, method, params) -> params[0].toString();
    }

    /**
     * 自定义缓存 Key 生成策略
     *
     * @return KeyGenerator
     */
    @Bean
    public KeyGenerator commonKeyGenerator() {
        final String dot = ".";
        final String hashTag = "#";
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());
            sb.append(dot);
            sb.append(method.getName());
            sb.append(hashTag);
            for (Object obj : params) {
                sb.append(obj.toString());
            }
            return sb.toString();
        };
    }

    /**
     * 自定义 RedisCacheManager 类, 主要是设置序列化, 解决乱码问题
     *
     * @return CacheManager
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        // 配置 ObjectMapper
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(JsonUtil.getJsonMapper(), Object.class);

        // 配置 Key & Value 序列化
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues().entryTtl(timeToLive);

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(factory);
        return builder.cacheDefaults(config).build();
    }

}

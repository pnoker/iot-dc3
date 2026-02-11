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
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache Configuration Class
 * <p>
 * Configuration class for Redis caching in Spring Boot applications.
 * Configures cache manager, key generators, and serialization strategies
 * to resolve character encoding issues and support custom cache policies.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
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
     * Custom cache key generator strategy
     *
     * @return KeyGenerator
     */
    @Bean
    public KeyGenerator firstKeyGenerator() {
        return (target, method, params) -> params[0].toString();
    }

    /**
     * Custom cache key generator strategy
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
     * Custom RedisCacheManager class, mainly for setting serialization to solve character encoding issues
     *
     * @return CacheManager
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        // Configure ObjectMapper
        JacksonJsonRedisSerializer<Object> serializer = new JacksonJsonRedisSerializer<>(JsonUtil.getObjectMapper(), Object.class);

        // Configure Key & Value serialization
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues().entryTtl(timeToLive);

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(factory);
        return builder.cacheDefaults(config).build();
    }

}

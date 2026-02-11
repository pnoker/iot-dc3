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
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis Template Configuration Class
 * <p>
 * Configuration class for Redis template in Spring Boot applications.
 * Configures serialization strategies for Redis operations to ensure
 * proper data handling and type safety.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Configuration
@AutoConfigureBefore(DataRedisAutoConfiguration.class)
public class RedisTemplateConfig {

    private final RedisConnectionFactory factory;

    public RedisTemplateConfig(RedisConnectionFactory factory) {
        this.factory = factory;
    }

    /**
     * Configure RedisTemplate with custom serialization
     *
     * @return Configured RedisTemplate bean
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        // Configure ObjectMapper for JSON serialization
        ObjectMapper objectMapper = JsonUtil.getObjectMapper();
        JacksonJsonRedisSerializer<Object> serializer = new JacksonJsonRedisSerializer<>(objectMapper, Object.class);

        // Configure Key & Value serialization
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        return template;
    }
}

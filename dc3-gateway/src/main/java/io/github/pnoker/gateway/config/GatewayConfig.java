/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.gateway.config;

import io.github.pnoker.gateway.fallback.GatewayFallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 自定义Route配置
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Configuration
public class GatewayConfig {

    private final GatewayFallback gatewayFallback;

    public GatewayConfig(GatewayFallback gatewayFallback) {
        this.gatewayFallback = gatewayFallback;
    }

    /**
     * 根据 HostAddress 进行限流
     *
     * @return KeyResolver
     */
    @Bean
    public KeyResolver hostKeyResolver() {
        return exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getHostString());
    }

    /**
     * Redis 令牌桶 限流
     *
     * @return RedisRateLimiter
     */
    @Bean
    RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 2000);
    }

    /**
     * fallback 配置
     *
     * @return RouterFunction
     */
    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions.route(RequestPredicates.path("/fallback").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), gatewayFallback);
    }
}

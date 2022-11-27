/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.gateway.config;

import io.github.pnoker.common.constant.service.AuthServiceConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class RouteConfig {

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
     * 自定义 RouteLocator
     *
     * @param builder RouteLocatorBuilder
     * @return RouteLocator
     */
    @Bean
    public RouteLocator gatewayRouteLocator(RouteLocatorBuilder builder) {
        String v3ApiPrefix = "/api/v3";
        String lbPrefix = "lb://";
        String authUri = lbPrefix + AuthServiceConstant.SERVICE_NAME;
        return builder.routes()
                .route("token_salt",
                        r -> r.path(v3ApiPrefix + "/token/salt")
                                .filters(
                                        f -> f.setPath("/auth/token/salt")
                                                .requestRateLimiter(this::setDefaultRequestRateLimiter)
                                                .circuitBreaker(this::setDefaultCircuitBreaker)
                                ).uri(authUri)
                )
                .route("generate_token",
                        r -> r.path(v3ApiPrefix + "/token/generate")
                                .filters(
                                        f -> f.setPath("/auth/token/generate")
                                                .requestRateLimiter(this::setDefaultRequestRateLimiter)
                                                .circuitBreaker(this::setDefaultCircuitBreaker)
                                ).uri(authUri)
                )
                .route("check_token",
                        r -> r.path(v3ApiPrefix + "/token/check")
                                .filters(
                                        f -> f.setPath("/auth/token/check")
                                                .requestRateLimiter(this::setDefaultRequestRateLimiter)
                                                .circuitBreaker(this::setDefaultCircuitBreaker)
                                ).uri(authUri)
                )
                .route("cancel_token",
                        r -> r.path(v3ApiPrefix + "/token/cancel")
                                .filters(
                                        f -> f.setPath("/auth/token/cancel")
                                                .requestRateLimiter(this::setDefaultRequestRateLimiter)
                                                .circuitBreaker(this::setDefaultCircuitBreaker)
                                ).uri(authUri)
                )
                .route("register_user",
                        r -> r.path(v3ApiPrefix + "/register")
                                .filters(
                                        f -> f.setPath("/auth/user/add")
                                                .requestRateLimiter(this::setDefaultRequestRateLimiter)
                                                .circuitBreaker(this::setDefaultCircuitBreaker)
                                ).uri(authUri)
                )
                .build();
    }

    /**
     * 设置默认的接口速率限制
     *
     * @param config Request Rate Limiter Config
     */
    private void setDefaultRequestRateLimiter(RequestRateLimiterGatewayFilterFactory.Config config) {
        config.setKeyResolver(hostKeyResolver()).setRateLimiter(redisRateLimiter());
    }

    /**
     * 设置默认的熔断地址
     *
     * @param config CircuitBreaker Config
     */
    private void setDefaultCircuitBreaker(SpringCloudCircuitBreakerFilterFactory.Config config) {
        config.setName("default").setFallbackUri("forward:/fallback");
    }

}

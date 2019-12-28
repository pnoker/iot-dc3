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

package com.pnoker.gateway.config;

import com.pnoker.gateway.hystrix.GatewayHystrix;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Mono;

/**
 * 自定义Route配置
 *
 * @author pnoker
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class RouteConfig {
    private final GatewayHystrix gatewayHystrix;

    @Bean
    public RouteLocator myRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("generate_token",
                        r -> r.path("/api/v3/token")
                                .filters(
                                        f -> f.stripPrefix(3).prefixPath("/auth/token")
                                                .requestRateLimiter(l -> l.setKeyResolver(hostKeyResolver()).setRateLimiter(redisRateLimiter()))
                                                .hystrix(h -> h.setName("default").setFallbackUri("forward:/fallback"))
                                ).uri("lb://dc3-auth")
                )
                .route("register_user",
                        r -> r.path("/api/v3/register")
                                .filters(
                                        f -> f.stripPrefix(3).prefixPath("/auth/user/add")
                                                .requestRateLimiter(l -> l.setKeyResolver(hostKeyResolver()).setRateLimiter(redisRateLimiter()))
                                                .hystrix(h -> h.setName("default").setFallbackUri("forward:/fallback"))
                                ).uri("lb://dc3-auth")
                )
                .build();
    }

    /**
     * 根据 HostAddress 进行限流
     *
     * @return
     */
    @Bean
    @Order(-10)
    public KeyResolver hostKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    /**
     * 限流
     *
     * @return
     */
    @Bean
    RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 2000);
    }

    @Bean
    public RouterFunction routerFunction() {
        return RouterFunctions.route(RequestPredicates.path("/fallback").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), gatewayHystrix);
    }
}

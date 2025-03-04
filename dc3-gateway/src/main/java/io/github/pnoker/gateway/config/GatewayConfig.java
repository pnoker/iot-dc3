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

import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.utils.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 自定义网关路由配置类
 * <p>
 * 该类用于配置网关的路由规则、限流策略以及 fallback 处理逻辑。
 * 通过注解 {@code @Configuration} 标记为 Spring 配置类，确保在应用启动时加载。
 *
 * @author pnoker
 * @version 2025.2.1
 * @since 2022.1.0
 */
@Slf4j
@Configuration
public class GatewayConfig {

    /**
     * 配置基于客户端 IP 地址的限流解析器
     * <p>
     * 该解析器从请求中提取客户端的 IP 地址，并将其作为限流的唯一标识。
     * 适用于根据客户端 IP 地址进行限流的场景。
     *
     * @return KeyResolver 返回一个基于客户端 IP 地址的限流解析器
     */
    @Bean
    public KeyResolver hostKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String ip = RequestUtil.getRemoteIp(request);
            return Mono.just(ip);
        };
    }

    /**
     * 配置 Redis 令牌桶限流器
     * <p>
     * 该限流器允许每秒最多处理 100 个请求，并且令牌桶的容量为 2000 个令牌。
     * 当请求速率超过限流器的处理能力时，多余的请求将被限流。
     *
     * @return RedisRateLimiter 返回一个 Redis 令牌桶限流器实例
     */
    @Bean
    RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 2000);
    }

    /**
     * 配置网关的 fallback 路由，用于处理服务不可用时的请求
     *
     * @return RouterFunction<ServerResponse> 返回一个路由函数，用于处理 "/fallback" 路径的请求
     */
    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions.route(RequestPredicates.path("/fallback"),
                request -> {
                    log.info(request.toString());
                    Optional<Object> originalUris = request.attribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
                    originalUris.ifPresent(originalUri -> log.error("Request Url:{} , Service Fallback", originalUri));
                    R<String> response = R.fail("No available server for this request");
                    return ServerResponse
                            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(response));
                });
    }
}

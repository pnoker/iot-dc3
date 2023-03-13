/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.gateway.filter.BlackIpGlobalFilter;
import io.github.pnoker.gateway.filter.factory.AuthenticGatewayFilterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.stream.Collectors;

/**
 * 网关配置
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

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public BlackIpGlobalFilter blackIpGlobalFilter() {
        return new BlackIpGlobalFilter();
    }

    @Bean
    public AuthenticGatewayFilterFactory authenticGatewayFilterFactory() {
        return new AuthenticGatewayFilterFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
        return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return RouterFunctions.route(RequestPredicates.path("/fallback").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), gatewayFallback);
    }

}
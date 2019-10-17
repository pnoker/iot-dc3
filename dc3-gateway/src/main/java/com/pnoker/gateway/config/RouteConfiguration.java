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

import com.pnoker.gateway.hystrix.FallbackHystrix;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

/**
 * <p>自定义Route配置
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class RouteConfiguration {
    private final FallbackHystrix fallbackHystrix;

    @Bean
    public RouteLocator myRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("register_route", r -> r.path("/register/**").filters(f -> f.stripPrefix(1)).uri("http://iotdc3.register:8100"))
                .route("eureka_route", r -> r.path("/eureka/**").uri("http://iotdc3.register:8100"))
                .route("monitor_route", r -> r.path("/monitor/**").filters(f -> f.stripPrefix(1)).uri("http://iotdc3.monitor:8200"))
                .build();
    }

    @Bean
    public RouterFunction routerFunction() {
        return RouterFunctions.route(
                RequestPredicates.path("/fallback")
                        .and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), fallbackHystrix);
    }
}

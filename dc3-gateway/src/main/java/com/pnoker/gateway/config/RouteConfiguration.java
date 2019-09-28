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
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 自定义Route配置
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
                .build();
    }

    @Bean
    public RouterFunction routerFunction() {
        return RouterFunctions.route(
                RequestPredicates.path("/fallback")
                        .and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), fallbackHystrix);
    }
}

/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.gateway.config;

import com.dc3.api.center.auth.blackIp.feign.BlackIpClient;
import com.dc3.common.bean.R;
import com.dc3.gateway.hystrix.GatewayHystrix;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author pnoker
 */
@Slf4j
@Configuration
public class GatewayConfig {
    private final GatewayHystrix gatewayHystrix;

    public GatewayConfig(GatewayHystrix gatewayHystrix) {
        this.gatewayHystrix = gatewayHystrix;
    }

    @Resource
    private BlackIpClient blackIpClient;

    @Bean
    public Encoder encoder() {
        return new JacksonEncoder();
    }

    @Bean
    public Decoder decoder() {
        return new JacksonDecoder();
    }

    @Bean
    public RouterFunction routerFunction() {
        return RouterFunctions.route(RequestPredicates.path("/fallback").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), gatewayHystrix);
    }

    @Bean
    @Order(-100)
    public GlobalFilter globalFilter() {
        return (exchange, chain) -> {
            //调用请求之前统计时间
            Long startTime = System.currentTimeMillis();

            ServerHttpRequest request = exchange.getRequest();
            String remoteIp = Objects.requireNonNull(request.getRemoteAddress()).getHostString();
            R<Boolean> blackIpValid = blackIpClient.checkBlackIpValid(remoteIp);
            if (blackIpValid.isOk()) {
                log.error("Forbidden Ip: {}", remoteIp);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange).then().then(Mono.fromRunnable(() -> {
                //调用请求之后统计时间
                Long endTime = System.currentTimeMillis();

                log.info("Remote Ip: {}; Request url: {}; Response code: {}; Time: {}ms", remoteIp, request.getURI().getRawPath(), exchange.getResponse().getStatusCode(), (endTime - startTime));
            }));
        };
    }

}
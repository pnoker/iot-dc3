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

package com.dc3.gateway.filter;

import com.dc3.api.center.auth.blackIp.feign.BlackIpClient;
import com.dc3.common.bean.R;
import com.dc3.gateway.utils.GatewayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * 自定义Ip黑名单过滤器
 *
 * @author pnoker
 */
@Slf4j
public class BlackIpGlobalFilter implements GlobalFilter, Ordered {

    @Resource
    private BlackIpClient blackIpClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //调用请求之前统计时间
        Long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();
        String remoteIp = GatewayUtil.getRemoteIp(request);
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
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

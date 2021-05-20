/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.gateway.utils.GatewayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * 自定义 Request Header 校验过滤器
 *
 * @author pnoker
 */
@Slf4j
public class HeaderGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //调用请求之前统计时间
        Long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();
        try {
            // 判断某个 Header Key 是否存在
            GatewayUtil.getRequestHeader(request, Common.Service.DC3_AUTH_TENANT_ID);
        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add(Common.Response.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.setStatusCode(HttpStatus.FORBIDDEN);

            DataBuffer dataBuffer = response.bufferFactory().wrap(JSON.toJSONBytes(R.fail(e.getMessage())));
            return response.writeWith(Mono.just(dataBuffer));
        }

        //调用请求之后统计时间
        Long endTime = System.currentTimeMillis();

        log.info("Request url: {}; Response code: {}; Time: {}ms", request.getURI().getRawPath(), exchange.getResponse().getStatusCode(), (endTime - startTime));

        return chain.filter(exchange);
    }
}

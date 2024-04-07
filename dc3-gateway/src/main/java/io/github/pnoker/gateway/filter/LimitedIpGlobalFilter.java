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

package io.github.pnoker.gateway.filter;

import io.github.pnoker.api.center.auth.LimitedIpApiGrpc;
import io.github.pnoker.common.constant.service.AuthConstant;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 自定义限制IP过滤器
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class LimitedIpGlobalFilter implements GlobalFilter, Ordered {

    @GrpcClient(AuthConstant.SERVICE_NAME)
    private LimitedIpApiGrpc.LimitedIpApiBlockingStub limitedIpApiBlockingStub;

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 20240327 先去掉登录限制
        /*ServerHttpRequest request = exchange.getRequest();
        String remoteIp = GatewayUtil.getRemoteIp(request);
        try {
            GrpcRLimitedIpDTO rLimitedIpDTO = limitedIpApiBlockingStub.checkValid(GrpcIpQuery.newBuilder().setIp(remoteIp).build());
            if (rLimitedIpDTO.getResult().getOk()) {
                log.error("Forbidden Ip: {}", remoteIp);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }*/

        return chain.filter(exchange);
    }
}

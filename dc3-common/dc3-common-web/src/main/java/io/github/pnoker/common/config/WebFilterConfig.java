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

package io.github.pnoker.common.config;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RequestUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.WebFilter;

import java.util.Objects;
import java.util.Optional;

/**
 * WebFilter 配置
 *
 * @author pnoker
 * @version 2025.6.1
 * @since 2022.1.0
 */
@Slf4j
@Configuration
public class WebFilterConfig {

    @Resource
    private ServerProperties serverProperties;

    /**
     * 自定义过滤器
     *
     * @return WebFilter
     */
    @Bean
    public WebFilter contextPathWebFilter() {
        String contextPath = Optional.ofNullable(serverProperties.getServlet().getContextPath()).orElse("/");
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (request.getURI().getPath().startsWith(contextPath)) {
                return chain.filter(
                        exchange.mutate()
                                .request(request.mutate().contextPath(contextPath).build())
                                .build());
            }
            return chain.filter(exchange);
        };
    }

    /**
     * 自定义拦截器
     *
     * @return WebFilter
     */
    @Bean
    public WebFilter interceptor() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String user = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_USER);

            if (CharSequenceUtil.isNotEmpty(user)) {
                try {
                    RequestHeader.UserHeader userHeader = JsonUtil.parseObject(user, RequestHeader.UserHeader.class);

                    if (Objects.isNull(userHeader) || Objects.isNull(userHeader.getTenantId()) || Objects.isNull(userHeader.getUserId())) {
                        log.warn("Invalid user header: {}", JsonUtil.toJsonString(userHeader));
                        return chain.filter(exchange).contextWrite(context -> context.delete(RequestConstant.Key.USER_HEADER));
                    } else {
                        log.debug("User header: {}", JsonUtil.toJsonString(userHeader));
                        return chain.filter(exchange).contextWrite(context -> context.put(RequestConstant.Key.USER_HEADER, userHeader));
                    }
                } catch (Exception e) {
                    log.error("Error parsing user header", e);
                }
            }

            return chain.filter(exchange);
        };
    }
}
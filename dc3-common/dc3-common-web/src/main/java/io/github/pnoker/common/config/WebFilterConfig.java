/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @version 2025.6.0
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
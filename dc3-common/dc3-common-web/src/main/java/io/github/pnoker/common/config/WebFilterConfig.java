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

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.utils.HmacAuthSigner;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.RequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Web Filter Configuration Class
 * <p>
 * Configuration class for custom web filters in reactive applications. Configures user
 * header interception for request processing.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class WebFilterConfig {

    private final HmacAuthSigner hmacAuthSigner;

    /**
     * Custom user header interceptor filter
     *
     * @return WebFilter for intercepting and processing user headers
     */
    @Bean
    public WebFilter interceptor() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String user = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_USER);

            if (StringUtils.isEmpty(user)) {
                return chain.filter(exchange);
            }

            // When signing is enabled, every X-Auth-User must come with a valid signature.
            // Without this, any client able to reach a backend port directly can spoof a
            // tenant by crafting their own X-Auth-User header.
            if (hmacAuthSigner.isEnabled()) {
                String sign = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_SIGN);
                if (!hmacAuthSigner.verify(user, sign)) {
                    log.warn("Rejecting X-Auth-User with missing/invalid signature, Url: {}", request.getURI());
                    return writeUnauthorized(exchange);
                }
            }

            try {
                RequestHeader.UserHeader userHeader = JsonUtil.parseObject(user, RequestHeader.UserHeader.class);

                if (Objects.isNull(userHeader) || Objects.isNull(userHeader.getTenantId())
                        || Objects.isNull(userHeader.getUserId())) {
                    log.warn("Invalid user header: {}", JsonUtil.toJsonString(userHeader));
                    return chain.filter(exchange)
                            .contextWrite(context -> context.delete(RequestConstant.Key.USER_HEADER));
                } else {
                    log.debug("User header: {}", JsonUtil.toJsonString(userHeader));
                    return chain.filter(exchange)
                            .contextWrite(context -> context.put(RequestConstant.Key.USER_HEADER, userHeader));
                }
            } catch (Exception e) {
                log.warn("Rejecting request with malformed X-Auth-User header, Url: {}", request.getURI(), e);
                return writeUnauthorized(exchange);
            }
        };
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        DataBuffer buffer = response.bufferFactory()
                .wrap(JsonUtil.toJsonBytes(R.fail(RequestConstant.Message.INVALID_REQUEST)));
        return response.writeWith(Mono.just(buffer));
    }

}

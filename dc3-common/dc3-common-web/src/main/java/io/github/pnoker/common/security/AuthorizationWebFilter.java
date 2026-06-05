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

package io.github.pnoker.common.security;

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Authorization filter that runs after UserHeader extraction.
 * Rejects requests to non-public paths that lack a valid UserHeader in the Reactor context.
 * <p>
 * The UserHeader is placed in the context by WebFilterConfig during the authentication phase.
 * This filter merely asserts its presence for protected paths.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@AutoConfiguration
@ConditionalOnBean(PermissionProvider.class)
public class AuthorizationWebFilter {

    private static final Set<Pattern> PUBLIC_PATHS = Set.of(
            Pattern.compile(".*/auth/token/salt"),
            Pattern.compile(".*/auth/token/generate"),
            Pattern.compile(".*/actuator(/.*)?"),
            Pattern.compile(".*/health(/.*)?")
    );

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    public WebFilter authorizationFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            return Mono.deferContextual(ctx -> {
                if (ctx.hasKey(RequestConstant.Key.USER_HEADER)) {
                    return chain.filter(exchange);
                }
                log.warn("Authorization rejected — no UserHeader in context for path: {}", path);
                return writeUnauthorized(exchange);
            });
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(p -> p.matcher(path).matches());
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        DataBuffer buffer = response.bufferFactory()
                .wrap(JsonUtil.toJsonBytes(R.fail("Authentication required")));
        return response.writeWith(Mono.just(buffer));
    }
}

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

package io.github.pnoker.common.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for creating authentic gateway filter instances.
 * <p>
 * Declares {@code AbstractGatewayFilterFactory<Object>} rather than a dedicated config
 * type. The filter has no tunable options, and Spring Cloud Gateway binds a plain
 * {@code Object} config for an argument-less {@code filters: - Authentic} route entry.
 * A concrete config type would make javac synthesize a bridge {@code apply(Object)}
 * that casts to that type and throw {@link ClassCastException} at runtime when the
 * framework passes the {@code Object} instance; {@code Object} keeps {@code apply}
 * cast-free.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final AuthenticGatewayFilter authenticGatewayFilter;

    @Override
    public GatewayFilter apply(Object config) {
        return authenticGatewayFilter;
    }

}

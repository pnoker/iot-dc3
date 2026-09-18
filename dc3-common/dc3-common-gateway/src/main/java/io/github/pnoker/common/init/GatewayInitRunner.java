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
package io.github.pnoker.common.init;

import io.github.pnoker.common.gateway.mcp.McpGatewayProperties;
import io.github.pnoker.common.gateway.security.GatewayOAuthProperties;
import io.github.pnoker.common.gateway.security.OAuthTokenResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Gateway Component Scan Configuration for DC3 IoT Platform. Enables component scanning
 * for gateway-related beans (filters, services, etc.).
 *
 * @author pnoker
 * @since 2016.10.1
 */
@AutoConfiguration
@EnableConfigurationProperties({McpGatewayProperties.class, GatewayOAuthProperties.class})
@ComponentScan(basePackages = {"io.github.pnoker.common.gateway"})
public class GatewayInitRunner {

    /**
     * OAuth RS256 ticket resolver, instantiated only when {@code dc3.gateway.oauth.enabled}
     * is true; when absent, the Authentic filter falls through to classic login tickets.
     *
     * @param properties gateway-side OAuth verification configuration
     * @return the resolver
     */
    @Bean
    @ConditionalOnProperty(prefix = "dc3.gateway.oauth", name = "enabled", havingValue = "true")
    public OAuthTokenResolver oauthTokenResolver(GatewayOAuthProperties properties) {
        return new OAuthTokenResolver(properties);
    }
}

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
package io.github.pnoker.gateway.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single-center topology route definitions (activate with
 * {@code DC3_GATEWAY_TOPOLOGY=single}).
 *
 * <p>Every api/v3 route proxies to the aggregated dc3-center-single service:
 * StripPrefix=3 removes api/v3/{svc} and PrefixPath=/single targets the single
 * service's shared base path, so the web frontend keeps its unchanged
 * /api/v3/** protocol while every domain resolves in one process.
 *
 * <p>The routes are written straight into {@link GatewayProperties} by a
 * post-processor: profile-specific YAML lists do not replace the distributed
 * block on this Boot/Cloud combination, and a custom
 * {@code RouteDefinitionLocator} loses the composite merge to the property
 * definitions. The bean itself is unconditional and the topology switch
 * lives inside the runtime logic (DC3_GATEWAY_TOPOLOGY=single env check):
 * both @Profile and @ConditionalOnProperty conditions are baked into the
 * image by native AOT at build time, while one image must serve both
 * topologies.
 * The public token endpoints stay before auth_route - the same ordering
 * contract as the distributed routes.
 *
 * @author pnoker
 */
@Configuration(proxyBeanMethods = false)
public class SingleGatewayRouteConfig {

    private static final String[] OAUTH_METADATA_PATHS = {
        "/.well-known/oauth-authorization-server", "/oauth2/jwks",
        "/oauth2/token", "/oauth2/revoke", "/oauth2/register"
    };
    private static final String[] TOKEN_PATHS = {
        "/api/v3/auth/token/salt", "/api/v3/auth/token/generate",
        "/api/v3/auth/token/change_password"
    };

    @Bean
    static BeanPostProcessor singleTopologyRoutesPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof GatewayProperties properties
                        && "single".equals(System.getenv("DC3_GATEWAY_TOPOLOGY"))) {
                    properties.setRoutes(buildDefinitions());
                }
                return bean;
            }
        };
    }

    private static List<RouteDefinition> buildDefinitions() {
        Map<String, String> env = System.getenv();
        String host = env.getOrDefault("CENTER_SINGLE_HOST", "dc3-center-single");
        String port = env.getOrDefault("DC3_SINGLE_PORT", "8100");
        URI uri = URI.create("http://" + host + ":" + port);
        List<RouteDefinition> routes = new ArrayList<>(7);

        // OAuth discovery and public token endpoints for MCP clients
        RouteDefinition oauthMetadata = new RouteDefinition();
        oauthMetadata.setId("oauth_metadata_route");
        oauthMetadata.setUri(uri);
        oauthMetadata.getPredicates().add(predicate("Path", OAUTH_METADATA_PATHS));
        oauthMetadata.getFilters().add(filter("PrefixPath", "/single"));
        routes.add(oauthMetadata);

        // OAuth authorization endpoint requires an existing DC3 login context
        RouteDefinition oauthAuthorize = new RouteDefinition();
        oauthAuthorize.setId("oauth_authorize_route");
        oauthAuthorize.setUri(uri);
        oauthAuthorize.getPredicates().add(predicate("Path", "/oauth2/authorize"));
        oauthAuthorize.getFilters().add(filter("PrefixPath", "/single"));
        oauthAuthorize.getFilters().add(filter("Authentic"));
        routes.add(oauthAuthorize);

        // Pre-authentication token endpoints MUST stay before auth_route.
        RouteDefinition authToken = new RouteDefinition();
        authToken.setId("auth_route_token");
        authToken.setUri(uri);
        authToken.getPredicates().add(predicate("Path", TOKEN_PATHS));
        authToken.getFilters().add(filter("StripPrefix", "3"));
        authToken.getFilters().add(filter("PrefixPath", "/single"));
        routes.add(authToken);

        for (String svc : new String[] {"auth", "manager", "data", "agentic"}) {
            RouteDefinition route = new RouteDefinition();
            route.setId(svc + "_route");
            route.setUri(uri);
            route.getPredicates().add(predicate("Path", "/api/v3/" + svc + "/**"));
            route.getFilters().add(filter("StripPrefix", "3"));
            route.getFilters().add(filter("PrefixPath", "/single"));
            route.getFilters().add(filter("Authentic"));
            routes.add(route);
        }
        return routes;
    }

    private static PredicateDefinition predicate(String name, String... args) {
        PredicateDefinition p = new PredicateDefinition();
        p.setName(name);
        for (int i = 0; i < args.length; i++) {
            p.getArgs().put("_genkey_" + i, args[i]);
        }
        return p;
    }

    private static FilterDefinition filter(String name, String... args) {
        FilterDefinition f = new FilterDefinition();
        f.setName(name);
        for (int i = 0; i < args.length; i++) {
            f.getArgs().put("_genkey_" + i, args[i]);
        }
        return f;
    }
}

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

import io.github.pnoker.common.security.GatewayJwtConverter;
import io.github.pnoker.common.security.PermissionMethods;
import io.github.pnoker.common.security.PermissionProvider;
import io.github.pnoker.common.utils.HmacAuthSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

/**
 * WebFlux security configuration.
 * <p>
 * Replaces the legacy WebFilterConfig + AuthorizationWebFilter with a standard
 * Spring Security Reactive pipeline: X-Auth-User → GatewayAuthenticationToken →
 * {@code @PreAuthorize} method-level authorization.
 *
 * <p>
 * Registered as an {@link AutoConfiguration} (see {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports})
 * so it is applied without center applications having to scan the
 * {@code io.github.pnoker.common.config} package.
 * <p>
 * The chain needs a {@link PermissionProvider}. The auth center contributes the
 * real {@code authPermissionProvider} (role/resource backed) via component scan;
 * every other web service (gateway, manager, data, agentic) has none, so this
 * class supplies a permissive {@link PermissionProvider.DefaultPermissionProvider}
 * fallback. That keeps the security chain — and its doc/actuator whitelist —
 * active everywhere. Without it those services fall back to Spring Boot's default
 * reactive security chain, which locks every endpoint (including
 * {@code /actuator/health} and the API docs) behind a generated password.
 * <p>
 * Not applied on the API gateway. The gateway is a Spring Cloud Gateway app
 * whose authentication is performed by {@code AuthenticGatewayFilter} (it reads
 * X-Auth-Tenant/Login/Token and injects X-Auth-User downstream). A
 * {@link SecurityWebFilterChain} here would run before routing and reject every
 * inbound {@code /api/v3/**} request, since the converter expects the
 * X-Auth-User header that only exists downstream. {@link ConditionalOnMissingClass}
 * on the gateway's marker class keeps this off the gateway; the gateway disables
 * Spring Security's default reactive chain via {@code spring.autoconfigure.exclude}.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@AutoConfiguration
@ConditionalOnMissingClass("org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping")
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebFluxSecurityConfig {

    /**
     * The {@code @perm} SpEL bean used by {@code @PreAuthorize("@perm.can(...)")}
     * on controllers. Lives in the (unscanned) {@code common.security} package, so
     * it must be registered explicitly here — otherwise every method-security
     * expression fails to evaluate (500) once method security is active.
     */
    @Bean("perm")
    @ConditionalOnMissingBean(name = "perm")
    public PermissionMethods permissionMethods(
            @Value("${spring.application.name:unknown}") String serviceName) {
        return new PermissionMethods(serviceName);
    }

    /**
     * Permissive fallback used by services that do not ship the auth module's
     * {@code authPermissionProvider}. Backs off automatically when a real
     * provider is present (see {@link PermissionProvider.DefaultPermissionProvider}'s
     * {@code @ConditionalOnMissingBean}).
     */
    @Bean
    @ConditionalOnMissingBean(PermissionProvider.class)
    public PermissionProvider defaultPermissionProvider() {
        return new PermissionProvider.DefaultPermissionProvider();
    }

    @Bean
    public GatewayJwtConverter gatewayJwtConverter(
            HmacAuthSigner hmacAuthSigner,
            PermissionProvider permissionProvider) {
        return new GatewayJwtConverter(hmacAuthSigner, permissionProvider);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            GatewayJwtConverter converter) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(spec -> spec
                        // NOTE: spring.webflux.base-path (/auth, /manager, ...) is stripped
                        // before the security chain sees the path, so matchers must NOT include
                        // it. The auth center serves the token endpoints at /token/** under its
                        // /auth base-path; the chain matches the post-strip path /token/salt.
                        .pathMatchers(HttpMethod.POST, "/token/salt").permitAll()
                        .pathMatchers(HttpMethod.POST, "/token/generate").permitAll()
                        .pathMatchers(HttpMethod.GET, "/mcp_tools").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/health/**").permitAll()
                        // OpenAPI / Swagger UI documentation endpoints. springdoc itself
                        // is disabled in the production profile (application-pro.yml), so
                        // permitting these paths unconditionally exposes nothing there.
                        .pathMatchers("/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(authenticationWebFilter(converter),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(spec -> spec
                        .authenticationEntryPoint(
                                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(
                                new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                )
                .build();
    }

    private AuthenticationWebFilter authenticationWebFilter(GatewayJwtConverter converter) {
        ReactiveAuthenticationManager noOpAuthManager = Mono::just;

        AuthenticationWebFilter filter = new AuthenticationWebFilter(noOpAuthManager);
        filter.setServerAuthenticationConverter(converter);
        filter.setSecurityContextRepository(
                NoOpServerSecurityContextRepository.getInstance());
        return filter;
    }
}

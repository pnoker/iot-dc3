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
import io.github.pnoker.common.security.PermissionProvider;
import io.github.pnoker.common.utils.HmacAuthSigner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebFluxSecurityConfig {

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
                        .pathMatchers(HttpMethod.POST, "/auth/token/salt").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/token/generate").permitAll()
                        .pathMatchers(HttpMethod.GET, "/mcp_tools").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/health/**").permitAll()
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

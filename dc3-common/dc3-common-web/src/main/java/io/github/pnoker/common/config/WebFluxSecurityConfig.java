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

import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.facade.api.PermissionFacade;
import io.github.pnoker.common.security.FacadePermissionProvider;
import io.github.pnoker.common.security.GatewayJwtConverter;
import io.github.pnoker.common.security.PermissionMethods;
import io.github.pnoker.common.security.PermissionProvider;
import io.github.pnoker.common.utils.HmacAuthSigner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebFlux security configuration.
 * <p>
 * Replaces the legacy WebFilterConfig + AuthorizationWebFilter with a standard
 * Spring Security Reactive pipeline: X-Auth-Principal -> GatewayAuthenticationToken ->
 * {@code @PreAuthorize} method-level authorization.
 *
 * <p>
 * Registered as an {@link AutoConfiguration} (see {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports})
 * so it is applied without center applications having to scan the
 * {@code io.github.pnoker.common.config} package.
 * <p>
 * The chain needs a {@link PermissionProvider}. The auth center contributes the
 * real {@code authPermissionProvider} (role/resource backed) via component scan.
 * Other center services use a facade-backed provider that queries the auth center over
 * gRPC. If neither exists, the default provider fails closed so the chain remains active
 * without granting any controller permission.
 * <p>
 * Not applied on the API gateway. The gateway is a Spring Cloud Gateway app
 * whose authentication is performed by {@code AuthenticGatewayFilter} (it reads
 * X-Auth-Tenant/Login/Token and injects X-Auth-Principal downstream). A
 * {@link SecurityWebFilterChain} here would run before routing and reject every
 * inbound {@code /api/v3/**} request, since the converter expects the
 * X-Auth-Principal header that only exists downstream. {@link ConditionalOnMissingClass}
 * on the gateway's marker class keeps this off the gateway; the gateway disables
 * Spring Security's default reactive chain via {@code spring.autoconfigure.exclude}.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "io.github.pnoker.common.init.AuthInitRunner",
        "io.github.pnoker.common.facade.grpc.config.GrpcFacadeAutoConfiguration",
        "io.github.pnoker.common.facade.local.config.LocalFacadeAuthAutoConfiguration"
})
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
     * Facade-backed provider for non-auth services. If no facade exists either, return a
     * fail-closed default provider so method security still returns 403 instead of
     * falling back to Spring Boot's generated-password security chain.
     */
    @Bean
    @ConditionalOnMissingBean(PermissionProvider.class)
    public PermissionProvider defaultPermissionProvider(ObjectProvider<PermissionFacade> permissionFacadeProvider) {
        PermissionFacade permissionFacade = permissionFacadeProvider.getIfAvailable();
        if (permissionFacade != null) {
            return new FacadePermissionProvider(permissionFacade);
        }
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
            GatewayJwtConverter converter,
            HmacAuthSigner hmacAuthSigner,
            Environment environment,
            @Value("${dc3.docs.public-enabled:true}") boolean docsPublicEnabled,
            @Value("${dc3.docs.internal-signature-enabled:false}") boolean docsInternalSignatureEnabled,
            @Value("${dc3.oauth.dcr.enabled:false}") boolean oauthDcrEnabled) {

        validateDocsSecurity(environment, hmacAuthSigner, docsInternalSignatureEnabled);

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
                        .pathMatchers(HttpMethod.POST, "/token/change_password").permitAll()
                        .pathMatchers(HttpMethod.GET, McpConstant.WELL_KNOWN_AUTHORIZATION_SERVER).permitAll()
                        .pathMatchers(HttpMethod.GET, McpConstant.OAUTH2_JWKS).permitAll()
                        .pathMatchers(HttpMethod.POST, McpConstant.OAUTH2_TOKEN).permitAll()
                        .pathMatchers(HttpMethod.POST, McpConstant.OAUTH2_REVOKE).permitAll()
                        // Dynamic Client Registration (RFC 7591) for external MCP clients. Open in
                        // dev/test for convenience; in pre/pro it stays closed unless explicitly
                        // enabled via dc3.oauth.dcr.enabled=true. Admin UI registration is unaffected
                        // — it goes through the @perm.can('mcp','add')-gated management endpoint.
                        .pathMatchers(HttpMethod.POST, McpConstant.OAUTH2_REGISTER)
                        .access((authentication, context) -> dcrAccess(environment, oauthDcrEnabled))
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/health/**").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/v3/api-docs.yaml")
                        .access((authentication, context) -> docsAccess(context.getExchange(), environment,
                                hmacAuthSigner, docsPublicEnabled, docsInternalSignatureEnabled))
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**")
                        .access((authentication, context) -> docsAccess(context.getExchange(), environment,
                                hmacAuthSigner, docsPublicEnabled, docsInternalSignatureEnabled))
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

    private Mono<AuthorizationResult> docsAccess(ServerWebExchange exchange,
                                                 Environment environment,
                                                 HmacAuthSigner hmacAuthSigner,
                                                 boolean docsPublicEnabled,
                                                 boolean docsInternalSignatureEnabled) {
        if (!isProtectedEnvironment(environment) && docsPublicEnabled) {
            return Mono.just(new AuthorizationDecision(true));
        }
        if (!docsInternalSignatureEnabled || !hmacAuthSigner.isEnabled()) {
            return Mono.just(new AuthorizationDecision(false));
        }
        String caller = exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.X_INTERNAL_CALLER);
        String timestamp = exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.X_INTERNAL_TIMESTAMP);
        String nonce = exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.X_INTERNAL_NONCE);
        String sign = exchange.getRequest().getHeaders().getFirst(RequestConstant.Header.X_INTERNAL_SIGN);
        boolean allowed = isFresh(timestamp) && Objects.nonNull(caller) && Objects.nonNull(nonce)
                && hmacAuthSigner.verify(internalDocsPayload(caller, timestamp, nonce,
                exchange.getRequest().getPath().pathWithinApplication().value()), sign);
        return Mono.just(new AuthorizationDecision(allowed));
    }

    private Mono<AuthorizationResult> dcrAccess(Environment environment, boolean dcrEnabled) {
        // Open in non-protected environments; in pre/pro closed unless explicitly enabled.
        boolean allow = !isProtectedEnvironment(environment) || dcrEnabled;
        return Mono.just(new AuthorizationDecision(allow));
    }

    private void validateDocsSecurity(Environment environment, HmacAuthSigner hmacAuthSigner,
                                      boolean docsInternalSignatureEnabled) {
        boolean apiDocsEnabled = environment.getProperty("springdoc.api-docs.enabled", Boolean.class, true);
        if (!isProtectedEnvironment(environment) || !apiDocsEnabled) {
            return;
        }
        if (!docsInternalSignatureEnabled || !hmacAuthSigner.isEnabled()) {
            throw new IllegalStateException("springdoc api-docs in pre/pro requires "
                    + EnvironmentConstant.DOCS_INTERNAL_SIGNATURE_ENABLED
                    + "=true and a configured internal HMAC secret");
        }
    }

    private boolean isFresh(String timestamp) {
        try {
            long epochMs = Long.parseLong(timestamp);
            long diffMs = Math.abs(Instant.now().toEpochMilli() - epochMs);
            return diffMs <= RequestConstant.DEFAULT_INTERNAL_SIGNATURE_TTL_MS;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String internalDocsPayload(String caller, String timestamp, String nonce, String path) {
        return caller + '\n' + timestamp + '\n' + nonce + '\n' + path;
    }

    private boolean isProtectedEnvironment(Environment environment) {
        Set<String> names = Arrays.stream(environment.getActiveProfiles())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(profile -> !profile.isEmpty())
                .collect(Collectors.toSet());
        String springEnv = environment.getProperty(EnvironmentConstant.SPRING_ENV);
        if (Objects.nonNull(springEnv) && !springEnv.isBlank()) {
            names.add(springEnv.trim());
        }
        return names.contains(EnvironmentConstant.ENV_PRE) || names.contains(EnvironmentConstant.ENV_PRO);
    }
}

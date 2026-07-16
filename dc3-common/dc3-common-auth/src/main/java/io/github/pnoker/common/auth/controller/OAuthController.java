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

package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.annotation.PublicEndpoint;
import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.auth.biz.impl.OAuthMcpRuntimeServiceImpl.OAuthProtocolException;
import io.github.pnoker.common.auth.entity.builder.OAuthClientBuilder;
import io.github.pnoker.common.auth.entity.vo.OAuthClientRegistrationRequestVO;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.utils.JsonUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth 2.1 endpoints used by MCP clients.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@RestController
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthMcpRuntimeService oauthMcpRuntimeService;
    private final OAuthClientBuilder oauthClientBuilder;

    /**
     * Publish the OAuth 2.1 authorization server metadata for MCP client discovery.
     *
     * @return the authorization server metadata (issuer, token and registration endpoints)
     */
    @Operation(summary = "Get Authorization Server Metadata", description = "Publish the OAuth 2.1 authorization server metadata at the well-known discovery endpoint so MCP clients can resolve issuer, token and registration URLs.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PublicEndpoint
    @GetMapping(McpConstant.WELL_KNOWN_AUTHORIZATION_SERVER)
    public Mono<Map<String, Object>> authorizationServerMetadata() {
        return Mono.fromSupplier(oauthMcpRuntimeService::authorizationServerMetadata);
    }

    /**
     * Publish the JSON Web Key Set used to verify issued tokens.
     *
     * @return the JWKS as a map of key descriptors
     */
    @Operation(summary = "Get JWKS", description = "Publish the JSON Web Key Set (JWKS) so MCP clients and resource servers can verify signatures on issued access and ID tokens.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PublicEndpoint
    @GetMapping(McpConstant.OAUTH2_JWKS)
    public Mono<Map<String, Object>> jwks() {
        return Mono.fromSupplier(oauthMcpRuntimeService::jwks);
    }

    /**
     * Register an OAuth client dynamically for MCP access.
     *
     * @param request       client registration payload (grant types, redirect URIs, scopes)
     * @param principalJson optional serialized principal header that scopes ownership of the new client
     * @return a 201 response carrying the new client id and one-time secret; OAuth protocol errors map to the spec status
     */
    @Operation(summary = "Register OAuth Client", description = "Dynamically register an OAuth client for MCP access from a JSON request body, optionally scoped by the X-Auth-Principal header; returns the client id and a one-time secret.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PublicEndpoint
    @PostMapping(value = McpConstant.OAUTH2_REGISTER, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> register(
            @RequestBody OAuthClientRegistrationRequestVO request,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = RequestConstant.Header.X_AUTH_PRINCIPAL, required = false) String principalJson) {
        return Mono.<ResponseEntity<?>>fromSupplier(() -> {
                    if (oauthClientBuilder.isUnknownClientType(request)) {
                        throw new OAuthProtocolException(HttpStatus.BAD_REQUEST.value(), "invalid_client_metadata",
                                "unsupported client_type");
                    }
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(oauthMcpRuntimeService.registerClient(oauthClientBuilder.buildBOByRequestVO(request),
                                    parsePrincipal(principalJson)));
                })
                .onErrorResume(OAuthProtocolException.class, this::oauthAnyError);
    }

    /**
     * Handle the OAuth 2.1 authorization endpoint with PKCE, redirecting the user agent.
     *
     * @param params        OAuth 2.1 authorization request parameters (client_id, redirect_uri, response_type, scope, PKCE, state)
     * @param principalJson optional serialized principal header identifying the consenting user
     * @return a 302 redirect to the consent or callback location; OAuth protocol errors map to the spec status
     */
    @Operation(summary = "Authorize", description = "OAuth 2.1 authorization endpoint: validate the authorization request (including PKCE) and redirect the user agent to the consent flow or callback location.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PublicEndpoint
    @GetMapping(McpConstant.OAUTH2_AUTHORIZE)
    public Mono<ResponseEntity<Map<String, Object>>> authorize(
            @Parameter(description = "OAuth 2.1 authorization request parameters. Required fields: client_id, redirect_uri, response_type (must be 'code'), scope, and code_challenge / code_challenge_method (PKCE). Optional: state (recommended for CSRF protection).", example = "client_id=my-client&response_type=code&redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback&scope=openid&state=xyz&code_challenge=abc123&code_challenge_method=S256") @RequestParam MultiValueMap<String, String> params,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = RequestConstant.Header.X_AUTH_PRINCIPAL, required = false) String principalJson) {
        return Mono.fromSupplier(() -> {
            URI location = oauthMcpRuntimeService.authorize(firstValues(params), parsePrincipal(principalJson));
            return ResponseEntity.status(HttpStatus.FOUND).location(location).<Map<String, Object>>build();
        }).onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    /**
     * Exchange an authorization code (or supported grant) for access and refresh tokens.
     *
     * @param body                form-encoded token request parameters (grant_type, code, redirect_uri, code_verifier, etc.)
     * @param authorizationHeader optional HTTP Basic client credentials
     * @return a 200 response carrying the token JSON; OAuth protocol errors map to the spec status
     */
    @Operation(summary = "Issue Token", description = "OAuth 2.1 token endpoint: exchange an authorization code (or other supported grant) for access and refresh tokens using a form-encoded body, optionally authenticating the client via the Authorization header.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PublicEndpoint
    @PostMapping(value = McpConstant.OAUTH2_TOKEN, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> token(
            ServerWebExchange exchange,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return exchange.getFormData()
                .map(this::firstValues)
                .map(form -> ResponseEntity.ok(oauthMcpRuntimeService.token(form, authorizationHeader)))
                .onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    /**
     * Revoke a previously issued access or refresh token.
     *
     * @param body                form-encoded revocation parameters (token and token_type_hint)
     * @param authorizationHeader optional HTTP Basic client credentials
     * @return a 200 response confirming the revocation; OAuth protocol errors map to the spec status
     */
    @Operation(summary = "Revoke Token", description = "OAuth 2.1 revocation endpoint: invalidate a previously issued access or refresh token from a form-encoded body, optionally authenticating the client via the Authorization header.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false"),
                    @ExtensionProperty(name = "hidden", value = "true")
            }))
    @PublicEndpoint
    @PostMapping(value = McpConstant.OAUTH2_REVOKE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> revoke(
            ServerWebExchange exchange,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return exchange.getFormData()
                .map(this::firstValues)
                .map(form -> ResponseEntity.ok(oauthMcpRuntimeService.revoke(form, authorizationHeader)))
                .onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    /**
     * Build an OAuth error response entity carrying the error code and description at the
     * exception's status code.
     *
     * @param exception the OAuth protocol exception
     * @return the error response entity
     */
    private Mono<ResponseEntity<Map<String, Object>>> oauthError(OAuthProtocolException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(McpConstant.Field.ERROR, exception.getError());
        body.put(McpConstant.Field.ERROR_DESCRIPTION, exception.getDescription());
        return Mono.just(ResponseEntity.status(exception.getStatusCode()).body(body));
    }

    private Mono<ResponseEntity<?>> oauthAnyError(OAuthProtocolException exception) {
        return oauthError(exception).map(response -> response);
    }

    /**
     * Collapse a multi-value map to a single-value map, keeping the first value of each
     * key (empty string when none).
     *
     * @param values the multi-value map
     * @return the collapsed single-value map
     */
    private Map<String, String> firstValues(MultiValueMap<String, String> values) {
        Map<String, String> map = new LinkedHashMap<>();
        values.forEach((key, value) -> map.put(key, value.isEmpty() ? "" : value.get(0)));
        return map;
    }

    /**
     * Deserialize the principal header JSON, returning null when blank.
     *
     * @param principalJson the principal header JSON string
     * @return the parsed principal header, or null
     */
    private RequestHeader.PrincipalHeader parsePrincipal(String principalJson) {
        if (StringUtils.isBlank(principalJson)) {
            return null;
        }
        return JsonUtil.parseObject(principalJson, RequestHeader.PrincipalHeader.class);
    }

}

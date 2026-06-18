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

import io.github.pnoker.common.auth.biz.OAuthMcpRuntimeService;
import io.github.pnoker.common.auth.biz.impl.OAuthMcpRuntimeServiceImpl.OAuthProtocolException;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.OAuthClientRegistrationRequestDTO;
import io.github.pnoker.common.utils.JsonUtil;
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
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    @GetMapping(McpConstant.WELL_KNOWN_AUTHORIZATION_SERVER)
    public Mono<Map<String, Object>> authorizationServerMetadata() {
        return Mono.fromSupplier(oauthMcpRuntimeService::authorizationServerMetadata);
    }

    @GetMapping(McpConstant.OAUTH2_JWKS)
    public Mono<Map<String, Object>> jwks() {
        return Mono.fromSupplier(oauthMcpRuntimeService::jwks);
    }

    @PostMapping(value = McpConstant.OAUTH2_REGISTER, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> register(
            @RequestBody OAuthClientRegistrationRequestDTO request,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = RequestConstant.Header.X_AUTH_PRINCIPAL, required = false) String principalJson) {
        return Mono.<ResponseEntity<?>>fromSupplier(() -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(oauthMcpRuntimeService.registerClient(request, parsePrincipal(principalJson))))
                .onErrorResume(OAuthProtocolException.class, this::oauthAnyError);
    }

    @GetMapping(McpConstant.OAUTH2_AUTHORIZE)
    public Mono<ResponseEntity<Map<String, Object>>> authorize(
            @Parameter(description = "OAuth authorization request parameters including client_id, redirect_uri, response_type, scope, and state.") @RequestParam MultiValueMap<String, String> params,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = RequestConstant.Header.X_AUTH_PRINCIPAL, required = false) String principalJson) {
        return Mono.fromSupplier(() -> {
            URI location = oauthMcpRuntimeService.authorize(firstValues(params), parsePrincipal(principalJson));
            return ResponseEntity.status(HttpStatus.FOUND).location(location).<Map<String, Object>>build();
        }).onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    @PostMapping(value = McpConstant.OAUTH2_TOKEN, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> token(
            @RequestBody Mono<MultiValueMap<String, String>> body,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return body.map(this::firstValues)
                .map(form -> ResponseEntity.ok(oauthMcpRuntimeService.token(form, authorizationHeader)))
                .onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    @PostMapping(value = McpConstant.OAUTH2_REVOKE, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> revoke(
            @RequestBody Mono<MultiValueMap<String, String>> body,
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return body.map(this::firstValues)
                .map(form -> ResponseEntity.ok(oauthMcpRuntimeService.revoke(form, authorizationHeader)))
                .onErrorResume(OAuthProtocolException.class, this::oauthError);
    }

    private Mono<ResponseEntity<Map<String, Object>>> oauthError(OAuthProtocolException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(McpConstant.Field.ERROR, exception.getError());
        body.put(McpConstant.Field.ERROR_DESCRIPTION, exception.getDescription());
        return Mono.just(ResponseEntity.status(exception.getStatusCode()).body(body));
    }

    private Mono<ResponseEntity<?>> oauthAnyError(OAuthProtocolException exception) {
        return oauthError(exception).map(response -> response);
    }

    private Map<String, String> firstValues(MultiValueMap<String, String> values) {
        Map<String, String> map = new LinkedHashMap<>();
        values.forEach((key, value) -> map.put(key, value.isEmpty() ? "" : value.get(0)));
        return map;
    }

    private RequestHeader.PrincipalHeader parsePrincipal(String principalJson) {
        if (StringUtils.isBlank(principalJson)) {
            return null;
        }
        return JsonUtil.parseObject(principalJson, RequestHeader.PrincipalHeader.class);
    }

}

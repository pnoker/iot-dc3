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
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthConsentRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import java.time.LocalDateTime;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Complete reactive persistence port for OAuth and MCP state. */
public interface ReactiveOAuthMcpStore extends ReactiveMcpRuntimeStore {
    /** Load the client for the request. */
    Mono<OAuthRegisteredClientRecord> getClient(String clientId);

    /** Insert one client and emit the stored row. */
    Mono<Integer> insertClient(OAuthRegisteredClientRecord client);

    /** List o auth mcps matched by owner. */
    Flux<OAuthRegisteredClientRecord> listClientsByOwner(Long ownerPrincipalId, Long tenantId);

    /** Resolve the o auth mcp by its code hash. */
    Mono<OAuthAuthorizationRecord> getAuthorizationByCodeHash(String codeHash);

    /** Resolve the o auth mcp by its refresh token hash. */
    Mono<OAuthAuthorizationRecord> getAuthorizationByRefreshTokenHash(String refreshHash);

    /** Resolve the o auth mcp by its previous refresh token hash. */
    Mono<OAuthAuthorizationRecord> getAuthorizationByPreviousRefreshTokenHash(String previousRefreshHash);

    /** Insert one authorization and emit the stored row. */
    Mono<Integer> insertAuthorization(OAuthAuthorizationRecord authorization);

    /** Load the consent scoped to the tenant by id. */
    Mono<OAuthConsentRecord> getConsent(Long registeredClientId, Long principalId, Long tenantId);

    /** Save the consent, inserting or updating as needed. */
    Mono<Integer> upsertConsent(OAuthConsentRecord consent);

    /** Activate the authorization's tokens when the id and code hash still match. */
    Mono<Integer> activateAuthorizationTokens(
            Long id,
            String codeHash,
            String accessTokenJti,
            LocalDateTime accessIssued,
            LocalDateTime accessExpires,
            String refreshHash,
            String previousRefreshHash,
            LocalDateTime refreshIssued,
            LocalDateTime refreshExpires,
            String tokenClaims);

    /** Exchange the authorization code for tokens when the code hash still matches. */
    Mono<Integer> activateAuthorizationCode(
            String expectedCodeHash,
            Long id,
            String accessTokenJti,
            LocalDateTime accessIssued,
            LocalDateTime accessExpires,
            String refreshHash,
            String previousRefreshHash,
            LocalDateTime refreshIssued,
            LocalDateTime refreshExpires,
            String tokenClaims);

    /** Rotate the refresh token when the previous hash still matches. */
    Mono<Integer> rotateAuthorizationRefreshToken(
            Long id,
            String expectedRefreshHash,
            String accessTokenJti,
            LocalDateTime accessIssued,
            LocalDateTime accessExpires,
            String refreshHash,
            LocalDateTime refreshIssued,
            LocalDateTime refreshExpires,
            String tokenClaims);

    /** Revoke the authorization owning the access token jti. */
    Mono<Integer> revokeAuthorizationByAccessTokenJti(String jti, String reason, LocalDateTime revokedTime);

    /** Revoke the authorization owning the refresh token hash. */
    Mono<Integer> revokeAuthorizationByRefreshTokenHash(String refreshHash, String reason, LocalDateTime revokedTime);

    /** Load the active connection scoped to the tenant by id. */
    Mono<McpConnectionRecord> getActiveConnection(String clientId, Long principalId, Long tenantId, String grantType);

    /** Stream connections matching the request. */
    Flux<McpConnectionRecord> listConnections(Long tenantId, Long principalId);

    /** Insert one connection and emit the stored row. */
    Mono<Integer> insertConnection(McpConnectionRecord connection);

    /** Revoke one MCP connection and its bound tokens. */
    Mono<Integer> revokeConnection(Long id, Long tenantId, Long principalId, LocalDateTime revokeTime);

    /** Stream registry tool candidates matching the request. */
    Flux<McpToolRecord> listRegistryToolCandidates();

    /** List o auth mcps matched by permission codes. */
    Flux<McpToolRecord> listEnabledToolsByPermissionCodes(Collection<String> permissionCodes);

    /** Load the tool for the request. */
    Mono<McpToolRecord> getTool(String toolId);
    /** Atomically insert or refresh a catalog entry by its stable tool id. */
    Mono<Integer> upsertTool(McpToolRecord tool);

    /** Insert one tool and emit the stored row. */
    Mono<Integer> insertTool(McpToolRecord tool);

    /** Update one tool and emit the updated row. */
    Mono<Integer> updateTool(McpToolRecord tool);

    /** Delete the connection tools. */
    Mono<Integer> deleteConnectionTools(Long connectionId);

    /** Stream connection tool ids matching the request. */
    Flux<String> listConnectionToolIds(Long connectionId, Long tenantId, Long principalId);

    /** Insert one connection tool and emit the stored row. */
    Mono<Integer> insertConnectionTool(Long id, Long connectionId, String toolId, Long operatorId, String operatorName);
}

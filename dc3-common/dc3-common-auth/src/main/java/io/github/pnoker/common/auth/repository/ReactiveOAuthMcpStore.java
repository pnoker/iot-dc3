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
    Mono<OAuthRegisteredClientRecord> getClient(String clientId);

    Mono<Integer> insertClient(OAuthRegisteredClientRecord client);

    Flux<OAuthRegisteredClientRecord> listClientsByOwner(Long ownerPrincipalId, Long tenantId);

    Mono<OAuthAuthorizationRecord> getAuthorizationByCodeHash(String codeHash);

    Mono<OAuthAuthorizationRecord> getAuthorizationByRefreshTokenHash(String refreshHash);

    Mono<OAuthAuthorizationRecord> getAuthorizationByPreviousRefreshTokenHash(String previousRefreshHash);

    Mono<Integer> insertAuthorization(OAuthAuthorizationRecord authorization);

    Mono<OAuthConsentRecord> getConsent(Long registeredClientId, Long principalId, Long tenantId);

    Mono<Integer> upsertConsent(OAuthConsentRecord consent);

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

    Mono<Integer> revokeAuthorizationByAccessTokenJti(String jti, String reason, LocalDateTime revokedTime);

    Mono<Integer> revokeAuthorizationByRefreshTokenHash(String refreshHash, String reason, LocalDateTime revokedTime);

    Mono<McpConnectionRecord> getActiveConnection(String clientId, Long principalId, Long tenantId, String grantType);

    Flux<McpConnectionRecord> listConnections(Long tenantId, Long principalId);

    Mono<Integer> insertConnection(McpConnectionRecord connection);

    Mono<Integer> revokeConnection(Long id, Long tenantId, Long principalId, LocalDateTime revokeTime);

    Flux<McpToolRecord> listRegistryToolCandidates();

    Flux<McpToolRecord> listEnabledToolsByPermissionCodes(Collection<String> permissionCodes);

    Mono<McpToolRecord> getTool(String toolId);
    /** Atomically insert or refresh a catalog entry by its stable tool id. */
    Mono<Integer> upsertTool(McpToolRecord tool);

    Mono<Integer> insertTool(McpToolRecord tool);

    Mono<Integer> updateTool(McpToolRecord tool);

    Mono<Integer> deleteConnectionTools(Long connectionId);

    Flux<String> listConnectionToolIds(Long connectionId, Long tenantId, Long principalId);

    Mono<Integer> insertConnectionTool(Long id, Long connectionId, String toolId, Long operatorId, String operatorName);
}

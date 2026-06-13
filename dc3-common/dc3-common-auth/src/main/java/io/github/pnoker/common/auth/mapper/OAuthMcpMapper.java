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

package io.github.pnoker.common.auth.mapper;

import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SQL boundary for OAuth and MCP runtime state.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface OAuthMcpMapper {

    OAuthRegisteredClientRecord selectClientByClientId(@Param("clientId") String clientId);

    int insertClient(OAuthRegisteredClientRecord client);

    List<OAuthRegisteredClientRecord> listClientsByOwner(@Param("ownerPrincipalId") Long ownerPrincipalId,
                                                         @Param("tenantId") Long tenantId);

    OAuthAuthorizationRecord selectAuthorizationByCodeHash(@Param("codeHash") String codeHash);

    OAuthAuthorizationRecord selectAuthorizationByAccessTokenJti(@Param("jti") String jti);

    OAuthAuthorizationRecord selectAuthorizationByRefreshTokenHash(@Param("refreshHash") String refreshHash);

    int insertAuthorization(OAuthAuthorizationRecord authorization);

    int activateAuthorizationTokens(@Param("id") Long id,
                                    @Param("codeHash") String codeHash,
                                    @Param("accessTokenJti") String accessTokenJti,
                                    @Param("accessIssued") LocalDateTime accessIssued,
                                    @Param("accessExpires") LocalDateTime accessExpires,
                                    @Param("refreshHash") String refreshHash,
                                    @Param("refreshIssued") LocalDateTime refreshIssued,
                                    @Param("refreshExpires") LocalDateTime refreshExpires,
                                    @Param("tokenClaims") String tokenClaims);

    int revokeAuthorizationByAccessTokenJti(@Param("jti") String jti,
                                            @Param("reason") String reason,
                                            @Param("revokedTime") LocalDateTime revokedTime);

    int revokeAuthorizationByRefreshTokenHash(@Param("refreshHash") String refreshHash,
                                              @Param("reason") String reason,
                                              @Param("revokedTime") LocalDateTime revokedTime);

    McpConnectionRecord selectConnectionById(@Param("id") Long id);

    McpConnectionRecord selectActiveConnection(@Param("clientId") String clientId,
                                               @Param("principalId") Long principalId,
                                               @Param("tenantId") Long tenantId,
                                               @Param("grantType") String grantType);

    List<McpConnectionRecord> listConnectionsByPrincipal(@Param("tenantId") Long tenantId,
                                                         @Param("principalId") Long principalId);

    int insertConnection(McpConnectionRecord connection);

    int revokeConnection(@Param("id") Long id,
                         @Param("tenantId") Long tenantId,
                         @Param("principalId") Long principalId,
                         @Param("revokeTime") LocalDateTime revokeTime);

    List<McpToolRecord> listRegistryToolCandidates();

    McpToolRecord selectToolByToolId(@Param("toolId") String toolId);

    List<McpToolRecord> listToolCatalog(@Param("keyword") String keyword,
                                        @Param("riskLevel") String riskLevel,
                                        @Param("limit") int limit);

    List<McpAuditCommand> listAudit(@Param("tenantId") Long tenantId,
                                    @Param("principalId") Long principalId,
                                    @Param("toolId") String toolId,
                                    @Param("status") String status,
                                    @Param("riskLevel") String riskLevel,
                                    @Param("limit") int limit);

    int insertTool(McpToolRecord tool);

    int updateTool(McpToolRecord tool);

    List<McpToolRecord> listVisibleTools(@Param("tenantId") Long tenantId,
                                         @Param("principalId") Long principalId,
                                         @Param("connectionId") Long connectionId,
                                         @Param("allowHighRisk") boolean allowHighRisk);

    McpToolRecord selectVisibleToolByName(@Param("tenantId") Long tenantId,
                                          @Param("principalId") Long principalId,
                                          @Param("connectionId") Long connectionId,
                                          @Param("toolName") String toolName,
                                          @Param("allowHighRisk") boolean allowHighRisk);

    int updateConnectionLastUsed(@Param("id") Long id, @Param("lastUsedTime") LocalDateTime lastUsedTime);

    int deleteConnectionTools(@Param("connectionId") Long connectionId);

    List<String> listConnectionToolIds(@Param("connectionId") Long connectionId,
                                       @Param("tenantId") Long tenantId,
                                       @Param("principalId") Long principalId);

    int insertConnectionTool(@Param("id") Long id,
                             @Param("connectionId") Long connectionId,
                             @Param("toolId") String toolId,
                             @Param("operatorId") Long operatorId,
                             @Param("operatorName") String operatorName);

    int insertAudit(McpAuditCommand command);

}

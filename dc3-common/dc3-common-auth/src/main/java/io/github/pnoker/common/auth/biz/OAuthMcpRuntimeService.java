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

package io.github.pnoker.common.auth.biz;

import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionAddVO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionVO;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.common.auth.entity.vo.OAuthClientVO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.McpAuditCommandDTO;
import io.github.pnoker.common.entity.dto.McpIntrospectResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.McpToolResolveResponseDTO;
import io.github.pnoker.common.entity.dto.OAuthClientRegistrationRequestDTO;
import io.github.pnoker.common.entity.dto.OAuthClientRegistrationResponseDTO;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OAuth 2.1 and MCP runtime service.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
public interface OAuthMcpRuntimeService {

    Map<String, Object> authorizationServerMetadata();

    Map<String, Object> jwks();

    OAuthClientRegistrationResponseDTO registerClient(OAuthClientRegistrationRequestDTO request,
                                                      RequestHeader.PrincipalHeader principalHeader);

    List<OAuthClientVO> listClients(RequestHeader.PrincipalHeader principalHeader);

    URI authorize(Map<String, String> params, RequestHeader.PrincipalHeader principalHeader);

    Map<String, Object> token(Map<String, String> form, String authorizationHeader);

    McpIntrospectResponseDTO introspect(String token);

    Map<String, Object> revoke(Map<String, String> form, String authorizationHeader);

    int refreshToolCatalog();

    List<McpToolVO> listToolCatalog(String keyword, String riskLevel, int limit);

    List<McpConnectionVO> listConnections(RequestHeader.PrincipalHeader principalHeader);

    McpConnectionVO createConnection(McpConnectionAddVO connection,
                                     RequestHeader.PrincipalHeader principalHeader);

    void revokeConnection(Long connectionId, RequestHeader.PrincipalHeader principalHeader);

    void replaceConnectionTools(Long connectionId, List<String> toolIds,
                                RequestHeader.PrincipalHeader principalHeader);

    List<String> listConnectionToolIds(Long connectionId, RequestHeader.PrincipalHeader principalHeader);

    List<McpToolDefinitionDTO> listVisibleTools(Long tenantId, Long principalId, Long connectionId,
                                                Set<String> scopes);

    McpToolResolveResponseDTO resolveVisibleTool(Long tenantId, Long principalId, Long connectionId, String toolName,
                                                 Set<String> scopes);

    McpToolAuthorizeResponseDTO authorizeToolCall(McpToolAuthorizeRequestDTO request);

    void audit(McpAuditCommandDTO command);

    List<McpAuditVO> listAudit(Long tenantId, Long principalId, String toolId, String status,
                               String riskLevel, int limit);

}

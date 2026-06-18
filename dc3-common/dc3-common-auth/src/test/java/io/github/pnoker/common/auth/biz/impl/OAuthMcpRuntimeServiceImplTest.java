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

package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.dal.PrincipalManager;
import io.github.pnoker.common.auth.dal.ServiceAccountManager;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.common.auth.entity.oauth.McpToolConfirmationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.auth.mapper.OAuthMcpMapper;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.constant.service.McpConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeRequestDTO;
import io.github.pnoker.common.entity.dto.McpToolAuthorizeResponseDTO;
import io.github.pnoker.common.entity.dto.McpToolDefinitionDTO;
import io.github.pnoker.common.entity.dto.OAuthClientRegistrationRequestDTO;
import io.github.pnoker.common.entity.dto.OAuthClientRegistrationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthMcpRuntimeServiceImplTest {

    @Mock
    private OAuthMcpMapper oauthMcpMapper;

    @Mock
    private TenantMembershipService tenantMembershipService;

    @Mock
    private PrincipalManager principalManager;

    @Mock
    private ServiceAccountManager serviceAccountManager;

    @InjectMocks
    private OAuthMcpRuntimeServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "issuer", "https://gateway.example/auth");
        ReflectionTestUtils.setField(service, "audience", "dc3-mcp");
        ReflectionTestUtils.setField(service, "authorizationCodeTtl", Duration.ofMinutes(5));
        ReflectionTestUtils.setField(service, "accessTokenTtl", Duration.ofMinutes(15));
        ReflectionTestUtils.setField(service, "refreshTokenTtl", Duration.ofDays(30));
        ReflectionTestUtils.setField(service, "confirmTtl", Duration.ofMinutes(5));
    }

    private McpToolRecord highRiskTool() {
        McpToolRecord tool = new McpToolRecord();
        tool.setToolId("manager:POST:/device/delete");
        tool.setToolName("manager_device_delete");
        tool.setPermissionCode("manager:device:delete");
        tool.setRiskLevel(McpConstant.RiskLevel.HIGH);
        tool.setServiceName("dc3-center-manager");
        tool.setApiPath("/device/delete");
        tool.setHttpMethod("POST");
        return tool;
    }

    private McpToolAuthorizeRequestDTO authorizeRequest(String confirmId, String idempotencyKey, String digest) {
        return McpToolAuthorizeRequestDTO.builder()
                .tenantId(1L)
                .principalId(100L)
                .mcpConnectionId(300L)
                .scope("mcp:tools:call mcp:tools:call:high")
                .toolName("manager_device_delete")
                .argumentDigest(digest)
                .confirmId(confirmId)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    @Test
    void authorizeLowRiskToolPassesWithoutConfirmation() {
        McpToolRecord lowRisk = new McpToolRecord();
        lowRisk.setToolId("manager:GET:/device/get");
        lowRisk.setToolName("manager_device_get");
        lowRisk.setRiskLevel(McpConstant.RiskLevel.LOW);
        when(oauthMcpMapper.selectVisibleToolByName(1L, 100L, 300L, "manager_device_get", true))
                .thenReturn(lowRisk);

        McpToolAuthorizeResponseDTO decision = service.authorizeToolCall(McpToolAuthorizeRequestDTO.builder()
                .tenantId(1L).principalId(100L).mcpConnectionId(300L)
                .scope("mcp:tools:call mcp:tools:call:high").toolName("manager_device_get").build());

        assertThat(decision.getDecision()).isEqualTo("AUTHORIZED");
        verify(oauthMcpMapper, never()).insertConfirmation(any());
    }

    @Test
    void authorizeHighRiskWithoutConfirmIdIssuesPendingTicket() {
        when(oauthMcpMapper.selectVisibleToolByName(1L, 100L, 300L, "manager_device_delete", true))
                .thenReturn(highRiskTool());

        McpToolAuthorizeResponseDTO decision = service.authorizeToolCall(authorizeRequest("", "idem-1", "digest-1"));

        assertThat(decision.getDecision()).isEqualTo("CONFIRM_REQUIRED");
        assertThat(decision.getConfirmId()).isNotBlank();
        ArgumentCaptor<McpToolConfirmationRecord> captor = ArgumentCaptor.forClass(McpToolConfirmationRecord.class);
        verify(oauthMcpMapper).insertConfirmation(captor.capture());
        McpToolConfirmationRecord ticket = captor.getValue();
        assertThat(ticket.getStatus()).isEqualTo("PENDING");
        assertThat(ticket.getToolId()).isEqualTo("manager:POST:/device/delete");
        assertThat(ticket.getArgumentDigest()).isEqualTo("digest-1");
        assertThat(ticket.getExpireTime()).isAfter(LocalDateTime.now());
    }

    @Test
    void authorizeHighRiskWithUsedIdempotencyKeyIsRejected() {
        when(oauthMcpMapper.selectVisibleToolByName(1L, 100L, 300L, "manager_device_delete", true))
                .thenReturn(highRiskTool());
        when(oauthMcpMapper.selectConsumedByIdempotencyKey(300L, "idem-used"))
                .thenReturn(new McpToolConfirmationRecord());

        McpToolAuthorizeResponseDTO decision =
                service.authorizeToolCall(authorizeRequest("", "idem-used", "digest-1"));

        assertThat(decision.getDecision()).isEqualTo("REJECTED");
        assertThat(decision.getMessage()).contains("idempotency key");
        verify(oauthMcpMapper, never()).insertConfirmation(any());
    }

    @Test
    void authorizeHighRiskWithValidConfirmIdConsumesTicketAndAuthorizes() {
        when(oauthMcpMapper.selectVisibleToolByName(1L, 100L, 300L, "manager_device_delete", true))
                .thenReturn(highRiskTool());
        McpToolConfirmationRecord ticket = new McpToolConfirmationRecord();
        ticket.setId(900L);
        ticket.setConfirmId("confirm-ok");
        ticket.setPrincipalId(100L);
        ticket.setConnectionId(300L);
        ticket.setToolId("manager:POST:/device/delete");
        ticket.setArgumentDigest("digest-1");
        ticket.setStatus("PENDING");
        ticket.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(oauthMcpMapper.selectConfirmationByConfirmId("confirm-ok")).thenReturn(ticket);
        when(oauthMcpMapper.consumeConfirmation(eq(900L), any())).thenReturn(1);

        McpToolAuthorizeResponseDTO decision =
                service.authorizeToolCall(authorizeRequest("confirm-ok", "idem-1", "digest-1"));

        assertThat(decision.getDecision()).isEqualTo("AUTHORIZED");
        verify(oauthMcpMapper).consumeConfirmation(eq(900L), any());
    }

    @Test
    void authorizeHighRiskWithMismatchedDigestIsRejected() {
        when(oauthMcpMapper.selectVisibleToolByName(1L, 100L, 300L, "manager_device_delete", true))
                .thenReturn(highRiskTool());
        McpToolConfirmationRecord ticket = new McpToolConfirmationRecord();
        ticket.setId(901L);
        ticket.setConfirmId("confirm-mismatch");
        ticket.setPrincipalId(100L);
        ticket.setConnectionId(300L);
        ticket.setToolId("manager:POST:/device/delete");
        ticket.setArgumentDigest("original-digest");
        ticket.setStatus("PENDING");
        ticket.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(oauthMcpMapper.selectConfirmationByConfirmId("confirm-mismatch")).thenReturn(ticket);

        McpToolAuthorizeResponseDTO decision =
                service.authorizeToolCall(authorizeRequest("confirm-mismatch", "idem-1", "tampered-digest"));

        assertThat(decision.getDecision()).isEqualTo("REJECTED");
        assertThat(decision.getMessage()).contains("arguments do not match");
        verify(oauthMcpMapper, never()).consumeConfirmation(any(), any());
    }

    @Test
    void authorizeHighRiskWithExpiredConfirmIdIsRejected() {
        when(oauthMcpMapper.selectVisibleToolByName(1L, 100L, 300L, "manager_device_delete", true))
                .thenReturn(highRiskTool());
        McpToolConfirmationRecord ticket = new McpToolConfirmationRecord();
        ticket.setId(902L);
        ticket.setConfirmId("confirm-expired");
        ticket.setPrincipalId(100L);
        ticket.setConnectionId(300L);
        ticket.setToolId("manager:POST:/device/delete");
        ticket.setArgumentDigest("digest-1");
        ticket.setStatus("PENDING");
        ticket.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(oauthMcpMapper.selectConfirmationByConfirmId("confirm-expired")).thenReturn(ticket);

        McpToolAuthorizeResponseDTO decision =
                service.authorizeToolCall(authorizeRequest("confirm-expired", "idem-1", "digest-1"));

        assertThat(decision.getDecision()).isEqualTo("REJECTED");
        assertThat(decision.getMessage()).contains("expired");
        verify(oauthMcpMapper, never()).consumeConfirmation(any(), any());
    }

    @Test
    void authorizeHighRiskReplayOfConsumedConfirmIdIsRejected() {
        when(oauthMcpMapper.selectVisibleToolByName(1L, 100L, 300L, "manager_device_delete", true))
                .thenReturn(highRiskTool());
        McpToolConfirmationRecord ticket = new McpToolConfirmationRecord();
        ticket.setId(903L);
        ticket.setConfirmId("confirm-consumed");
        ticket.setPrincipalId(100L);
        ticket.setConnectionId(300L);
        ticket.setToolId("manager:POST:/device/delete");
        ticket.setArgumentDigest("digest-1");
        ticket.setStatus("CONSUMED");
        ticket.setExpireTime(LocalDateTime.now().plusMinutes(5));
        when(oauthMcpMapper.selectConfirmationByConfirmId("confirm-consumed")).thenReturn(ticket);

        McpToolAuthorizeResponseDTO decision =
                service.authorizeToolCall(authorizeRequest("confirm-consumed", "idem-1", "digest-1"));

        assertThat(decision.getDecision()).isEqualTo("REJECTED");
        assertThat(decision.getMessage()).contains("already been used");
        verify(oauthMcpMapper, never()).consumeConfirmation(any(), any());
    }

    @Test
    void metadataOnlyAdvertisesSupportedOAuthGrants() {
        Map<String, Object> metadata = service.authorizationServerMetadata();

        assertThat(metadata.get("issuer")).isEqualTo("https://gateway.example/auth");
        assertThat(castList(metadata.get("grant_types_supported")))
                .contains("authorization_code", "client_credentials", "refresh_token")
                .doesNotContain("password", "implicit");
        assertThat(castList(metadata.get("code_challenge_methods_supported"))).containsExactly("S256");
    }

    @Test
    void publicClientRegistrationPersistsPkceClientWithoutSecret() {
        OAuthClientRegistrationRequestDTO request = OAuthClientRegistrationRequestDTO.builder()
                .clientName("Claude Desktop")
                .redirectUris(List.of("http://127.0.0.1/callback"))
                .scope("mcp:tools:list mcp:tools:call")
                .build();

        RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader();
        principal.setPrincipalId(100L);
        principal.setTenantId(1L);

        OAuthClientRegistrationResponseDTO response = service.registerClient(request, principal);

        ArgumentCaptor<OAuthRegisteredClientRecord> captor =
                ArgumentCaptor.forClass(OAuthRegisteredClientRecord.class);
        verify(oauthMcpMapper).insertClient(captor.capture());
        OAuthRegisteredClientRecord stored = captor.getValue();

        assertThat(response.getClientId()).startsWith("dc3_");
        assertThat(response.getClientSecret()).isNull();
        assertThat(stored.getClientType()).isEqualTo("PUBLIC");
        assertThat(stored.getOwnerPrincipalId()).isEqualTo(100L);
        assertThat(stored.getClientAuthMethods()).isEqualTo("none");
        assertThat(stored.getRequirePkce()).isEqualTo((byte) 1);
        assertThat(stored.getClientSecretHash()).isEmpty();
    }

    @Test
    void confidentialClientCredentialsRegistrationRequiresActiveServiceAccount() {
        OAuthClientRegistrationRequestDTO request = OAuthClientRegistrationRequestDTO.builder()
                .clientName("Robot")
                .clientType("CONFIDENTIAL")
                .grantTypes(List.of("client_credentials"))
                .serviceAccountPrincipalId(200L)
                .build();

        RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader();
        principal.setPrincipalId(100L);
        principal.setTenantId(1L);

        when(serviceAccountManager.getOne(any())).thenReturn(new ServiceAccountDO());

        OAuthClientRegistrationResponseDTO response = service.registerClient(request, principal);

        ArgumentCaptor<OAuthRegisteredClientRecord> captor =
                ArgumentCaptor.forClass(OAuthRegisteredClientRecord.class);
        verify(oauthMcpMapper).insertClient(captor.capture());
        OAuthRegisteredClientRecord stored = captor.getValue();

        assertThat(response.getClientSecret()).isNotBlank();
        assertThat(response.getTokenEndpointAuthMethod()).isEqualTo("client_secret_basic");
        assertThat(stored.getClientType()).isEqualTo("CONFIDENTIAL");
        assertThat(stored.getServiceAccountPrincipalId()).isEqualTo(200L);
        assertThat(stored.getTenantId()).isEqualTo(1L);
        assertThat(stored.getClientSecretHash()).isNotBlank();
    }

    @Test
    void confidentialRegistrationRejectsNullPrincipal() {
        OAuthClientRegistrationRequestDTO request = OAuthClientRegistrationRequestDTO.builder()
                .clientName("Robot")
                .clientType("CONFIDENTIAL")
                .grantTypes(List.of("client_credentials"))
                .serviceAccountPrincipalId(200L)
                .build();

        assertThatThrownBy(() -> service.registerClient(request, null))
                .isInstanceOf(OAuthMcpRuntimeServiceImpl.OAuthProtocolException.class)
                .hasMessageContaining("authenticated principal is required");
    }

    @Test
    void confidentialRegistrationIgnoresBodyTenantIdAndScopesToCallerTenant() {
        // Body claims tenantId 99 plus a foreign service account, but the caller is in tenant 1.
        // The service-account lookup must run against the caller's tenant (1), where none exists,
        // so the body-supplied tenantId is effectively ignored.
        OAuthClientRegistrationRequestDTO request = OAuthClientRegistrationRequestDTO.builder()
                .clientName("Robot")
                .clientType("CONFIDENTIAL")
                .grantTypes(List.of("client_credentials"))
                .tenantId(99L)
                .serviceAccountPrincipalId(200L)
                .build();

        RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader();
        principal.setPrincipalId(100L);
        principal.setTenantId(1L);

        when(serviceAccountManager.getOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.registerClient(request, principal))
                .isInstanceOf(OAuthMcpRuntimeServiceImpl.OAuthProtocolException.class)
                .hasMessageContaining("service account is not active");
    }

    @Test
    void authorizationCodeFlowRejectsMissingPkceChallenge() {
        OAuthRegisteredClientRecord client = new OAuthRegisteredClientRecord();
        client.setClientId("client-1");
        client.setAuthorizationGrantTypes("authorization_code");
        client.setRedirectUris("http://127.0.0.1/callback");
        client.setRequirePkce((byte) 1);
        client.setEnableFlag((byte) 0);
        when(oauthMcpMapper.selectClientByClientId("client-1")).thenReturn(client);

        RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader();
        principal.setPrincipalId(100L);
        principal.setTenantId(1L);

        Map<String, String> params = Map.of(
                "response_type", "code",
                "client_id", "client-1",
                "redirect_uri", "http://127.0.0.1/callback"
        );

        assertThatThrownBy(() -> service.authorize(params, principal))
                .isInstanceOf(OAuthMcpRuntimeServiceImpl.OAuthProtocolException.class)
                .hasMessageContaining("PKCE S256 is required");
    }

    @Test
    void visibleToolsRequireMcpListOrCallScope() {
        assertThatThrownBy(() -> service.listVisibleTools(1L, 100L, 300L, Set.of("mcp:resources:read")))
                .isInstanceOf(OAuthMcpRuntimeServiceImpl.OAuthProtocolException.class)
                .hasMessageContaining("mcp:tools:list scope is required");
    }

    @Test
    void visibleToolsMapCatalogRowsToMcpToolShape() {
        McpToolRecord tool = new McpToolRecord();
        tool.setToolId("auth:GET:/api/v3/auth/user");
        tool.setToolName("auth_user_get");
        tool.setToolTitle("List users");
        tool.setPermissionCode("auth:user:select");
        tool.setRiskLevel("LOW");
        tool.setReadOnlyHint((byte) 1);
        tool.setDestructiveHint((byte) 0);
        tool.setIdempotentHint((byte) 1);
        tool.setOpenWorldHint((byte) 0);
        when(oauthMcpMapper.listVisibleTools(1L, 100L, 300L, false)).thenReturn(List.of(tool));

        List<McpToolDefinitionDTO> visible = service.listVisibleTools(1L, 100L, 300L,
                Set.of("mcp:tools:list"));

        assertThat(visible).hasSize(1);
        assertThat(visible.get(0).getName()).isEqualTo("auth_user_get");
        assertThat(visible.get(0).getTitle()).isEqualTo("List users");
        assertThat(visible.get(0).getMeta().getPermissionCode()).isEqualTo("auth:user:select");
        assertThat(visible.get(0).getMeta().getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    void refreshTokenReplayOfRotatedTokenRevokesAuthorization() {
        OAuthAuthorizationRecord replayed = new OAuthAuthorizationRecord();
        replayed.setAccessTokenJti("jti-leaked");
        when(oauthMcpMapper.selectAuthorizationByRefreshTokenHash(any())).thenReturn(null);
        when(oauthMcpMapper.selectAuthorizationByPreviousRefreshTokenHash(any())).thenReturn(replayed);

        Map<String, String> form = Map.of(
                "grant_type", "refresh_token",
                "refresh_token", "leaked-old-token"
        );

        assertThatThrownBy(() -> service.token(form, null))
                .isInstanceOf(OAuthMcpRuntimeServiceImpl.OAuthProtocolException.class)
                .hasMessageContaining("refresh token has been revoked");
        verify(oauthMcpMapper).revokeAuthorizationByAccessTokenJti(eq("jti-leaked"),
                eq("refresh_token_replayed"), any());
    }

    @Test
    void refreshTokenRejectsUnknownTokenWithoutRevocation() {
        when(oauthMcpMapper.selectAuthorizationByRefreshTokenHash(any())).thenReturn(null);
        when(oauthMcpMapper.selectAuthorizationByPreviousRefreshTokenHash(any())).thenReturn(null);

        Map<String, String> form = Map.of(
                "grant_type", "refresh_token",
                "refresh_token", "never-issued"
        );

        assertThatThrownBy(() -> service.token(form, null))
                .isInstanceOf(OAuthMcpRuntimeServiceImpl.OAuthProtocolException.class)
                .hasMessageContaining("refresh token is invalid or expired");
        verify(oauthMcpMapper, never()).revokeAuthorizationByAccessTokenJti(any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object value) {
        return (List<String>) value;
    }

}

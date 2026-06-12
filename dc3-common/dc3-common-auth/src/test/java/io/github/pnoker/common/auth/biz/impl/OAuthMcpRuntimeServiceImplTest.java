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
import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.auth.mapper.OAuthMcpMapper;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("client_name", "Claude Desktop");
        request.put("redirect_uris", List.of("http://127.0.0.1/callback"));
        request.put("scope", "mcp:tools:list mcp:tools:call");

        RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader();
        principal.setPrincipalId(100L);

        Map<String, Object> response = service.registerClient(request, principal);

        ArgumentCaptor<OAuthRegisteredClientRecord> captor =
                ArgumentCaptor.forClass(OAuthRegisteredClientRecord.class);
        verify(oauthMcpMapper).insertClient(captor.capture());
        OAuthRegisteredClientRecord stored = captor.getValue();

        assertThat(response.get("client_id")).asString().startsWith("dc3_");
        assertThat(response).doesNotContainKey("client_secret");
        assertThat(stored.getClientType()).isEqualTo("PUBLIC");
        assertThat(stored.getOwnerPrincipalId()).isEqualTo(100L);
        assertThat(stored.getClientAuthMethods()).isEqualTo("none");
        assertThat(stored.getRequirePkce()).isEqualTo((byte) 1);
        assertThat(stored.getClientSecretHash()).isEmpty();
    }

    @Test
    void confidentialClientCredentialsRegistrationRequiresActiveServiceAccount() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("client_name", "Robot");
        request.put("client_type", "CONFIDENTIAL");
        request.put("grant_types", List.of("client_credentials"));
        request.put("tenant_id", 1L);
        request.put("service_account_principal_id", 200L);

        when(serviceAccountManager.getOne(any())).thenReturn(new ServiceAccountDO());

        Map<String, Object> response = service.registerClient(request, null);

        ArgumentCaptor<OAuthRegisteredClientRecord> captor =
                ArgumentCaptor.forClass(OAuthRegisteredClientRecord.class);
        verify(oauthMcpMapper).insertClient(captor.capture());
        OAuthRegisteredClientRecord stored = captor.getValue();

        assertThat(response.get("client_secret")).asString().isNotBlank();
        assertThat(response.get("token_endpoint_auth_method")).isEqualTo("client_secret_basic");
        assertThat(stored.getClientType()).isEqualTo("CONFIDENTIAL");
        assertThat(stored.getServiceAccountPrincipalId()).isEqualTo(200L);
        assertThat(stored.getTenantId()).isEqualTo(1L);
        assertThat(stored.getClientSecretHash()).isNotBlank();
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

        List<Map<String, Object>> visible = service.listVisibleTools(1L, 100L, 300L,
                Set.of("mcp:tools:list"));

        assertThat(visible).hasSize(1);
        assertThat(visible.get(0))
                .containsEntry("name", "auth_user_get")
                .containsEntry("title", "List users");
        Map<String, Object> meta = castMap(visible.get(0).get("_meta"));
        assertThat(meta).containsEntry("permission_code", "auth:user:select")
                .containsEntry("risk_level", "LOW");
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object value) {
        return (List<String>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

}

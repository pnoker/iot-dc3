package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.config.OAuthJwtKeyProvider;
import io.github.pnoker.common.auth.config.OAuthProperties;
import io.github.pnoker.common.auth.entity.builder.McpConnectionBuilder;
import io.github.pnoker.common.auth.entity.builder.OAuthClientBuilder;
import io.github.pnoker.common.auth.entity.oauth.OAuthAuthorizationRecord;
import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.oauth.OAuthRegisteredClientRecord;
import io.github.pnoker.common.auth.exception.OAuthProtocolException;
import io.github.pnoker.common.auth.repository.ReactiveOAuthMcpStore;
import io.github.pnoker.common.auth.service.ReactivePrincipalService;
import io.github.pnoker.common.auth.service.ReactiveTenantMembershipService;
import io.github.pnoker.common.auth.tool.McpOpenApiAggregator;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveOAuthMcpRuntimeServiceImplTest {

    @Mock
    private ReactiveOAuthMcpStore store;
    @Mock
    private ReactivePrincipalService principalService;
    @Mock
    private ReactiveTenantMembershipService membershipService;
    @Mock
    private OAuthJwtKeyProvider keyProvider;
    @Mock
    private TransactionalOperator transactionalOperator;
    @Mock
    private OAuthClientBuilder clientBuilder;
    @Mock
    private McpConnectionBuilder connectionBuilder;
    @Mock
    private McpOpenApiAggregator openApiAggregator;

    @InjectMocks
    private ReactiveOAuthMcpRuntimeServiceImpl service;

    @BeforeEach
    void setUp() {
        OAuthProperties properties = new OAuthProperties();
        properties.setIssuer("https://issuer.example");
        properties.setAudience("dc3-mcp");
        properties.setAuthorizationCodeTtl(Duration.ofMinutes(5));
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenTtl(Duration.ofDays(30));
        ReflectionTestUtils.setField(service, "properties", properties);
    }

    @Test
    void authorizationCodeTokenRejectsRedirectUriMismatch() {
        OAuthAuthorizationRecord authorization = new OAuthAuthorizationRecord();
        authorization.setClientId("client-1");
        authorization.setAuthorizationCodeHash("stored-code-hash");
        authorization.setAuthorizationCodeExpires(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));
        authorization.setTokenMetadata("{\"redirect_uri\":\"https://client.example/callback\"}");

        OAuthRegisteredClientRecord client = new OAuthRegisteredClientRecord();
        client.setClientId("client-1");
        client.setClientType("PUBLIC");
        client.setEnableFlag((byte) 0);

        when(store.getAuthorizationByCodeHash(any())).thenReturn(Mono.just(authorization));
        when(store.getClient("client-1")).thenReturn(Mono.just(client));

        StepVerifier.create(service.token(Map.of(
                        "grant_type", "authorization_code",
                        "code", "one-time-code",
                        "redirect_uri", "https://attacker.example/callback"), null))
                .expectErrorSatisfies(error -> {
                    org.assertj.core.api.Assertions.assertThat(error).isInstanceOf(OAuthProtocolException.class);
                    OAuthProtocolException protocol = (OAuthProtocolException) error;
                    org.assertj.core.api.Assertions.assertThat(protocol.getError()).isEqualTo("invalid_grant");
                    org.assertj.core.api.Assertions.assertThat(protocol.getDescription()).contains("redirect_uri");
                })
                .verify();
    }

    @Test
    void revokeRequiresClientAuthentication() {
        StepVerifier.create(service.revoke(Map.of("token", "opaque-refresh-token"), null))
                .expectErrorSatisfies(error -> {
                    org.assertj.core.api.Assertions.assertThat(error).isInstanceOf(OAuthProtocolException.class);
                    OAuthProtocolException protocol = (OAuthProtocolException) error;
                    org.assertj.core.api.Assertions.assertThat(protocol.getError()).isEqualTo("invalid_client");
                })
                .verify();
    }

    @Test
    void authorizationRequiresAndPersistsExplicitConsent() {
        OAuthRegisteredClientRecord client = new OAuthRegisteredClientRecord();
        client.setId(11L);
        client.setClientId("client-1");
        client.setTenantId(7L);
        client.setAuthorizationGrantTypes("authorization_code");
        client.setRedirectUris("https://client.example/callback");
        client.setScopes("mcp:tools:list");
        client.setRequireConsent((byte) 1);
        client.setEnableFlag((byte) 0);
        McpConnectionRecord connection = new McpConnectionRecord();
        connection.setId(22L);
        connection.setClientId("client-1");
        connection.setPrincipalId(101L);
        connection.setTenantId(7L);
        connection.setGrantType("authorization_code");
        connection.setEnableFlag((byte) 0);
        RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader();
        principal.setPrincipalId(101L);
        principal.setTenantId(7L);

        when(store.getClient("client-1")).thenReturn(Mono.just(client));
        when(membershipService.isTenantMember(7L, 101L)).thenReturn(Mono.just(true));
        when(store.getActiveConnection("client-1", 101L, 7L, "authorization_code")).thenReturn(Mono.just(connection));
        when(store.getConsent(11L, 101L, 7L)).thenReturn(Mono.empty());
        when(store.upsertConsent(any())).thenReturn(Mono.just(1));
        when(store.insertAuthorization(any())).thenReturn(Mono.just(1));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> request = Map.of(
                "response_type", "code",
                "client_id", "client-1",
                "redirect_uri", "https://client.example/callback",
                "scope", "mcp:tools:list");
        StepVerifier.create(service.authorize(request, principal))
                .expectErrorSatisfies(error -> {
                    org.assertj.core.api.Assertions.assertThat(error).isInstanceOf(OAuthProtocolException.class);
                    org.assertj.core.api.Assertions.assertThat(((OAuthProtocolException) error).getError()).isEqualTo("consent_required");
                }).verify();

        StepVerifier.create(service.authorize(with(request, "consent", "approve"), principal))
                .expectNextCount(1).verifyComplete();
        org.mockito.Mockito.verify(store).upsertConsent(any());
    }

    private Map<String, String> with(Map<String, String> source, String key, String value) {
        Map<String, String> copy = new java.util.LinkedHashMap<>(source);
        copy.put(key, value);
        return copy;
    }
}

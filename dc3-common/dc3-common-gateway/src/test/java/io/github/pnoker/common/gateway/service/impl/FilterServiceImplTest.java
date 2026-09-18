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
package io.github.pnoker.common.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.LocalCredentialFacade;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.api.TokenFacade;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.utils.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FilterServiceImplTest {

    @Mock
    private TenantFacade tenantFacade;

    @Mock
    private LocalCredentialFacade localCredentialFacade;

    @Mock
    private UserFacade userFacade;

    @Mock
    private TokenFacade tokenFacade;

    @InjectMocks
    private FilterServiceImpl filterService;

    @Test
    void tenantLookupRequiresEnabledTenantAndCachesPublisher() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        when(tenantFacade.getByCode("acme")).thenReturn(Mono.just(tenant));
        ServerHttpRequest request = request("acme", "alice", null);

        StepVerifier.create(filterService.getTenantReactive(request))
                .expectNext(tenant)
                .verifyComplete();
        StepVerifier.create(filterService.getTenantReactive(request))
                .expectNext(tenant)
                .verifyComplete();

        verify(tenantFacade, times(1)).getByCode("acme");
    }

    @Test
    void tenantLookupRejectsMissingAndDisabledTenant() {
        StepVerifier.create(filterService.getTenantReactive(request(null, "alice", null)))
                .expectError(UnAuthorizedException.class)
                .verify();
        verifyNoInteractions(tenantFacade);

        when(tenantFacade.getByCode("disabled")).thenReturn(Mono.just(tenant(12L, "disabled", EnableFlagEnum.DISABLE)));

        StepVerifier.create(filterService.getTenantReactive(request("disabled", "alice", null)))
                .expectError(UnAuthorizedException.class)
                .verify();
    }

    @Test
    void credentialLookupNormalizesLoginAndKeepsTenantInCacheKey() {
        FacadeLocalCredentialBO first = credential("alice", 100L, EnableFlagEnum.ENABLE);
        FacadeLocalCredentialBO second = credential("alice", 200L, EnableFlagEnum.ENABLE);
        when(localCredentialFacade.getByLoginName(1L, "alice")).thenReturn(Mono.just(first));
        when(localCredentialFacade.getByLoginName(2L, "alice")).thenReturn(Mono.just(second));
        ServerHttpRequest request = request("acme", " Alice ", null);

        StepVerifier.create(filterService.getLocalCredentialReactive(request, 1L))
                .expectNext(first)
                .verifyComplete();
        StepVerifier.create(filterService.getLocalCredentialReactive(request, 2L))
                .expectNext(second)
                .verifyComplete();

        verify(localCredentialFacade).getByLoginName(1L, "alice");
        verify(localCredentialFacade).getByLoginName(2L, "alice");
    }

    @Test
    void credentialLookupRejectsInvalidTenantAndDisabledCredential() {
        StepVerifier.create(filterService.getLocalCredentialReactive(request("acme", "alice", null), 0L))
                .expectError(UnAuthorizedException.class)
                .verify();
        verifyNoInteractions(localCredentialFacade);

        when(localCredentialFacade.getByLoginName(11L, "alice"))
                .thenReturn(Mono.just(credential("alice", 100L, EnableFlagEnum.DISABLE)));

        StepVerifier.create(filterService.getLocalCredentialReactive(request("acme", "alice", null), 11L))
                .expectError(UnAuthorizedException.class)
                .verify();
    }

    @Test
    void userLookupPreservesTenantAndBuildsPrincipalHeader() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeLocalCredentialBO credential = credential("alice", 100L, EnableFlagEnum.ENABLE);
        FacadeUserBO user = user(7L, 100L, "Alice", "alice");
        when(userFacade.getByPrincipalId(11L, 100L)).thenReturn(Mono.just(user));

        StepVerifier.create(filterService.getUserReactive(credential, tenant))
                .assertNext(header -> {
                    assertThat(header.getTenantId()).isEqualTo(11L);
                    assertThat(header.getPrincipalId()).isEqualTo(100L);
                    assertThat(header.getPrincipalName()).isEqualTo("alice");
                    assertThat(header.getDisplayName()).isEqualTo("Alice");
                })
                .verifyComplete();
    }

    @Test
    void userLookupRejectsMismatchedPrincipal() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeLocalCredentialBO credential = credential("alice", 100L, EnableFlagEnum.ENABLE);
        when(userFacade.getByPrincipalId(11L, 100L)).thenReturn(Mono.just(user(7L, 200L, "Mallory", "mallory")));

        StepVerifier.create(filterService.getUserReactive(credential, tenant))
                .expectError(UnAuthorizedException.class)
                .verify();
    }

    @Test
    void tokenValidationUsesReactiveFacadeWithoutCachingResult() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeLocalCredentialBO credential = credential("alice", 100L, EnableFlagEnum.ENABLE);
        ServerHttpRequest request =
                request("acme", "alice", JsonUtil.toJsonString(new RequestHeader.TokenHeader("salt", "token")));
        when(tokenFacade.checkValid("acme", "alice", "token")).thenReturn(Mono.just(true));

        StepVerifier.create(filterService.checkValidReactive(request, tenant, credential))
                .verifyComplete();
        StepVerifier.create(filterService.checkValidReactive(request, tenant, credential))
                .verifyComplete();

        verify(tokenFacade, times(2)).checkValid("acme", "alice", "token");
    }

    @Test
    void tokenValidationRejectsMalformedAndInvalidTokens() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeLocalCredentialBO credential = credential("alice", 100L, EnableFlagEnum.ENABLE);

        StepVerifier.create(filterService.checkValidReactive(request("acme", "alice", "{"), tenant, credential))
                .expectError(UnAuthorizedException.class)
                .verify();
        verify(tokenFacade, never()).checkValid("acme", "alice", "token");

        ServerHttpRequest invalid =
                request("acme", "alice", JsonUtil.toJsonString(new RequestHeader.TokenHeader("salt", "token")));
        when(tokenFacade.checkValid("acme", "alice", "token")).thenReturn(Mono.just(false));

        StepVerifier.create(filterService.checkValidReactive(invalid, tenant, credential))
                .expectError(UnAuthorizedException.class)
                .verify();
    }

    private static ServerHttpRequest request(String tenant, String login, String token) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("/api/manager/device");
        if (tenant != null) builder.header(RequestConstant.Header.X_AUTH_TENANT, tenant);
        if (login != null) builder.header(RequestConstant.Header.X_AUTH_LOGIN, login);
        if (token != null) builder.header(RequestConstant.Header.X_AUTH_TOKEN, token);
        return builder.build();
    }

    private static FacadeTenantBO tenant(Long id, String code, EnableFlagEnum enableFlag) {
        FacadeTenantBO tenant = new FacadeTenantBO();
        tenant.setId(id);
        tenant.setTenantCode(code);
        tenant.setEnableFlag(enableFlag);
        return tenant;
    }

    private static FacadeLocalCredentialBO credential(String name, Long principalId, EnableFlagEnum enableFlag) {
        FacadeLocalCredentialBO credential = new FacadeLocalCredentialBO();
        credential.setLoginName(name);
        credential.setPrincipalId(principalId);
        credential.setEnableFlag(enableFlag);
        return credential;
    }

    private static FacadeUserBO user(Long id, Long principalId, String nickName, String userName) {
        FacadeUserBO user = new FacadeUserBO();
        user.setId(id);
        user.setPrincipalId(principalId);
        user.setNickName(nickName);
        user.setUserName(userName);
        return user;
    }
}

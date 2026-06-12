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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    private static ServerHttpRequest request(String tenant, String login, String token) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("/api/manager/device");
        if (tenant != null) {
            builder.header(RequestConstant.Header.X_AUTH_TENANT, tenant);
        }
        if (login != null) {
            builder.header(RequestConstant.Header.X_AUTH_LOGIN, login);
        }
        if (token != null) {
            builder.header(RequestConstant.Header.X_AUTH_TOKEN, token);
        }
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

    private static FacadeUserBO user(Long id, String nickName, String userName) {
        FacadeUserBO user = new FacadeUserBO();
        user.setId(id);
        user.setPrincipalId(100L);
        user.setNickName(nickName);
        user.setUserName(userName);
        return user;
    }

    @Test
    void getTenantRequiresEnabledTenantAndCachesLookup() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        when(tenantFacade.getByCode("acme")).thenReturn(tenant);
        ServerHttpRequest request = request("acme", "alice", null);

        assertThat(filterService.getTenant(request)).isSameAs(tenant);
        assertThat(filterService.getTenant(request)).isSameAs(tenant);

        verify(tenantFacade, times(1)).getByCode("acme");
    }

    @Test
    void getTenantRejectsMissingOrDisabledTenant() {
        assertThatThrownBy(() -> filterService.getTenant(request(null, "alice", null)))
                .isInstanceOf(UnAuthorizedException.class);
        verifyNoInteractions(tenantFacade);

        when(tenantFacade.getByCode("disabled")).thenReturn(tenant(11L, "disabled", EnableFlagEnum.DISABLE));

        assertThatThrownBy(() -> filterService.getTenant(request("disabled", "alice", null)))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void getLocalCredentialRequiresEnabledCredentialAndCachesLookup() {
        FacadeLocalCredentialBO credential = credential("alice", 100L, EnableFlagEnum.ENABLE);
        when(localCredentialFacade.getByLoginName("alice")).thenReturn(credential);
        ServerHttpRequest request = request("acme", "alice", null);

        assertThat(filterService.getLocalCredential(request)).isSameAs(credential);
        assertThat(filterService.getLocalCredential(request)).isSameAs(credential);

        verify(localCredentialFacade, times(1)).getByLoginName("alice");
    }

    @Test
    void getUserBuildsForwardedHeaderAndCachesUserLookup() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeLocalCredentialBO credential = credential("alice", 100L, EnableFlagEnum.ENABLE);
        FacadeUserBO user = user(7L, "Alice", "alice");
        when(userFacade.getByPrincipalId(100L)).thenReturn(user);

        RequestHeader.PrincipalHeader header = filterService.getUser(credential, tenant);
        RequestHeader.PrincipalHeader cachedHeader = filterService.getUser(credential, tenant);

        assertThat(header.getPrincipalId()).isEqualTo(100L);
        assertThat(header.getPrincipalType()).isEqualTo("USER");
        assertThat(header.getDisplayName()).isEqualTo("Alice");
        assertThat(header.getPrincipalName()).isEqualTo("alice");
        assertThat(header.getTenantId()).isEqualTo(11L);
        assertThat(cachedHeader.getPrincipalName()).isEqualTo("alice");
        verify(userFacade, times(1)).getByPrincipalId(100L);
    }

    @Test
    void getUserRejectsCredentialWithoutPrincipalIdAndMissingUser() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);

        assertThatThrownBy(() -> filterService.getUser(credential("alice", null, EnableFlagEnum.ENABLE), tenant))
                .isInstanceOf(UnAuthorizedException.class);
        verify(userFacade, never()).getByPrincipalId(null);

        when(userFacade.getByPrincipalId(100L)).thenReturn(null);

        assertThatThrownBy(() -> filterService.getUser(credential("alice", 100L, EnableFlagEnum.ENABLE), tenant))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void checkValidParsesHeaderAndDoesNotCacheTokenValidation() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeLocalCredentialBO credential = credential("alice", 100L, EnableFlagEnum.ENABLE);
        String tokenHeader = JsonUtil.toJsonString(new RequestHeader.TokenHeader("salt", "token"));
        ServerHttpRequest request = request("acme", "alice", tokenHeader);
        when(tokenFacade.checkValid("acme", "alice", "salt", "token")).thenReturn(true);

        filterService.checkValid(request, tenant, credential);
        filterService.checkValid(request, tenant, credential);

        verify(tokenFacade, times(2)).checkValid("acme", "alice", "salt", "token");
    }

    @Test
    void checkValidRejectsMalformedMissingOrInvalidToken() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeLocalCredentialBO credential = credential("alice", 100L, EnableFlagEnum.ENABLE);

        assertThatThrownBy(() -> filterService.checkValid(request("acme", "alice", "{"), tenant, credential))
                .isInstanceOf(UnAuthorizedException.class);
        assertThatThrownBy(() -> filterService.checkValid(request("acme", "alice",
                JsonUtil.toJsonString(new RequestHeader.TokenHeader("salt", ""))), tenant, credential))
                .isInstanceOf(UnAuthorizedException.class);

        when(tokenFacade.checkValid("acme", "alice", "salt", "token")).thenReturn(false);

        assertThatThrownBy(() -> filterService.checkValid(request("acme", "alice",
                JsonUtil.toJsonString(new RequestHeader.TokenHeader("salt", "token"))), tenant, credential))
                .isInstanceOf(UnAuthorizedException.class);
    }

}

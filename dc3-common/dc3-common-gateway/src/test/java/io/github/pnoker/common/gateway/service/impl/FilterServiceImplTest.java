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
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.api.TokenFacade;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.api.UserLoginFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserLoginBO;
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
    private UserLoginFacade userLoginFacade;
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

    private static FacadeUserLoginBO userLogin(String name, Long userId, EnableFlagEnum enableFlag) {
        FacadeUserLoginBO userLogin = new FacadeUserLoginBO();
        userLogin.setLoginName(name);
        userLogin.setUserId(userId);
        userLogin.setEnableFlag(enableFlag);
        return userLogin;
    }

    private static FacadeUserBO user(Long id, String nickName, String userName) {
        FacadeUserBO user = new FacadeUserBO();
        user.setId(id);
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
    void getUserLoginRequiresEnabledLoginAndCachesLookup() {
        FacadeUserLoginBO userLogin = userLogin("alice", 7L, EnableFlagEnum.ENABLE);
        when(userLoginFacade.getByName("alice")).thenReturn(userLogin);
        ServerHttpRequest request = request("acme", "alice", null);

        assertThat(filterService.getUserLogin(request)).isSameAs(userLogin);
        assertThat(filterService.getUserLogin(request)).isSameAs(userLogin);

        verify(userLoginFacade, times(1)).getByName("alice");
    }

    @Test
    void getUserBuildsForwardedHeaderAndCachesUserLookup() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeUserLoginBO userLogin = userLogin("alice", 7L, EnableFlagEnum.ENABLE);
        FacadeUserBO user = user(7L, "Alice", "alice");
        when(userFacade.getById(7L)).thenReturn(user);

        RequestHeader.UserHeader header = filterService.getUser(userLogin, tenant);
        RequestHeader.UserHeader cachedHeader = filterService.getUser(userLogin, tenant);

        assertThat(header.getUserId()).isEqualTo(7L);
        assertThat(header.getNickName()).isEqualTo("Alice");
        assertThat(header.getUserName()).isEqualTo("alice");
        assertThat(header.getTenantId()).isEqualTo(11L);
        assertThat(cachedHeader.getUserName()).isEqualTo("alice");
        verify(userFacade, times(1)).getById(7L);
    }

    @Test
    void getUserRejectsLoginWithoutUserIdAndMissingUser() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);

        assertThatThrownBy(() -> filterService.getUser(userLogin("alice", null, EnableFlagEnum.ENABLE), tenant))
                .isInstanceOf(UnAuthorizedException.class);
        verify(userFacade, never()).getById(null);

        when(userFacade.getById(7L)).thenReturn(null);

        assertThatThrownBy(() -> filterService.getUser(userLogin("alice", 7L, EnableFlagEnum.ENABLE), tenant))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void checkValidParsesHeaderAndDoesNotCacheTokenValidation() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeUserLoginBO userLogin = userLogin("alice", 7L, EnableFlagEnum.ENABLE);
        String tokenHeader = JsonUtil.toJsonString(new RequestHeader.TokenHeader("salt", "token"));
        ServerHttpRequest request = request("acme", "alice", tokenHeader);
        when(tokenFacade.checkValid("acme", "alice", "salt", "token")).thenReturn(true);

        filterService.checkValid(request, tenant, userLogin);
        filterService.checkValid(request, tenant, userLogin);

        verify(tokenFacade, times(2)).checkValid("acme", "alice", "salt", "token");
    }

    @Test
    void checkValidRejectsMalformedMissingOrInvalidToken() {
        FacadeTenantBO tenant = tenant(11L, "acme", EnableFlagEnum.ENABLE);
        FacadeUserLoginBO userLogin = userLogin("alice", 7L, EnableFlagEnum.ENABLE);

        assertThatThrownBy(() -> filterService.checkValid(request("acme", "alice", "{"), tenant, userLogin))
                .isInstanceOf(UnAuthorizedException.class);
        assertThatThrownBy(() -> filterService.checkValid(request("acme", "alice",
                JsonUtil.toJsonString(new RequestHeader.TokenHeader("salt", ""))), tenant, userLogin))
                .isInstanceOf(UnAuthorizedException.class);

        when(tokenFacade.checkValid("acme", "alice", "salt", "token")).thenReturn(false);

        assertThatThrownBy(() -> filterService.checkValid(request("acme", "alice",
                JsonUtil.toJsonString(new RequestHeader.TokenHeader("salt", "token"))), tenant, userLogin))
                .isInstanceOf(UnAuthorizedException.class);
    }

}

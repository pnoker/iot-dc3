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

import io.github.pnoker.common.auth.cache.TokenDenylistCache;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.bo.UserPasswordBO;
import io.github.pnoker.common.auth.service.TenantBindService;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.auth.service.UserLoginService;
import io.github.pnoker.common.auth.service.UserPasswordService;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.DecodeUtil;
import io.github.pnoker.common.utils.KeyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    private static final String LOGIN = "alice";
    private static final String TENANT_CODE = "tenant-A";
    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final Long PASSWORD_ID = 9L;
    private static final String LOGIN_PASSWORD = "stored-password-hash";
    private static final String SALT = "0123456789abcdef0123456789abcdef";

    @Mock
    private TenantService tenantService;

    @Mock
    private UserLoginService userLoginService;

    @Mock
    private UserPasswordService userPasswordService;

    @Mock
    private TenantBindService tenantBindService;

    @Mock
    private TokenDenylistCache tokenDenylistCache;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private TenantBO tenant;
    private UserLoginBO userLogin;
    private UserPasswordBO password;
    private TenantBindBO bind;

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    @BeforeEach
    void setUp() throws Exception {
        tenant = new TenantBO();
        setField(tenant, "id", TENANT_ID);

        userLogin = new UserLoginBO();
        setField(userLogin, "userId", USER_ID);
        setField(userLogin, "userPasswordId", PASSWORD_ID);

        password = new UserPasswordBO();
        setField(password, "loginPassword", LOGIN_PASSWORD);

        bind = new TenantBindBO();
        setField(bind, "tenantId", TENANT_ID);
        setField(bind, "userId", USER_ID);
    }

    @Test
    void generateSaltReturnsUuidWhenTenantIsKnown() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        String salt = tokenService.generateSalt(LOGIN, TENANT_CODE);
        assertThat(salt).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void generateSaltRejectsUnknownTenant() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(null);
        assertThatThrownBy(() -> tokenService.generateSalt(LOGIN, TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenSucceedsForCorrectCredentials() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(bind);
        when(userPasswordService.getById(PASSWORD_ID)).thenReturn(password);

        String md5 = DecodeUtil.md5(LOGIN_PASSWORD, SALT);
        String token = tokenService.generateToken(LOGIN, SALT, md5, TENANT_CODE);

        assertThat(token).isNotBlank();
        // Parsing with the same salt + tenant must succeed; this also locks down the
        // KeyUtil.generateToken contract used by AuthenticGatewayFilter.
        assertThat(KeyUtil.parserToken(LOGIN, SALT, token, TENANT_ID)).isNotNull();
    }

    @Test
    void generateTokenRejectsUnknownTenant() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(null);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, "anything", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsUnknownLogin() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(null);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, "anything", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsUnboundUser() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(null);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, "anything", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsMissingPasswordRecord() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(bind);
        when(userPasswordService.getById(PASSWORD_ID)).thenReturn(null);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, "anything", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsBlankSalt() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(bind);
        when(userPasswordService.getById(PASSWORD_ID)).thenReturn(password);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, "", "anything", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsMismatchedPassword() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(bind);
        when(userPasswordService.getById(PASSWORD_ID)).thenReturn(password);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, "wrong-md5", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void checkValidRejectsUnknownTenant() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(null);
        assertThatThrownBy(() -> tokenService.checkValid(LOGIN, SALT, "token", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void checkValidReturnsInvalidForBlankToken() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        TokenValid result = tokenService.checkValid(LOGIN, SALT, "", TENANT_CODE);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getExpireTime()).isNull();
    }

    @Test
    void checkValidReturnsInvalidWhenLoginUnknown() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(null);
        TokenValid result = tokenService.checkValid(LOGIN, SALT, "any-token", TENANT_CODE);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void checkValidReturnsInvalidWhenUserNotBoundToTenant() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(null);
        TokenValid result = tokenService.checkValid(LOGIN, SALT, "any-token", TENANT_CODE);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void checkValidReturnsValidForCorrectlySignedToken() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(bind);
        when(tokenDenylistCache.isRevoked(eq(LOGIN), eq(TENANT_CODE), anyLong())).thenReturn(false);
        String token = KeyUtil.generateToken(LOGIN, SALT, TENANT_ID);
        TokenValid result = tokenService.checkValid(LOGIN, SALT, token, TENANT_CODE);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getExpireTime()).isNotNull();
    }

    @Test
    void checkValidReturnsInvalidWhenTokenWasRevoked() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(bind);
        when(tokenDenylistCache.isRevoked(eq(LOGIN), eq(TENANT_CODE), anyLong())).thenReturn(true);
        String token = KeyUtil.generateToken(LOGIN, SALT, TENANT_ID);

        TokenValid result = tokenService.checkValid(LOGIN, SALT, token, TENANT_CODE);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getExpireTime()).isNotNull();
    }

    @Test
    void checkValidSwallowsParseFailureAndReturnsInvalid() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(userLoginService.getByLoginName(LOGIN, false)).thenReturn(userLogin);
        when(tenantBindService.getByTenantIdAndUserId(TENANT_ID, USER_ID)).thenReturn(bind);
        TokenValid result = tokenService.checkValid(LOGIN, SALT, "garbage-token", TENANT_CODE);
        assertThat(result.isValid()).isFalse();
    }
}

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
import io.github.pnoker.common.auth.dal.PrincipalManager;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.service.LocalCredentialService;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.auth.service.TenantService;
import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.exception.PasswordChangeRequiredException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.KeyUtil;
import io.github.pnoker.common.utils.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    private static final String LOGIN = "alice";
    private static final String TENANT_CODE = "tenant-A";
    private static final Long TENANT_ID = 1L;
    private static final Long CREDENTIAL_ID = 9L;
    private static final Long PRINCIPAL_ID = 100L;
    private static final String RAW_PASSWORD = "secret";
    private static final String SALT = "0123456789abcdef0123456789abcdef";

    @Mock
    private TenantService tenantService;

    @Mock
    private LocalCredentialService localCredentialService;

    @Mock
    private TenantMembershipService tenantMembershipService;

    @Mock
    private PrincipalManager principalManager;

    @Mock
    private TokenDenylistCache tokenDenylistCache;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private TenantBO tenant;
    private LocalCredentialBO credential;

    @BeforeEach
    void setUp() {
        System.setProperty("dc3.security.key", "0123456789abcdef0123456789abcdef");

        tenant = new TenantBO();
        tenant.setId(TENANT_ID);

        credential = new LocalCredentialBO();
        credential.setId(CREDENTIAL_ID);
        credential.setPrincipalId(PRINCIPAL_ID);
        credential.setLoginName(LOGIN);
        credential.setPasswordHash(PasswordUtil.encode(RAW_PASSWORD));
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("dc3.security.key");
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
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);
        when(localCredentialService.verifyPassword(credential, RAW_PASSWORD)).thenReturn(true);

        PrincipalDO principal = new PrincipalDO();
        principal.setId(PRINCIPAL_ID);
        when(principalManager.getById(PRINCIPAL_ID)).thenReturn(principal);

        String token = tokenService.generateToken(LOGIN, SALT, RAW_PASSWORD, TENANT_CODE);

        assertThat(token).isNotBlank();
        assertThat(KeyUtil.parserToken(String.valueOf(PRINCIPAL_ID), SALT, token, TENANT_ID)).isNotNull();
        verify(localCredentialService).recordSuccessfulLogin(CREDENTIAL_ID);
        verify(localCredentialService, never()).recordFailedLogin(anyLong());
        verify(principalManager).updateById(any(PrincipalDO.class));
    }

    @Test
    void generateTokenRejectsUnknownTenant() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(null);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, RAW_PASSWORD, TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsUnknownLogin() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(null);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, RAW_PASSWORD, TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsUnboundPrincipal() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(false);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, RAW_PASSWORD, TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsBlankSalt() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);
        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, "", RAW_PASSWORD, TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void generateTokenRejectsMismatchedPasswordAndRecordsFailure() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);
        when(localCredentialService.verifyPassword(credential, "wrong")).thenReturn(false);

        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, "wrong", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
        verify(localCredentialService).recordFailedLogin(CREDENTIAL_ID);
    }

    @Test
    void generateTokenRejectsExpiredPasswordWithoutIssuingToken() {
        credential.setPasswordExpireTime(java.time.LocalDateTime.now().minusDays(1));
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);
        when(localCredentialService.verifyPassword(credential, RAW_PASSWORD)).thenReturn(true);

        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, RAW_PASSWORD, TENANT_CODE))
                .isInstanceOf(PasswordChangeRequiredException.class)
                .extracting(e -> ((PasswordChangeRequiredException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_EXPIRED);
        verify(localCredentialService).recordSuccessfulLogin(CREDENTIAL_ID);
        verify(principalManager, never()).updateById(any(PrincipalDO.class));
    }

    @Test
    void generateTokenRejectsRequirePasswordChangeWithoutIssuingToken() {
        credential.setRequirePasswordChange(RequirePasswordChangeFlagEnum.REQUIRED);
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);
        when(localCredentialService.verifyPassword(credential, RAW_PASSWORD)).thenReturn(true);

        assertThatThrownBy(() -> tokenService.generateToken(LOGIN, SALT, RAW_PASSWORD, TENANT_CODE))
                .isInstanceOf(PasswordChangeRequiredException.class)
                .extracting(e -> ((PasswordChangeRequiredException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        verify(principalManager, never()).updateById(any(PrincipalDO.class));
    }

    @Test
    void changePasswordDelegatesAfterTenantMembershipCheck() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);

        tokenService.changePassword(LOGIN, RAW_PASSWORD, "new-secret", TENANT_CODE);

        verify(localCredentialService).changePassword(LOGIN, RAW_PASSWORD, "new-secret");
    }

    @Test
    void changePasswordRejectsUnboundPrincipal() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(false);

        assertThatThrownBy(() -> tokenService.changePassword(LOGIN, RAW_PASSWORD, "new-secret", TENANT_CODE))
                .isInstanceOf(UnAuthorizedException.class);
        verify(localCredentialService, never()).changePassword(any(), any(), any());
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
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(null);
        TokenValid result = tokenService.checkValid(LOGIN, SALT, "any-token", TENANT_CODE);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void checkValidReturnsInvalidWhenPrincipalNotInTenant() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(false);
        TokenValid result = tokenService.checkValid(LOGIN, SALT, "any-token", TENANT_CODE);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void checkValidReturnsValidForCorrectlySignedToken() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);
        when(tokenDenylistCache.isRevoked(eq(String.valueOf(PRINCIPAL_ID)), eq(TENANT_CODE), anyLong()))
                .thenReturn(false);
        String token = KeyUtil.generateToken(String.valueOf(PRINCIPAL_ID), SALT, TENANT_ID);

        TokenValid result = tokenService.checkValid(LOGIN, SALT, token, TENANT_CODE);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getExpireTime()).isNotNull();
    }

    @Test
    void checkValidReturnsInvalidWhenTokenWasRevoked() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);
        when(tokenDenylistCache.isRevoked(eq(String.valueOf(PRINCIPAL_ID)), eq(TENANT_CODE), anyLong()))
                .thenReturn(true);
        String token = KeyUtil.generateToken(String.valueOf(PRINCIPAL_ID), SALT, TENANT_ID);

        TokenValid result = tokenService.checkValid(LOGIN, SALT, token, TENANT_CODE);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getExpireTime()).isNotNull();
    }

    @Test
    void checkValidSwallowsParseFailureAndReturnsInvalid() {
        when(tenantService.getByCode(TENANT_CODE)).thenReturn(tenant);
        when(localCredentialService.getByLoginName(LOGIN, false)).thenReturn(credential);
        when(tenantMembershipService.isTenantMember(TENANT_ID, PRINCIPAL_ID)).thenReturn(true);
        TokenValid result = tokenService.checkValid(LOGIN, SALT, "garbage-token", TENANT_CODE);
        assertThat(result.isValid()).isFalse();
    }

}

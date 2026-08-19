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

package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.dal.LocalCredentialManager;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.builder.LocalCredentialBuilder;
import io.github.pnoker.common.auth.entity.model.LocalCredentialDO;
import io.github.pnoker.common.auth.service.TenantMembershipService;
import io.github.pnoker.common.enums.CredentialTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.utils.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalCredentialServiceImplTest {

    @Mock
    private LocalCredentialBuilder localCredentialBuilder;

    @Mock
    private LocalCredentialManager localCredentialManager;

    @Mock
    private TenantMembershipService tenantMembershipService;

    private LocalCredentialServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LocalCredentialServiceImpl(
                localCredentialBuilder, localCredentialManager, tenantMembershipService);
    }

    @Test
    void addNormalizesLoginAndHashesPasswordBeforePersistence() {
        LocalCredentialBO credential = new LocalCredentialBO();
        credential.setPrincipalId(7L);
        credential.setLoginName("  Alice  ");
        credential.setRawPassword("correct horse battery staple");
        LocalCredentialDO persisted = new LocalCredentialDO();
        when(localCredentialBuilder.buildDOByBO(credential)).thenReturn(persisted);
        when(localCredentialManager.save(persisted)).thenReturn(true);

        service.add(credential);

        assertThat(credential.getLoginNameNormalized()).isEqualTo("alice");
        assertThat(credential.getCredentialType()).isEqualTo(CredentialTypeEnum.PASSWORD);
        assertThat(credential.getEnableFlag()).isEqualTo(EnableFlagEnum.ENABLE);
        assertThat(credential.getRequirePasswordChange()).isEqualTo(RequirePasswordChangeFlagEnum.REQUIRED);
        assertThat(credential.getPasswordHash()).doesNotContain("correct horse battery staple");
        assertThat(PasswordUtil.verify("correct horse battery staple", credential.getPasswordHash())).isTrue();
        verify(localCredentialManager).save(persisted);
    }

    @Test
    void fifthFailedLoginLocksCredentialForFifteenMinutes() {
        LocalCredentialDO credential = credentialDO(4);
        when(localCredentialManager.getById(11L)).thenReturn(credential);
        LocalDateTime before = LocalDateTime.now().plusMinutes(14);

        service.recordFailedLogin(11L);

        assertThat(credential.getFailedAttempts()).isEqualTo(5);
        assertThat(credential.getLockedUntil()).isAfter(before);
        assertThat(credential.getLockedUntil()).isBefore(LocalDateTime.now().plusMinutes(16));
        assertThat(credential.getOperateTime()).isNull();
        verify(localCredentialManager).updateById(credential);
    }

    @Test
    void successfulLoginClearsFailureState() {
        LocalCredentialDO credential = credentialDO(5);
        credential.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(localCredentialManager.getById(12L)).thenReturn(credential);

        service.recordSuccessfulLogin(12L);

        assertThat(credential.getFailedAttempts()).isZero();
        assertThat(credential.getLockedUntil()).isNull();
        verify(localCredentialManager).updateById(credential);
    }

    @Test
    void activeLockRejectsCorrectPassword() {
        LocalCredentialBO credential = new LocalCredentialBO();
        credential.setPasswordHash(PasswordUtil.encode("secret"));
        credential.setLockedUntil(LocalDateTime.now().plusMinutes(1));

        assertThat(service.verifyPassword(credential, "secret")).isFalse();
    }

    @Test
    void changePasswordRejectsWrongCurrentPasswordWithoutWriting() {
        LocalCredentialDO stored = credentialDO(0);
        stored.setPasswordHash(PasswordUtil.encode("old-secret"));
        LocalCredentialBO credential = new LocalCredentialBO();
        credential.setId(21L);
        credential.setPasswordHash(stored.getPasswordHash());
        when(localCredentialManager.getOne(any())).thenReturn(stored);
        when(localCredentialBuilder.buildBOByDO(stored)).thenReturn(credential);

        assertThatThrownBy(() -> service.changePassword("alice", "wrong", "new-secret"))
                .isInstanceOf(UnAuthorizedException.class);

        verify(localCredentialManager, never()).updateById(any());
    }

    @Test
    void changePasswordRehashesAndResetsSecurityState() {
        LocalCredentialDO stored = credentialDO(3);
        stored.setId(21L);
        stored.setPasswordHash(PasswordUtil.encode("old-secret"));
        stored.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        LocalCredentialBO credential = new LocalCredentialBO();
        credential.setId(21L);
        credential.setPasswordHash(stored.getPasswordHash());
        when(localCredentialManager.getOne(any())).thenReturn(stored);
        when(localCredentialBuilder.buildBOByDO(stored)).thenReturn(credential);
        when(localCredentialManager.getById(21L)).thenReturn(stored);
        when(localCredentialManager.updateById(stored)).thenReturn(true);
        ReflectionTestUtils.setField(service, "passwordExpireDays", 30L);

        service.changePassword(" Alice ", "old-secret", "new-secret");

        ArgumentCaptor<LocalCredentialDO> captor = ArgumentCaptor.forClass(LocalCredentialDO.class);
        verify(localCredentialManager).updateById(captor.capture());
        LocalCredentialDO updated = captor.getValue();
        assertThat(PasswordUtil.verify("new-secret", updated.getPasswordHash())).isTrue();
        assertThat(PasswordUtil.verify("old-secret", updated.getPasswordHash())).isFalse();
        assertThat(updated.getPasswordExpireTime()).isEqualTo(updated.getPasswordUpdatedTime().plusDays(30));
        assertThat(updated.getRequirePasswordChange()).isZero();
        assertThat(updated.getFailedAttempts()).isZero();
        assertThat(updated.getLockedUntil()).isNull();
        assertThat(updated.getOperateTime()).isNull();
    }

    private static LocalCredentialDO credentialDO(int failedAttempts) {
        LocalCredentialDO credential = new LocalCredentialDO();
        credential.setFailedAttempts(failedAttempts);
        credential.setOperateTime(LocalDateTime.now());
        return credential;
    }
}

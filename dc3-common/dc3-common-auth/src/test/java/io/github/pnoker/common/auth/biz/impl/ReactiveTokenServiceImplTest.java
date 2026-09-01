package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.cache.TokenDenylistCache;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.bo.TenantBO;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialCommandService;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.common.auth.service.ReactivePrincipalService;
import io.github.pnoker.common.auth.service.ReactiveTenantService;
import io.github.pnoker.common.enums.RequirePasswordChangeFlagEnum;
import io.github.pnoker.common.exception.UnAuthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveTokenServiceImplTest {

    @Mock ReactiveTenantService tenantService;
    @Mock ReactiveLocalCredentialService credentialService;
    @Mock ReactiveLocalCredentialCommandService credentialCommands;
    @Mock ReactivePrincipalService principalService;
    @Mock TokenDenylistCache denylist;

    private ReactiveTokenServiceImpl service;
    private TenantBO tenant;
    private LocalCredentialBO credential;

    @BeforeEach
    void setUp() {
        service = new ReactiveTokenServiceImpl(tenantService, credentialService, credentialCommands, principalService, denylist);
        tenant = new TenantBO();
        tenant.setId(7L);
        tenant.setTenantCode("tenant-a");
        credential = new LocalCredentialBO();
        credential.setId(11L);
        credential.setPrincipalId(19L);
        credential.setRequirePasswordChange(RequirePasswordChangeFlagEnum.NOT_REQUIRED);
    }

    @Test
    void invalidPasswordRecordsFailureAndNeverIssuesToken() {
        when(tenantService.getByCode("tenant-a")).thenReturn(Mono.just(tenant));
        when(credentialService.getByLoginName(7L, "alice")).thenReturn(Mono.just(credential));
        when(credentialService.verifyPassword(credential, "bad")).thenReturn(Mono.just(false));
        when(credentialCommands.recordFailedLogin(7L, 11L)).thenReturn(Mono.empty());

        StepVerifier.create(service.generateToken("alice", "bad", "tenant-a"))
                .expectError(UnAuthorizedException.class).verify();

        verify(credentialCommands).recordFailedLogin(7L, 11L);
        verify(principalService, never()).touchLastLogin(19L);
    }

    @Test
    void blankTokenIsInvalidWithoutCredentialLookup() {
        when(tenantService.getByCode("tenant-a")).thenReturn(Mono.just(tenant));

        StepVerifier.create(service.checkValid("alice", " ", "tenant-a"))
                .assertNext(result -> {
                    org.assertj.core.api.Assertions.assertThat(result.isValid()).isFalse();
                    org.assertj.core.api.Assertions.assertThat(result.getExpireTime()).isNull();
                }).verifyComplete();

        verify(credentialService, never()).getByLoginName(7L, "alice");
    }
}

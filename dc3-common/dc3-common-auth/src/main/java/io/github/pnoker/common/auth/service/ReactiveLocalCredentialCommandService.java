package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import reactor.core.publisher.Mono;

/** Atomic non-blocking local credential lifecycle and login state commands. */
public interface ReactiveLocalCredentialCommandService {
    Mono<LocalCredentialBO> add(Long tenantId, LocalCredentialBO credential, Long operatorId, String operatorName);
    Mono<LocalCredentialBO> update(Long tenantId, LocalCredentialBO credential, Long operatorId, String operatorName);
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
    Mono<LocalCredentialBO> resetPassword(Long tenantId, Long id, String rawPassword, Long operatorId, String operatorName);
    Mono<LocalCredentialBO> changePassword(Long tenantId, String loginName, String currentPassword, String newPassword,
                                           Long operatorId, String operatorName);
    Mono<Void> recordSuccessfulLogin(Long tenantId, Long id);
    Mono<Void> recordFailedLogin(Long tenantId, Long id);
}

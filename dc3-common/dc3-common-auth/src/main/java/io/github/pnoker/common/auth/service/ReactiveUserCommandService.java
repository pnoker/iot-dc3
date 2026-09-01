package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import reactor.core.publisher.Mono;

/** Atomic user lifecycle commands spanning principal, user and membership rows. */
public interface ReactiveUserCommandService {
    Mono<UserBO> add(Long tenantId, UserBO user, Long operatorId, String operatorName);
    Mono<UserBO> update(Long tenantId, UserBO user, Long operatorId, String operatorName);
    Mono<Boolean> delete(Long tenantId, Long userId, Long operatorId, String operatorName);
}

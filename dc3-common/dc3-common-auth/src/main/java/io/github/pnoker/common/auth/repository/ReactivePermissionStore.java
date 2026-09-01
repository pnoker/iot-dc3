package io.github.pnoker.common.auth.repository;

import reactor.core.publisher.Flux;

/** Reactive tenant-scoped permission projection used by authorization checks. */
public interface ReactivePermissionStore {
    Flux<String> listResourceCodes(Long tenantId, Long principalId);
}

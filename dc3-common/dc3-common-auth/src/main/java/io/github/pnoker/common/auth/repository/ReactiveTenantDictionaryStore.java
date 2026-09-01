package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.TenantDO;
import reactor.core.publisher.Flux;

/** Tenant projection used by the auth dictionary endpoint. */
public interface ReactiveTenantDictionaryStore {
    Flux<TenantDO> listEnabled();
}

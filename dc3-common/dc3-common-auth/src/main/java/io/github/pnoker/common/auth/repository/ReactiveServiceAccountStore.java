package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.ServiceAccountBO;
import io.github.pnoker.common.auth.entity.model.ServiceAccountDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Non-blocking persistence port for service-account aggregates. */
public interface ReactiveServiceAccountStore {
    Mono<ServiceAccountDO> getById(Long tenantId, Long id);
    Mono<OffsetPage<ServiceAccountDO>> list(ServiceAccountFilter filter);
    Mono<ServiceAccountDO> insert(ServiceAccountBO account);
    Mono<ServiceAccountDO> update(Long tenantId, ServiceAccountBO account);
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}

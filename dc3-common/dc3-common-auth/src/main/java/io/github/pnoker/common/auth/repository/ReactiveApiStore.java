package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

public interface ReactiveApiStore {
    Mono<ApiDO> getById(Long id);
    Mono<OffsetPage<ApiDO>> list(ApiFilter filter);
    Mono<ApiDO> insert(ApiBO api);
    Mono<ApiDO> update(ApiBO api);
    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);
    Mono<Boolean> existsDuplicate(ApiBO api);
}

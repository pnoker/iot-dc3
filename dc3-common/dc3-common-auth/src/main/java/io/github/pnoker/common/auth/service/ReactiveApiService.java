package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.repository.ApiFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

public interface ReactiveApiService {
    Mono<ApiBO> getById(Long id);
    Mono<OffsetPage<ApiBO>> list(ApiFilter filter);
    Mono<ApiBO> add(ApiBO api);
    Mono<ApiBO> update(ApiBO api);
    Mono<Void> delete(Long id, Long operatorId, String operatorName);
}

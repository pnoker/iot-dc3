package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.LabelBO;
import io.github.pnoker.common.manager.repository.LabelFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive application service for labels. */
public interface ReactiveLabelService {
    Mono<LabelBO> add(LabelBO label);
    Mono<LabelBO> update(LabelBO label);
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
    Mono<LabelBO> getById(Long tenantId, Long id);
    Mono<OffsetPage<LabelBO>> list(LabelFilter filter);
}

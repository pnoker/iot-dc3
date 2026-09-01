package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.LabelBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped labels. */
public interface ReactiveLabelStore {
    Mono<OffsetPage<LabelBO>> list(LabelFilter filter);
    Mono<LabelBO> get(Long tenantId, Long id);
    Mono<LabelBO> getByName(Long tenantId, String name, byte entityType);
    Mono<Boolean> hasActiveBindings(Long tenantId, Long labelId);
    Mono<LabelBO> insert(LabelBO label);
    Mono<LabelBO> update(LabelBO label);
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}

package io.github.pnoker.db.r2dbc.core.service;

import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.tenant.TenantScope;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive business contract for aggregate CRUD. Implementations must keep
 * tenant scope explicit and must never block or expose persistence entities.
 */
public interface ReactiveCrudService<B, Q> {

    Mono<B> add(TenantScope tenant, B businessObject);

    Mono<Void> delete(TenantScope tenant, UUID id);

    Mono<B> update(TenantScope tenant, B businessObject);

    Mono<B> get(TenantScope tenant, UUID id);

    Mono<OffsetPage<B>> list(TenantScope tenant, Q query, PageRequest pageRequest);
}

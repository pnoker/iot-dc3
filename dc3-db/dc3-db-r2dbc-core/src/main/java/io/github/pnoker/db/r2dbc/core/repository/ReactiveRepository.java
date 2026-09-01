package io.github.pnoker.db.r2dbc.core.repository;

import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.tenant.TenantScope;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Minimal repository port shared by domain modules; persistence details stay in adapters. */
public interface ReactiveRepository<DO, Q> {

    Mono<DO> insert(TenantScope tenant, DO entity);

    Mono<DO> update(TenantScope tenant, DO entity);

    Mono<Void> delete(TenantScope tenant, UUID id);

    Mono<DO> findById(TenantScope tenant, UUID id);

    Mono<OffsetPage<DO>> findPage(TenantScope tenant, Q query, PageRequest pageRequest);
}

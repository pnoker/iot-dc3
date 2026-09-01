package io.github.pnoker.common.auth.service;
import io.github.pnoker.common.auth.entity.bo.*; import io.github.pnoker.common.auth.repository.ResourceFilter; import io.github.pnoker.db.r2dbc.core.page.OffsetPage; import reactor.core.publisher.*;
public interface ReactiveResourceService{Mono<ResourceBO> getById(Long id);Mono<OffsetPage<ResourceBO>> list(ResourceFilter filter);Flux<ResourceTreeBO> listTree(ResourceFilter filter);Mono<ResourceBO> add(ResourceBO resource);Mono<ResourceBO> update(ResourceBO resource);Mono<Void> delete(Long id,Long operatorId,String operatorName);}

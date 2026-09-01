package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.bo.MenuTreeBO;
import io.github.pnoker.common.auth.repository.MenuFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveMenuService {
    Mono<MenuBO> getById(Long id);
    Mono<OffsetPage<MenuBO>> list(MenuFilter filter);
    Flux<MenuTreeBO> listTree(MenuFilter filter);
    Mono<MenuBO> add(MenuBO menu);
    Mono<MenuBO> update(MenuBO menu);
    Mono<Void> delete(Long id, Long operatorId, String operatorName);
}

package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.model.MenuDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveMenuStore {
    Mono<MenuDO> getById(Long id);
    Mono<OffsetPage<MenuDO>> list(MenuFilter filter);
    Flux<MenuDO> listTree(MenuFilter filter);
    Mono<MenuDO> insert(MenuBO menu);
    Mono<MenuDO> update(MenuBO menu);
    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);
    Mono<Boolean> existsDuplicate(MenuBO menu);
    Mono<Boolean> hasChildren(Long id);
    Mono<Boolean> isDescendant(Long rootId, Long candidateId);
}

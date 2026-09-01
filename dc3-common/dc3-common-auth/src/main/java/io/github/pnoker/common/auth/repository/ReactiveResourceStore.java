package io.github.pnoker.common.auth.repository;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.enums.ResourceTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
public interface ReactiveResourceStore {

    Mono<ResourceDO> getById(Long id);

    Mono<OffsetPage<ResourceDO>> list(ResourceFilter filter);

    Flux<ResourceDO> listTree(ResourceFilter filter);

    Mono<ResourceDO> insert(ResourceBO resource);

    Mono<ResourceDO> update(ResourceBO resource);

    Mono<Boolean> delete(Long id, Long operatorId, String operatorName);

    Mono<Boolean> existsDuplicate(ResourceBO resource);

    Mono<Boolean> hasChildren(Long id);

    Mono<Boolean> isDescendant(Long rootId, Long candidateId);

    Mono<ResourceDO> getByTypeAndEntity(ResourceTypeEnum resourceType, Long entityId);
}

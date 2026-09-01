package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ReactiveResourceRegistryStore {

    Flux<ApiDO> listApis(String serviceName);

    Mono<ApiDO> insertApi(ApiDO api);

    Mono<ApiDO> updateApi(ApiDO api);

    Mono<Boolean> deleteApi(Long id, Long operatorId, String operatorName);

    Flux<ResourceDO> listApiResources(String serviceName);

    Flux<ResourceDO> listResourcesByEntityIds(List<Long> entityIds);

    Mono<ResourceDO> getResourceByCode(String resourceCode);

    Mono<ResourceDO> insertResource(ResourceDO resource);

    Mono<ResourceDO> updateResource(ResourceDO resource);

    Mono<Boolean> deleteResource(Long id, Long operatorId, String operatorName);

    Mono<Long> countChildren(Long parentId);

    Mono<Long> acquireLock(String lockName);
}

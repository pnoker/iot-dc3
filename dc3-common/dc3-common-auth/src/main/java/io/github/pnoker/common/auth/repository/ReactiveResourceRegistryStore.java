/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Reactive persistence port for resource registry records. */

public interface ReactiveResourceRegistryStore {

    /** Stream apis matching the request. */
    Flux<ApiDO> listApis(String serviceName);

    /** Insert one api and emit the stored row. */
    Mono<ApiDO> insertApi(ApiDO api);

    /** Update one api and emit the updated row. */
    Mono<ApiDO> updateApi(ApiDO api);

    /** Delete the api, reporting whether a row was removed. */
    Mono<Boolean> deleteApi(Long id, Long operatorId, String operatorName);

    /** Stream api resources matching the request. */
    Flux<ResourceDO> listApiResources(String serviceName);

    /** List resource registries matched by entity ids. */
    Flux<ResourceDO> listResourcesByEntityIds(List<Long> entityIds);

    /** Resolve the resource registry by its code. */
    Mono<ResourceDO> getResourceByCode(String resourceCode);

    /** Insert one resource and emit the stored row. */
    Mono<ResourceDO> insertResource(ResourceDO resource);

    /** Update one resource and emit the updated row. */
    Mono<ResourceDO> updateResource(ResourceDO resource);

    /** Delete the resource, reporting whether a row was removed. */
    Mono<Boolean> deleteResource(Long id, Long operatorId, String operatorName);

    /** Count childs matching the request. */
    Mono<Long> countChildren(Long parentId);

    /** Acquire the named advisory lock, emitting the lock token. */
    Mono<Long> acquireLock(String lockName);
}

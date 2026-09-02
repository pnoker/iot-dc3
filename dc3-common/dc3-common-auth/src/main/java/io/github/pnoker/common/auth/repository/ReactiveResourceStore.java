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

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
package io.github.pnoker.common.agentic.repository;

import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped model configurations. */
public interface ReactiveModelConfigStore {

    Flux<ModelConfigBO> list(RequestHeader.PrincipalHeader header, boolean enabledOnly);

    Mono<ModelConfigBO> get(Long id, RequestHeader.PrincipalHeader header);

    Mono<ModelConfigBO> findByModel(String model, RequestHeader.PrincipalHeader header);

    Mono<ModelConfigBO> findDefault(RequestHeader.PrincipalHeader header);

    Mono<ModelConfigBO> insert(ModelConfigBO config, RequestHeader.PrincipalHeader header);

    Mono<ModelConfigBO> update(ModelConfigBO config, RequestHeader.PrincipalHeader header);

    Mono<Boolean> delete(Long id, RequestHeader.PrincipalHeader header);
}

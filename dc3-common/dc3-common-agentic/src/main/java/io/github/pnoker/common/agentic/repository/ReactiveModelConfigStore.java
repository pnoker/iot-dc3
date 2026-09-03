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

    /** Stream model configs matching the request. */
    Flux<ModelConfigBO> list(RequestHeader.PrincipalHeader header, boolean enabledOnly);

    /** Load the model config for the request. */
    Mono<ModelConfigBO> get(Long id, RequestHeader.PrincipalHeader header);

    /** Resolve the model config by its model. */
    Mono<ModelConfigBO> findByModel(String model, RequestHeader.PrincipalHeader header);

    /** Load the default for the request. */
    Mono<ModelConfigBO> findDefault(RequestHeader.PrincipalHeader header);

    /** Insert one model config and emit the stored row. */
    Mono<ModelConfigBO> insert(ModelConfigBO config, RequestHeader.PrincipalHeader header);

    /** Update one model config and emit the updated row. */
    Mono<ModelConfigBO> update(ModelConfigBO config, RequestHeader.PrincipalHeader header);

    /** Delete the model config, reporting whether a row was removed. */
    Mono<Boolean> delete(Long id, RequestHeader.PrincipalHeader header);
}

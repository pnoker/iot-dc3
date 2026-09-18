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

import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped model providers. */
public interface ReactiveModelProviderStore {

    /** Stream model providers matching the request. */
    Flux<ModelProviderBO> list(RequestHeader.PrincipalHeader header);

    /** Load the model provider for the request. */
    Mono<ModelProviderBO> get(Long id, RequestHeader.PrincipalHeader header);

    /** Insert one model provider and emit the stored row. */
    Mono<ModelProviderBO> insert(ModelProviderBO provider, RequestHeader.PrincipalHeader header);

    /** Update one model provider and emit the updated row. */
    Mono<ModelProviderBO> update(ModelProviderBO provider, RequestHeader.PrincipalHeader header);

    /** Delete the model provider, reporting whether a row was removed. */
    Mono<Boolean> delete(Long id, RequestHeader.PrincipalHeader header);
}

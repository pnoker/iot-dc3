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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped command parameters. */
public interface ReactiveCommandParamStore {
    Mono<CommandParamBO> get(Long tenantId, Long id);

    Flux<CommandParamBO> listByCommandId(Long tenantId, Long commandId);

    Flux<CommandParamBO> listByIds(Long tenantId, Collection<Long> ids);

    Mono<Boolean> existsByNameOrCode(
            Long tenantId, Long commandId, String paramName, String paramCode, Long excludedId);

    Mono<CommandParamBO> insert(CommandParamBO value);

    Mono<CommandParamBO> update(CommandParamBO value, int expectedVersion);

    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    Mono<Long> deleteByCommandId(Long tenantId, Long commandId, Long operatorId, String operatorName);

    Mono<OffsetPage<CommandParamBO>> list(CommandParamFilter filter);
}

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
package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.repository.CommandFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive application service for command metadata. */
public interface ReactiveCommandService {
    /** Resolve the command by its id. */
    Mono<CommandBO> getById(Long tenantId, Long id);

    /** Add one command. */
    Mono<CommandBO> add(CommandBO value);

    /** Update one command and emit the updated row. */
    Mono<CommandBO> update(CommandBO value);

    /** Delete the command, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** List commands matched by ids. */
    Flux<CommandBO> listByIds(Long tenantId, List<Long> ids);

    /** List commands matched by profile id. */
    Flux<CommandBO> listByProfileId(Long tenantId, Long profileId);

    /** List commands matched by device id. */
    Flux<CommandBO> listByDeviceId(Long tenantId, Long deviceId);

    /** Page commands matching the tenant-scoped filters. */
    Mono<OffsetPage<CommandBO>> list(CommandFilter filter);
}

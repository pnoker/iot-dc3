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

import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped commands. */
public interface ReactiveCommandStore {
    /** Load the command scoped to the tenant by id. */
    Mono<CommandBO> get(Long tenantId, Long id);

    /** Check whether a record exists for the given name or code. */
    Mono<Boolean> existsByNameOrCode(
            Long tenantId, Long profileId, String commandName, String commandCode, Long excludingId);

    /** Insert one command and emit the stored row. */
    Mono<CommandBO> insert(CommandBO value);

    /** Update one command and emit the updated row. */
    Mono<CommandBO> update(CommandBO value, int expectedVersion);

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

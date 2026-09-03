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

import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** Reactive persistence port for command attribute config records. */

public interface ReactiveCommandAttributeConfigStore {
    /** Load the command attribute config scoped to the tenant by id. */
    Mono<CommandAttributeConfigBO> get(Long tenantId, Long id);

    /** Resolve the command attribute config by its attribute device command. */
    Mono<CommandAttributeConfigBO> getByAttributeDeviceCommand(
            Long tenantId, Long attributeId, Long deviceId, Long commandId);

    /** List command attribute configs matched by device id. */
    Flux<CommandAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId);

    /** List command attribute configs matched by device id and command id. */
    Flux<CommandAttributeConfigBO> listByDeviceIdAndCommandId(Long tenantId, Long deviceId, Long commandId);

    /** Insert one command attribute config and emit the stored row. */
    Mono<CommandAttributeConfigBO> insert(CommandAttributeConfigBO value);

    /** Update one command attribute config and emit the updated row. */
    Mono<CommandAttributeConfigBO> update(CommandAttributeConfigBO value, int expectedVersion);

    /** Delete the command attribute config, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Page command attribute configs matching the tenant-scoped filters. */
    Mono<OffsetPage<CommandAttributeConfigBO>> list(CommandAttributeConfigFilter filter);
}

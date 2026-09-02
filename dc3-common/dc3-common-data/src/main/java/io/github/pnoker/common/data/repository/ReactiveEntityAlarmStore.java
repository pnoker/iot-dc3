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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import java.util.List;
import reactor.core.publisher.Mono;

/** Reactive, tenant-scoped persistence port for entity alarms. */
public interface ReactiveEntityAlarmStore {

    /** Inserts one alarm and returns the persisted row including its generated id. */
    Mono<EntityAlarmDO> insert(EntityAlarmDO alarm);

    /** Inserts a batch atomically and returns the persisted rows in input order. */
    Mono<List<EntityAlarmDO>> insertBatch(List<EntityAlarmDO> alarms);

    /** Deletes one alarm only when it belongs to the supplied tenant. */
    Mono<Boolean> delete(Long tenantId, Long alarmId);

    /** Updates confirmation state only within the supplied tenant. */
    Mono<Boolean> updateConfirm(Long tenantId, Long alarmId, byte confirmFlag);
}

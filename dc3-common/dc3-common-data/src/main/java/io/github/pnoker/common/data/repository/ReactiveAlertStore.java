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

import io.github.pnoker.common.data.entity.bo.dashboard.AlertItemRow;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import java.time.LocalDateTime;
import reactor.core.publisher.Mono;

/** Reactive, tenant-scoped read/write port for dashboard alert projections. */
public interface ReactiveAlertStore {

    /** Lists alert rows using canonical offset pagination and whitelisted sorting. */
    Mono<OffsetPage<AlertItemRow>> list(
            Long tenantId,
            String source,
            Integer alarmTypeFlag,
            Integer confirmFlag,
            LocalDateTime from,
            PageRequest page);

    /** Updates confirmation state for one tenant-owned alert row and source. */
    Mono<Boolean> updateConfirm(Long tenantId, String source, Long id, byte confirmFlag);
}

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

import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/** Reactive, tenant-scoped persistence port for custom command history. */
public interface ReactiveCommandHistoryStore {

    /** Load the command history record by its record id. */
    Mono<CommandHistoryDO> find(Long tenantId, String recordId);

    /** Insert one command history entry and emit the stored row. */
    Mono<CommandHistoryDO> insert(CommandHistoryDO history);

    /** Mark the command sent at the given instant, reporting whether it was still pending. */
    Mono<Boolean> markSent(Long tenantId, String recordId, Instant sentAt);

    /** Mark the command publish-failed with the error, reporting whether it was updated. */
    Mono<Boolean> markPublishFailed(
            Long tenantId, String recordId, String errorCode, String errorMessage, Instant finishedAt);

    /** Close the command with the final status, response and error, reporting whether it was updated. */
    Mono<Boolean> complete(
            Long tenantId,
            String recordId,
            PointCommandStatusEnum status,
            String resultValues,
            String configSnapshot,
            String errorCode,
            String errorMessage,
            Instant finishedAt);

    /** Mark the command dead with the error, reporting whether it was updated. */
    Mono<Boolean> markDead(Long tenantId, String recordId, String errorCode, String errorMessage, Instant finishedAt);

    /** Page command history entries matching the tenant-scoped filters. */
    Mono<OffsetPage<CommandHistoryDO>> list(
            Long tenantId,
            Long deviceId,
            Long commandId,
            String commandCode,
            PointCommandStatusEnum status,
            long offset,
            int limit,
            List<SortSpec> sort);
}

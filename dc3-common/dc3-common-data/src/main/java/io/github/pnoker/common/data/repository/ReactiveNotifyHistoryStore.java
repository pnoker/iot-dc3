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

import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped persistence for notification delivery history. */
public interface ReactiveNotifyHistoryStore {

    /** Load the notify configuration history scoped to the tenant by id. */
    Mono<NotifyHistoryDO> get(long tenantId, long historyId);

    /** Page notify configuration histories matching the tenant-scoped filters. */
    Mono<OffsetPage<NotifyHistoryDO>> list(
            long tenantId,
            Long ruleId,
            Long notifyId,
            Long messageId,
            Long channelId,
            Long alarmId,
            NotifyChannelTypeEnum channelTypeFlag,
            String target,
            NotifyHistoryStatusEnum statusFlag,
            PageRequest page);

    /** Delete the notify configuration history, reporting whether a row was removed. */
    Mono<Boolean> delete(long tenantId, long historyId);

    /** Insert one notify configuration history and emit the stored row. */
    Mono<NotifyHistoryDO> insert(NotifyHistoryDO history);

    /** Insert the history row idempotently, emitting the stored row. */
    Mono<NotifyHistoryInsertResult> insertIdempotent(NotifyHistoryDO history);

    /** Update one delivery and emit the updated row. */
    Mono<Boolean> updateDelivery(
            long tenantId,
            long historyId,
            byte statusFlag,
            String target,
            Object responseExt,
            String errorMessage,
            int retryCount);
}

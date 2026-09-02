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

    Mono<NotifyHistoryDO> get(long tenantId, long historyId);

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

    Mono<Boolean> delete(long tenantId, long historyId);

    Mono<NotifyHistoryDO> insert(NotifyHistoryDO history);

    Mono<NotifyHistoryInsertResult> insertIdempotent(NotifyHistoryDO history);

    Mono<Boolean> updateDelivery(
            long tenantId,
            long historyId,
            byte statusFlag,
            String target,
            Object responseExt,
            String errorMessage,
            int retryCount);
}

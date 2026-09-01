package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped persistence for notification delivery history. */
public interface ReactiveNotifyHistoryStore {

    Mono<NotifyHistoryDO> get(long tenantId, long historyId);

    Mono<OffsetPage<NotifyHistoryDO>> list(long tenantId, Long ruleId, Long notifyId, Long messageId, Long channelId,
                                           Long alarmId, NotifyChannelTypeEnum channelTypeFlag, String target,
                                           NotifyHistoryStatusEnum statusFlag, PageRequest page);

    Mono<Boolean> delete(long tenantId, long historyId);

    Mono<NotifyHistoryDO> insert(NotifyHistoryDO history);

    Mono<NotifyHistoryInsertResult> insertIdempotent(NotifyHistoryDO history);

    Mono<Boolean> updateDelivery(long tenantId, long historyId, byte statusFlag, String target,
                                 Object responseExt, String errorMessage, int retryCount);
}

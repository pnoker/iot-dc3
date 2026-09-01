package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped reads for notification configuration used by alarm delivery. */
public interface ReactiveNotifyConfigStore {

    Mono<NotifyBO> getNotify(long tenantId, long id);

    Mono<MessageBO> getMessage(long tenantId, long id);

    Mono<NotifyChannelBO> getChannel(long tenantId, long id);

    Flux<NotifyChannelBindBO> listEnabledBinds(long tenantId, long notifyId);
}

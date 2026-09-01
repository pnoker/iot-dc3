package io.github.pnoker.common.data.service.impl;

import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.builder.NotifyHistoryBuilder;
import io.github.pnoker.common.data.entity.query.NotifyHistoryQuery;
import io.github.pnoker.common.data.repository.ReactiveNotifyHistoryStore;
import io.github.pnoker.common.data.service.NotifyHistoryService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotifyHistoryServiceImpl implements NotifyHistoryService {

    private final ReactiveNotifyHistoryStore notifyHistoryStore;
    private final NotifyHistoryBuilder notifyHistoryBuilder;

    @Override
    public Mono<NotifyHistoryBO> getById(Long tenantId, Long id) {
        return notifyHistoryStore.get(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Notify history does not exist")))
                .map(notifyHistoryBuilder::buildBOByDO);
    }

    @Override
    public Mono<OffsetPage<NotifyHistoryBO>> list(Long tenantId, NotifyHistoryQuery query) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            NotifyHistoryQuery request = Objects.requireNonNullElseGet(query, NotifyHistoryQuery::new);
            PageRequest page = new PageRequest(request.getOffset(), request.getLimit(), request.getSort());
            return notifyHistoryStore.list(tenantId, request.getRuleId(), request.getNotifyId(), request.getMessageId(),
                            request.getChannelId(), request.getAlarmId(), request.getChannelTypeFlag(), request.getTarget(),
                            request.getStatusFlag(), page)
                    .map(result -> OffsetPage.of(result.items().stream().map(notifyHistoryBuilder::buildBOByDO).toList(),
                            result.offset(), result.limit(), result.total()));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id) {
        return notifyHistoryStore.delete(value(tenantId), value(id));
    }

    private long value(Long value) { return value == null ? 0 : value; }
    private void requireTenant(Long tenantId) { if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required"); }
}

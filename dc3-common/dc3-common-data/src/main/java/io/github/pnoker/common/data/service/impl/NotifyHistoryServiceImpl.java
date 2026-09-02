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
package io.github.pnoker.common.data.service.impl;

import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.builder.NotifyHistoryBuilder;
import io.github.pnoker.common.data.entity.query.NotifyHistoryQuery;
import io.github.pnoker.common.data.repository.ReactiveNotifyHistoryStore;
import io.github.pnoker.common.data.service.NotifyHistoryService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NotifyHistoryServiceImpl implements NotifyHistoryService {

    private final ReactiveNotifyHistoryStore notifyHistoryStore;
    private final NotifyHistoryBuilder notifyHistoryBuilder;

    @Override
    public Mono<NotifyHistoryBO> getById(Long tenantId, Long id) {
        return notifyHistoryStore
                .get(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Notify history does not exist")))
                .map(notifyHistoryBuilder::buildBOByDO);
    }

    @Override
    public Mono<OffsetPage<NotifyHistoryBO>> list(Long tenantId, NotifyHistoryQuery query) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            NotifyHistoryQuery request = Objects.requireNonNullElseGet(query, NotifyHistoryQuery::new);
            PageRequest page = new PageRequest(request.getOffset(), request.getLimit(), request.getSort());
            return notifyHistoryStore
                    .list(
                            tenantId,
                            request.getRuleId(),
                            request.getNotifyId(),
                            request.getMessageId(),
                            request.getChannelId(),
                            request.getAlarmId(),
                            request.getChannelTypeFlag(),
                            request.getTarget(),
                            request.getStatusFlag(),
                            page)
                    .map(result -> OffsetPage.of(
                            result.items().stream()
                                    .map(notifyHistoryBuilder::buildBOByDO)
                                    .toList(),
                            result.offset(),
                            result.limit(),
                            result.total()));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id) {
        return notifyHistoryStore.delete(value(tenantId), value(id));
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required");
    }
}

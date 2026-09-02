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

import io.github.pnoker.common.data.biz.alarm.NotifyConfigCache;
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.query.MessageQuery;
import io.github.pnoker.common.data.repository.ReactiveNotifyAdminStore;
import io.github.pnoker.common.data.service.MessageService;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final MessageBuilder builder;
    private final ReactiveNotifyAdminStore store;
    private final NotifyConfigCache cache;

    @Override
    public Mono<MessageBO> add(MessageBO value) {
        return Mono.defer(() -> {
            validate(value);
            MessageDO data = builder.buildDOByBO(value);
            return store.existsMessageCode(value(value.getTenantId()), data.getMessageCode(), null)
                    .flatMap(exists -> exists
                            ? Mono.<MessageBO>error(new DuplicateException("Alarm message has been duplicated"))
                            : store.insertMessage(data)
                                    .map(builder::buildBOByDO)
                                    .switchIfEmpty(Mono.error(new AddException("Failed to create alarm message"))))
                    .doOnSuccess(result -> {
                        if (result != null) cache.invalidateMessage(result.getId());
                    })
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Alarm message has been duplicated"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id) {
        return store.getMessage(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Alarm message does not exist")))
                .then(store.deleteMessage(value(tenantId), value(id)))
                .flatMap(ok -> ok ? Mono.just(true) : Mono.error(new DeleteException("Failed to remove alarm message")))
                .doOnSuccess(ok -> cache.invalidateMessage(id));
    }

    @Override
    public Mono<MessageBO> update(MessageBO value) {
        return Mono.defer(() -> {
            validate(value);
            MessageDO data = builder.buildDOByBO(value);
            return store.getMessage(value(value.getTenantId()), value(value.getId()))
                    .switchIfEmpty(Mono.error(new NotFoundException("Alarm message does not exist")))
                    .then(store.existsMessageCode(value(value.getTenantId()), data.getMessageCode(), value.getId()))
                    .flatMap(exists -> exists
                            ? Mono.<MessageBO>error(new DuplicateException("Alarm message has been duplicated"))
                            : store.updateMessage(data)
                                    .map(builder::buildBOByDO)
                                    .switchIfEmpty(Mono.error(new UpdateException("Failed to update alarm message"))))
                    .doOnSuccess(result -> {
                        if (result != null) cache.invalidateMessage(result.getId());
                    })
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Alarm message has been duplicated"));
        });
    }

    @Override
    public Mono<MessageBO> getById(Long tenantId, Long id) {
        return store.getMessage(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Alarm message does not exist")))
                .map(builder::buildBOByDO);
    }

    @Override
    public Mono<OffsetPage<MessageBO>> list(Long tenantId, MessageQuery query) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            MessageQuery request = query == null ? new MessageQuery() : query;
            PageRequest page = new PageRequest(request.getOffset(), request.getLimit(), request.getSort());
            return store.listMessage(
                            tenantId,
                            request.getMessageName(),
                            request.getMessageCode(),
                            request.getMessageLevel(),
                            request.getEnableFlag(),
                            page)
                    .map(result -> OffsetPage.of(
                            result.items().stream().map(builder::buildBOByDO).toList(),
                            result.offset(),
                            result.limit(),
                            result.total()));
        });
    }

    private void validate(MessageBO value) {
        if (value == null || value.getTenantId() == null || value.getTenantId() <= 0)
            throw new IllegalArgumentException("tenantId is required");
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required");
    }
}

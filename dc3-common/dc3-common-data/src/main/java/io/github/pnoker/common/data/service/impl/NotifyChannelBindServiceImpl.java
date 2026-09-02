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
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.query.NotifyChannelBindQuery;
import io.github.pnoker.common.data.repository.ReactiveNotifyAdminStore;
import io.github.pnoker.common.data.service.NotifyChannelBindService;
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
public class NotifyChannelBindServiceImpl implements NotifyChannelBindService {
    private final NotifyChannelBindBuilder builder;
    private final ReactiveNotifyAdminStore store;
    private final NotifyConfigCache cache;

    @Override
    public Mono<NotifyChannelBindBO> add(NotifyChannelBindBO value) {
        return Mono.defer(() -> {
            validate(value);
            return refs(value)
                    .then(store.existsBind(
                            value(value.getTenantId()), value(value.getNotifyId()), value(value.getChannelId()), null))
                    .flatMap(exists -> {
                        if (exists)
                            return Mono.error(new DuplicateException("Notify channel binding has been duplicated"));
                        NotifyChannelBindDO data = builder.buildDOByBO(value);
                        return store.insertBind(data)
                                .map(builder::buildBOByDO)
                                .switchIfEmpty(Mono.error(new AddException("Failed to create notify channel binding")));
                    })
                    .doOnSuccess(result -> {
                        if (result != null) cache.invalidateBinds(result.getTenantId(), result.getNotifyId());
                    })
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Notify channel binding has been duplicated"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id) {
        return store.getBind(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Notify channel binding does not exist")))
                .flatMap(existing -> store.deleteBind(value(tenantId), value(id))
                        .flatMap(ok -> ok
                                ? Mono.just(true)
                                : Mono.error(new DeleteException("Failed to remove notify channel binding")))
                        .doOnSuccess(ok -> cache.invalidateBinds(existing.getTenantId(), existing.getNotifyId())));
    }

    @Override
    public Mono<NotifyChannelBindBO> update(NotifyChannelBindBO value) {
        return Mono.defer(() -> {
            validate(value);
            return store.getBind(value(value.getTenantId()), value(value.getId()))
                    .switchIfEmpty(Mono.error(new NotFoundException("Notify channel binding does not exist")))
                    .flatMap(existing -> refs(value)
                            .then(store.existsBind(
                                    value(value.getTenantId()),
                                    value(value.getNotifyId()),
                                    value(value.getChannelId()),
                                    value.getId()))
                            .flatMap(exists -> {
                                if (exists)
                                    return Mono.error(
                                            new DuplicateException("Notify channel binding has been duplicated"));
                                NotifyChannelBindDO data = builder.buildDOByBO(value);
                                return store.updateBind(data)
                                        .map(builder::buildBOByDO)
                                        .switchIfEmpty(Mono.error(
                                                new UpdateException("Failed to update notify channel binding")))
                                        .doOnSuccess(updated -> {
                                            cache.invalidateBinds(existing.getTenantId(), existing.getNotifyId());
                                            if (updated != null)
                                                cache.invalidateBinds(updated.getTenantId(), updated.getNotifyId());
                                        });
                            }));
        });
    }

    @Override
    public Mono<NotifyChannelBindBO> getById(Long tenantId, Long id) {
        return store.getBind(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Notify channel binding does not exist")))
                .map(builder::buildBOByDO);
    }

    @Override
    public Mono<OffsetPage<NotifyChannelBindBO>> list(Long tenantId, NotifyChannelBindQuery query) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            NotifyChannelBindQuery request = query == null ? new NotifyChannelBindQuery() : query;
            PageRequest page = new PageRequest(request.getOffset(), request.getLimit(), request.getSort());
            return store.listBind(
                            tenantId, request.getNotifyId(), request.getChannelId(), request.getEnableFlag(), page)
                    .map(result -> OffsetPage.of(
                            result.items().stream().map(builder::buildBOByDO).toList(),
                            result.offset(),
                            result.limit(),
                            result.total()));
        });
    }

    private Mono<Void> refs(NotifyChannelBindBO value) {
        return store.existsNotify(value(value.getTenantId()), value(value.getNotifyId()))
                .flatMap(notify -> notify
                        ? store.existsChannel(value(value.getTenantId()), value(value.getChannelId()))
                        : Mono.just(false))
                .flatMap(ok -> ok
                        ? Mono.empty()
                        : Mono.error(new NotFoundException("Notify policy or channel does not exist")));
    }

    private void validate(NotifyChannelBindBO value) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getNotifyId() == null
                || value.getChannelId() == null)
            throw new IllegalArgumentException("tenantId, notifyId and channelId are required");
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required");
    }
}

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
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.data.entity.query.NotifyQuery;
import io.github.pnoker.common.data.repository.ReactiveNotifyAdminStore;
import io.github.pnoker.common.data.service.NotifyService;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
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
public class NotifyServiceImpl implements NotifyService {
    private final NotifyBuilder builder;
    private final ReactiveNotifyAdminStore store;
    private final NotifyConfigCache cache;

    @Override
    public Mono<NotifyBO> add(NotifyBO value) {
        return Mono.defer(() -> {
            validate(value);
            NotifyDO data = builder.buildDOByBO(value);
            return store.existsNotifyCode(value(value.getTenantId()), data.getNotifyCode(), null)
                    .flatMap(exists -> exists
                            ? Mono.<NotifyBO>error(new DuplicateException("Alarm notify profile has been duplicated"))
                            : store.insertNotify(data)
                                    .map(builder::buildBOByDO)
                                    .switchIfEmpty(
                                            Mono.error(new AddException("Failed to create alarm notify profile"))))
                    .doOnSuccess(result -> {
                        if (result != null) cache.invalidateNotify(result.getId());
                    })
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Alarm notify profile has been duplicated"));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id) {
        return store.getNotify(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Alarm notify profile does not exist")))
                .flatMap(existing -> store.hasNotifyBindings(value(tenantId), value(id))
                        .flatMap(has -> has
                                ? Mono.<Boolean>error(new AssociatedException(
                                        "Failed to remove alarm notify profile: notify channel bindings exist"))
                                : store.deleteNotify(value(tenantId), value(id))))
                .flatMap(deleted -> deleted
                        ? Mono.just(true)
                        : Mono.error(new DeleteException("Failed to remove alarm notify profile")))
                .doOnSuccess(ok -> cache.invalidateNotify(id));
    }

    @Override
    public Mono<NotifyBO> update(NotifyBO value) {
        return Mono.defer(() -> {
            validate(value);
            NotifyDO data = builder.buildDOByBO(value);
            return store.getNotify(value(value.getTenantId()), value(value.getId()))
                    .switchIfEmpty(Mono.error(new NotFoundException("Alarm notify profile does not exist")))
                    .then(store.existsNotifyCode(value(value.getTenantId()), data.getNotifyCode(), value.getId()))
                    .flatMap(exists -> exists
                            ? Mono.<NotifyBO>error(new DuplicateException("Alarm notify profile has been duplicated"))
                            : store.updateNotify(data)
                                    .map(builder::buildBOByDO)
                                    .switchIfEmpty(
                                            Mono.error(new UpdateException("Failed to update alarm notify profile"))))
                    .doOnSuccess(result -> {
                        if (result != null) cache.invalidateNotify(result.getId());
                    })
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Alarm notify profile has been duplicated"));
        });
    }

    @Override
    public Mono<NotifyBO> getById(Long tenantId, Long id) {
        return store.getNotify(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Alarm notify profile does not exist")))
                .map(builder::buildBOByDO);
    }

    @Override
    public Mono<OffsetPage<NotifyBO>> list(Long tenantId, NotifyQuery query) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            NotifyQuery request = query == null ? new NotifyQuery() : query;
            PageRequest page = page(request.getOffset(), request.getLimit(), request.getSort());
            return store.listNotify(
                            tenantId,
                            request.getNotifyName(),
                            request.getNotifyCode(),
                            request.getAutoConfirmFlag(),
                            request.getNotifyInterval(),
                            request.getEnableFlag(),
                            page)
                    .map(result -> OffsetPage.of(
                            result.items().stream().map(builder::buildBOByDO).toList(),
                            result.offset(),
                            result.limit(),
                            result.total()));
        });
    }

    private void validate(NotifyBO value) {
        if (value == null || value.getTenantId() == null || value.getTenantId() <= 0)
            throw new IllegalArgumentException("tenantId is required");
    }

    private PageRequest page(
            long offset, int limit, java.util.List<io.github.pnoker.db.r2dbc.core.page.SortSpec> sort) {
        return new PageRequest(offset, limit, sort);
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required");
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }
}

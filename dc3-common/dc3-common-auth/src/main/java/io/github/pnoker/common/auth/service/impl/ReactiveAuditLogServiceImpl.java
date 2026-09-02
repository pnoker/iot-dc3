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
package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import io.github.pnoker.common.auth.entity.builder.IdentityAuditLogBuilder;
import io.github.pnoker.common.auth.entity.vo.IdentityAuditLogVO;
import io.github.pnoker.common.auth.repository.IdentityAuditLogFilter;
import io.github.pnoker.common.auth.repository.ReactiveAuditLogQueryStore;
import io.github.pnoker.common.auth.repository.ReactiveAuditLogStore;
import io.github.pnoker.common.auth.service.ReactiveAuditLogService;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default non-blocking audit service with best-effort failure isolation. */
@Slf4j
@Service
public class ReactiveAuditLogServiceImpl implements ReactiveAuditLogService {

    private final ReactiveAuditLogStore store;
    private final ReactiveAuditLogQueryStore queryStore;
    private final IdentityAuditLogBuilder builder;

    public ReactiveAuditLogServiceImpl(
            ReactiveAuditLogStore store, ReactiveAuditLogQueryStore queryStore, IdentityAuditLogBuilder builder) {
        this.store = store;
        this.queryStore = queryStore;
        this.builder = builder;
    }

    @Override
    public Mono<Void> log(
            Long tenantId,
            Long principalId,
            String principalType,
            String action,
            String resourceType,
            Long resourceId,
            String resourceName,
            String status,
            String errorCode) {
        return Mono.defer(() -> {
                    if (tenantId == null || tenantId <= 0) {
                        return Mono.error(new IllegalArgumentException("audit tenantId is required"));
                    }
                    IdentityAuditLogBO event = ReactiveAuditLogService.event(
                            tenantId,
                            principalId,
                            principalType,
                            action,
                            resourceType,
                            resourceId,
                            resourceName,
                            status,
                            errorCode);
                    return store.append(event);
                })
                .onErrorResume(error -> {
                    log.warn(
                            "Failed to record identity audit log (action={}, resourceType={}, resourceId={})",
                            action,
                            resourceType,
                            resourceId,
                            error);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<CursorPage<IdentityAuditLogVO>> list(IdentityAuditLogFilter filter) {
        return Mono.defer(() -> queryStore.list(filter))
                .map(page -> new CursorPage<>(
                        page.items().stream()
                                .map(builder::buildBOByDO)
                                .map(builder::buildVOByBO)
                                .toList(),
                        page.nextCursor(),
                        page.hasNext()));
    }
}

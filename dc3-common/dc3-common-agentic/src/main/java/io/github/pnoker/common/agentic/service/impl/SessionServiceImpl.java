/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.service.impl;

import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.agentic.repository.ReactiveSessionStore;
import io.github.pnoker.common.agentic.service.SessionService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default reactive session service. */
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final ReactiveSessionStore sessionStore;

    @Override
    public Mono<SessionBO> touch(String conversationId, RequestHeader.PrincipalHeader header,
                                 SessionExt sessionExt) {
        return sessionStore.touch(conversationId, sessionExt, header);
    }

    @Override
    public Mono<SessionBO> get(String conversationId, RequestHeader.PrincipalHeader header) {
        return sessionStore.get(conversationId, header);
    }

    @Override
    public Mono<OffsetPage<SessionBO>> list(long offset, int limit, String conversationId,
                                            java.util.List<SortSpec> sort,
                                            RequestHeader.PrincipalHeader header) {
        return sessionStore.list(offset, limit, conversationId, sort, header);
    }

    @Override
    public Mono<SessionBO> update(String conversationId, SessionExt sessionExt, String title,
                                  RequestHeader.PrincipalHeader header) {
        return sessionStore.update(conversationId, sessionExt, title, header);
    }

    @Override
    public Mono<Long> delete(String conversationId, RequestHeader.PrincipalHeader header) {
        return sessionStore.delete(conversationId, header);
    }
}

/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.repository;

import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.model.SessionExt;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import reactor.core.publisher.Mono;

/** Reactive persistence port for agentic conversation sessions. */
public interface ReactiveSessionStore {

    Mono<SessionBO> touch(String conversationId, SessionExt sessionExt,
                          RequestHeader.PrincipalHeader header);

    Mono<SessionBO> get(String conversationId, RequestHeader.PrincipalHeader header);

    Mono<OffsetPage<SessionBO>> list(long offset, int limit, String conversationId,
                                     java.util.List<SortSpec> sort,
                                     RequestHeader.PrincipalHeader header);

    Mono<SessionBO> update(String conversationId, SessionExt sessionExt, String title,
                           RequestHeader.PrincipalHeader header);

    Mono<Long> delete(String conversationId, RequestHeader.PrincipalHeader header);
}

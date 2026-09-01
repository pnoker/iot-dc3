/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.repository;

import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/** Reactive persistence port for tenant-scoped agentic actions. */
public interface ReactiveActionStore {

    /** Persist a newly created action. */
    Mono<ActionBO> create(ActionBO action);

    /** Find an action owned by the authenticated tenant and user. */
    Mono<ActionBO> find(String actionId, RequestHeader.PrincipalHeader header);

    /** List non-expired pending actions for a conversation. */
    Mono<OffsetPage<ActionBO>> listPending(long offset, int limit, String conversationId,
                                           RequestHeader.PrincipalHeader header, Instant now);

    /** Atomically claim a pending action, returning empty when the claim loses a race. */
    Mono<ActionBO> claimPending(String actionId, RequestHeader.PrincipalHeader header,
                                AgenticActionStatusEnum nextStatus, Instant now);

    /** Persist the result of an already claimed action. */
    Mono<ActionBO> updateExecutionResult(String actionId, RequestHeader.PrincipalHeader header,
                                         AgenticActionStatusEnum status, String remark, Instant now);
}

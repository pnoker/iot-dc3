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
package io.github.pnoker.common.agentic.service;

import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;


/**
 * Service for managing agentic actions including write-point-value confirmation and rejection.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface ActionService {

    /**
     * Create and persist a pending write action through the reactive repository.
     *
     * @param conversationId the conversation the action belongs to
     * @param deviceId target device
     * @param pointId target point
     * @param value value to write
     * @param header authenticated caller principal and tenant
     * @return the created action id
     */
    Mono<String> createWritePointValueAction(String conversationId, Long deviceId, Long pointId,
                                              String value, RequestHeader.PrincipalHeader header);

    /**
     * List pending, non-expired actions for a conversation.
     *
     * @param offset zero-based result offset
     * @param limit maximum number of rows
     * @param conversationId the conversation to query
     * @param header authenticated caller principal and tenant
     * @return an offset page of pending actions
     */
    Mono<OffsetPage<ActionBO>> listPending(long offset, int limit, String conversationId,
                                           RequestHeader.PrincipalHeader header);

    /**
     * Confirm a pending action and execute it. For a write-point-value action this
     * dispatches the write command and transitions the action to EXECUTED or FAILED.
     *
     * @param actionId the action to confirm
     * @param header   authenticated caller principal and tenant
     * @return the updated action
     */
    Mono<ActionBO> confirm(String actionId, RequestHeader.PrincipalHeader header);

    /**
     * Reject a pending action, transitioning it to REJECTED without executing it.
     *
     * @param actionId the action to reject
     * @param header   authenticated caller principal and tenant
     * @return the updated action
     */
    Mono<ActionBO> reject(String actionId, RequestHeader.PrincipalHeader header);

}

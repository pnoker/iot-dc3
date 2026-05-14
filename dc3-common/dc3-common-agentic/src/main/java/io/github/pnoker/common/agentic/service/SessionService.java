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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.query.SessionQuery;
import io.github.pnoker.common.agentic.entity.request.SessionUpdateRequest;

/**
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface SessionService {

    /**
     * Create a new session if one does not exist for the given conversation ID. If a
     * session already exists, update its operate_time and latest model.
     *
     * @param conversationId conversation ID
     * @param tenantId       tenant scope
     * @param userId         user scope
     * @param model          latest selected model
     * @return the session BO
     */
    SessionBO touch(String conversationId, Long tenantId, Long userId, String model);

    /**
     * Get session by conversation ID.
     *
     * @param conversationId conversation ID
     * @return session BO or null
     */
    SessionBO getByConversationId(String conversationId);

    /**
     * Delete session by conversation ID (logical delete) and clear associated chat
     * memory.
     *
     * @param conversationId conversation ID
     */
    void removeByConversationId(String conversationId);

    /**
     * Update mutable session metadata.
     *
     * @param conversationId conversation ID
     * @param request        mutable fields
     * @return updated session BO or null if the session does not exist
     */
    SessionBO update(String conversationId, SessionUpdateRequest request);

    /**
     * Query sessions with pagination.
     *
     * @param query query parameters
     * @return paginated results
     */
    Page<SessionBO> selectByPage(SessionQuery query);

}

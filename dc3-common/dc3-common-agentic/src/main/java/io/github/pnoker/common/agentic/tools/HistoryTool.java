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
package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.facade.api.CommandHistoryFacade;
import io.github.pnoker.common.facade.api.EventHistoryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandHistoryBO;
import io.github.pnoker.common.facade.entity.bo.FacadeEventHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * History tools reading command and event execution records, closing the traceability loop
 * after commands are dispatched.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryTool {

    private final Optional<CommandHistoryFacade> commandHistoryFacade;

    private final Optional<EventHistoryFacade> eventHistoryFacade;

    /**
     * Look up one command history record by its record identifier.
     *
     * @param recordId record identifier
     * @param toolContext tool context
     * @return command history result
     */
    public Mono<AgenticToolResult<FacadeCommandHistoryBO>> lookupCommandHistoryByRecordId(
            String recordId, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "lookupCommandHistoryByRecordId", tenantId);
        if (Objects.isNull(recordId) || recordId.isBlank()) {
            return Mono.just(AgenticToolResult.invalid("Record ID must not be blank."));
        }
        CommandHistoryFacade facade = commandHistoryFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.getByRecordIdReactive(tenantId, recordId)
                .map(record -> AgenticToolResult.ok("Command history loaded", record))
                .defaultIfEmpty(
                        AgenticToolResult.notFound("Command history does not exist for record ID: " + recordId));
    }

    /**
     * Search command history records with canonical offset pagination.
     *
     * @param deviceId optional device id filter
     * @param commandId optional command id filter
     * @param commandCode optional command code filter
     * @param status optional status code filter
     * @param offset canonical pagination offset
     * @param limit canonical pagination limit, 1..200
     * @param toolContext tool context
     * @return command history page result
     */
    public Mono<AgenticToolResult<OffsetPage<FacadeCommandHistoryBO>>> searchCommandHistories(
            String deviceId,
            String commandId,
            String commandCode,
            String status,
            long offset,
            int limit,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "searchCommandHistories", tenantId);
        if (offset < 0) {
            return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
        }
        if (limit < 1 || limit > 200) {
            return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
        }
        CommandHistoryFacade facade = commandHistoryFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.listReactive(tenantId, deviceId, commandId, commandCode, status, offset, limit)
                .map(page -> page.items().isEmpty()
                        ? AgenticToolResult.empty("No command history found.", page)
                        : AgenticToolResult.ok("Command history loaded", page));
    }

    /**
     * Look up one event history record by its record identifier.
     *
     * @param recordId record identifier
     * @param toolContext tool context
     * @return event history result
     */
    public Mono<AgenticToolResult<FacadeEventHistoryBO>> lookupEventHistoryByRecordId(
            String recordId, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "lookupEventHistoryByRecordId", tenantId);
        if (Objects.isNull(recordId) || recordId.isBlank()) {
            return Mono.just(AgenticToolResult.invalid("Record ID must not be blank."));
        }
        EventHistoryFacade facade = eventHistoryFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.getByRecordIdReactive(tenantId, recordId)
                .map(record -> AgenticToolResult.ok("Event history loaded", record))
                .defaultIfEmpty(AgenticToolResult.notFound("Event history does not exist for record ID: " + recordId));
    }

    /**
     * Search event history records with canonical offset pagination.
     *
     * @param deviceId optional device id filter
     * @param eventId optional event id filter
     * @param offset canonical pagination offset
     * @param limit canonical pagination limit, 1..200
     * @param toolContext tool context
     * @return event history page result
     */
    public Mono<AgenticToolResult<OffsetPage<FacadeEventHistoryBO>>> searchEventHistories(
            String deviceId, String eventId, long offset, int limit, ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}", "searchEventHistories", tenantId);
        if (offset < 0) {
            return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
        }
        if (limit < 1 || limit > 200) {
            return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
        }
        EventHistoryFacade facade = eventHistoryFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return Mono.just(AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE));
        }
        return facade.listReactive(tenantId, deviceId, eventId, offset, limit)
                .map(page -> page.items().isEmpty()
                        ? AgenticToolResult.empty("No event history found.", page)
                        : AgenticToolResult.ok("Event history loaded", page));
    }
}

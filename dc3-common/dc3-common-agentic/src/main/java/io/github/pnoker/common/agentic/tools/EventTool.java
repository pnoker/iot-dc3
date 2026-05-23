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

import io.github.pnoker.common.agentic.annotation.AgenticToolMetadata;
import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeEventQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Event-domain tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventTool {

    private final EventFacade eventFacade;

    @Tool(description = "Look up an event by its numeric ID. Returns event name, code, type (info/alert/fault/lifecycle), level (low/medium/high/critical), and bound profile ID.")
    @AgenticToolMetadata(domain = "event", title = "Query event by ID")
    public AgenticToolResult<FacadeEventBO> lookupEventById(
            @ToolParam(description = "The numeric event ID") Long eventId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, eventId={}", "lookupEventById", tenantId, eventId);
        FacadeEventBO bo = eventFacade.getById(tenantId, eventId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("Event not found for ID: " + eventId);
        }
        return AgenticToolResult.ok("Event loaded", bo);
    }

    @Tool(description = "Batch look up events by numeric IDs. Returns up to 50 tenant-scoped events.")
    @AgenticToolMetadata(domain = "event", title = "Batch query events by IDs")
    public AgenticToolResult<List<FacadeEventBO>> lookupEventsByIds(
            @ToolParam(description = "The numeric event IDs") List<Long> eventIds,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        List<Long> ids = AgenticToolUtil.normalizeIds(eventIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, eventIds={}", "lookupEventsByIds", tenantId, ids);
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid event IDs provided.");
        }
        List<FacadeEventBO> events = eventFacade.listByIds(tenantId, ids);
        if (Objects.isNull(events) || events.isEmpty()) {
            return AgenticToolResult.empty("No events found for IDs: " + ids, List.of());
        }
        return AgenticToolResult.ok("Events loaded", events);
    }

    @Tool(description = "Search for events with optional filters. Returns a paginated list.")
    @AgenticToolMetadata(domain = "event", title = "Search events")
    public AgenticToolResult<FacadePage<FacadeEventBO>> searchEvents(
            @ToolParam(description = "Event name filter (partial match), or null to skip") String eventName,
            @ToolParam(description = "Profile ID filter, or null to skip") Long profileId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, eventName={}, profileId={}, page={}, size={}",
                "searchEvents", tenantId, eventName, profileId, page, size);

        FacadeEventQuery query = new FacadeEventQuery();
        query.setEventName(eventName);
        query.setProfileId(profileId);
        query.setTenantId(tenantId);
        query.setPage(AgenticToolUtil.page(page, size));

        FacadePage<FacadeEventBO> result = eventFacade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No events found.", result);
        }
        return AgenticToolResult.ok("Event page loaded", result);
    }

    @Tool(description = "List events bound to a specific device ID. Use this when the user knows the device but not the event ID.")
    @AgenticToolMetadata(domain = "event", title = "List events by device")
    public AgenticToolResult<FacadePage<FacadeEventBO>> listEventsByDeviceId(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, page={}, size={}", "listEventsByDeviceId",
                tenantId, deviceId, page, size);

        FacadeEventQuery query = new FacadeEventQuery();
        query.setDeviceId(deviceId);
        query.setTenantId(tenantId);
        query.setPage(AgenticToolUtil.page(page, size));

        FacadePage<FacadeEventBO> result = eventFacade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No events found for device ID: " + deviceId, result);
        }
        return AgenticToolResult.ok("Event page loaded for device " + deviceId, result);
    }

    @Tool(description = "List events under a specific profile/template ID. Use this when the user wants all events defined by a template.")
    @AgenticToolMetadata(domain = "event", title = "List events by profile")
    public AgenticToolResult<FacadePage<FacadeEventBO>> listEventsByProfileId(
            @ToolParam(description = "The profile/template ID") Long profileId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}, page={}, size={}",
                "listEventsByProfileId", tenantId, profileId, page, size);

        FacadeEventQuery query = new FacadeEventQuery();
        query.setProfileId(profileId);
        query.setTenantId(tenantId);
        query.setPage(AgenticToolUtil.page(page, size));

        FacadePage<FacadeEventBO> result = eventFacade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No events found for profile ID: " + profileId, result);
        }
        return AgenticToolResult.ok("Event page loaded for profile " + profileId, result);
    }

}

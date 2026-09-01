package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.query.FacadeEventOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/** Non-blocking event-domain tools. */
@Component
@RequiredArgsConstructor
public class EventTool {
    private final EventFacade eventFacade;

    public Mono<AgenticToolResult<FacadeEventBO>> lookupEventByIdReactive(Long eventId, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (eventId == null || eventId <= 0) return Mono.just(AgenticToolResult.invalid("Event ID must be positive."));
        return eventFacade.getById(tenantId, eventId)
                .map(value -> AgenticToolResult.ok("Event loaded", value))
                .defaultIfEmpty(AgenticToolResult.notFound("Event not found for ID: " + eventId));
    }

    public Mono<AgenticToolResult<List<FacadeEventBO>>> lookupEventsByIdsReactive(List<Long> eventIds, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        List<Long> ids = AgenticToolUtil.normalizeIds(eventIds);
        if (ids.isEmpty()) return Mono.just(AgenticToolResult.invalid("No valid event IDs provided."));
        return eventFacade.listByIds(tenantId, ids).collectList().map(values -> values.isEmpty()
                ? AgenticToolResult.empty("No events found for IDs: " + ids, List.of())
                : AgenticToolResult.ok("Events loaded", values));
    }

    public Mono<AgenticToolResult<OffsetPage<FacadeEventBO>>> searchEventsReactive(String eventName, Long profileId,
                                                                                     long offset, int limit,
                                                                                     ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        return Mono.defer(() -> eventFacade.list(new FacadeEventOffsetQuery(tenantId, eventName, null, null, null, profileId,
                        null, null, null, offset, limit, List.of())))
                .map(page -> page.items().isEmpty() ? AgenticToolResult.empty("No events found.", page)
                        : AgenticToolResult.ok("Event page loaded", page));
    }

    public Mono<AgenticToolResult<OffsetPage<FacadeEventBO>>> listEventsByDeviceIdReactive(Long deviceId, long offset,
                                                                                              int limit,
                                                                                              ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        return Mono.defer(() -> eventFacade.list(new FacadeEventOffsetQuery(tenantId, null, null, null, null, null, null,
                        null, deviceId, offset, limit, List.of())))
                .map(page -> page.items().isEmpty() ? AgenticToolResult.empty("No events found for device ID: " + deviceId, page)
                        : AgenticToolResult.ok("Event page loaded for device " + deviceId, page));
    }

    public Mono<AgenticToolResult<OffsetPage<FacadeEventBO>>> listEventsByProfileIdReactive(Long profileId, long offset,
                                                                                               int limit,
                                                                                               ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        return Mono.defer(() -> eventFacade.list(new FacadeEventOffsetQuery(tenantId, null, null, null, null, profileId, null,
                        null, null, offset, limit, List.of())))
                .map(page -> page.items().isEmpty() ? AgenticToolResult.empty("No events found for profile ID: " + profileId, page)
                        : AgenticToolResult.ok("Event page loaded for profile " + profileId, page));
    }

}

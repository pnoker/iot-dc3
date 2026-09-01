package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.query.FacadePointOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/** Non-blocking point metadata tools. */
@Component
@RequiredArgsConstructor
public class PointTool {
    private final PointFacade pointFacade;

    public Mono<AgenticToolResult<FacadePointBO>> lookupPointByIdReactive(Long pointId, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (pointId == null || pointId <= 0) return Mono.just(AgenticToolResult.invalid("Point ID must be positive."));
        return pointFacade.getByIdReactive(tenantId, pointId).map(value -> AgenticToolResult.ok("Point loaded", value))
                .defaultIfEmpty(AgenticToolResult.notFound("Point not found for ID: " + pointId));
    }

    public Mono<AgenticToolResult<List<FacadePointBO>>> lookupPointsByIdsReactive(List<Long> pointIds, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        List<Long> ids = AgenticToolUtil.normalizeIds(pointIds);
        if (ids.isEmpty()) return Mono.just(AgenticToolResult.invalid("No valid point IDs provided."));
        return pointFacade.listByIdsReactive(tenantId, ids).collectList().map(values -> values.isEmpty()
                ? AgenticToolResult.empty("No points found for IDs: " + ids, List.of())
                : AgenticToolResult.ok("Points loaded", values));
    }

    public Mono<AgenticToolResult<OffsetPage<FacadePointBO>>> searchPointsReactive(String pointName, Long profileId,
                                                                                     long offset, int limit,
                                                                                     ToolContext context) {
        return listReactive(null, profileId, pointName, offset, limit, context, "No points found.", "Point page loaded");
    }

    public Mono<AgenticToolResult<OffsetPage<FacadePointBO>>> listPointsByDeviceIdReactive(Long deviceId, long offset,
                                                                                              int limit,
                                                                                              ToolContext context) {
        return listReactive(deviceId, null, null, offset, limit, context,
                "No points found for device ID: " + deviceId, "Point page loaded for device " + deviceId);
    }

    public Mono<AgenticToolResult<OffsetPage<FacadePointBO>>> listPointsByProfileIdReactive(Long profileId, long offset,
                                                                                               int limit,
                                                                                               ToolContext context) {
        return listReactive(null, profileId, null, offset, limit, context,
                "No points found for profile ID: " + profileId, "Point page loaded for profile " + profileId);
    }

    private Mono<AgenticToolResult<OffsetPage<FacadePointBO>>> listReactive(Long deviceId, Long profileId,
                                                                              String pointName, long offset,
                                                                              int limit, ToolContext context,
                                                                              String emptyMessage,
                                                                              String successMessage) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(context);
            if (offset < 0) return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
            if (limit < 1 || limit > 200) return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
            return pointFacade.listReactive(new FacadePointOffsetQuery(tenantId, pointName, null, null, null,
                            profileId, null, null, null, null, deviceId, offset, limit, List.of()))
                    .map(page -> page.items().isEmpty() ? AgenticToolResult.empty(emptyMessage, page)
                            : AgenticToolResult.ok(successMessage, page));
        });
    }
}

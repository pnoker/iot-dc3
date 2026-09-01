package io.github.pnoker.common.agentic.tools;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/** Non-blocking driver metadata tools. */
@Component
@RequiredArgsConstructor
public class DriverTool {
    private final DriverFacade driverFacade;

    public Mono<AgenticToolResult<FacadeDriverBO>> lookupDriverByIdReactive(Long driverId, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (driverId == null || driverId <= 0) return Mono.just(AgenticToolResult.invalid("Driver ID must be positive."));
        return driverFacade.getByIdReactive(tenantId, driverId).map(value -> AgenticToolResult.ok("Driver loaded", value))
                .defaultIfEmpty(AgenticToolResult.notFound("Driver not found for ID: " + driverId));
    }

    public Mono<AgenticToolResult<List<FacadeDriverBO>>> lookupDriversByIdsReactive(List<Long> driverIds,
                                                                                      ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        List<Long> ids = AgenticToolUtil.normalizeIds(driverIds);
        if (ids.isEmpty()) return Mono.just(AgenticToolResult.invalid("No valid driver IDs provided."));
        return driverFacade.listByIdsReactive(tenantId, ids).collectList().map(values -> values.isEmpty()
                ? AgenticToolResult.empty("No drivers found for IDs: " + ids, List.of())
                : AgenticToolResult.ok("Drivers loaded", values));
    }

    public Mono<AgenticToolResult<OffsetPage<FacadeDriverBO>>> searchDriversReactive(String driverName, long offset,
                                                                                        int limit,
                                                                                        ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (offset < 0) return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
        if (limit < 1 || limit > 200) return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
        return driverFacade.listReactive(new FacadeDriverOffsetQuery(tenantId, driverName, null, null, null, null,
                        null, null, null, null, offset, limit, List.of()))
                .map(page -> page.items().isEmpty() ? AgenticToolResult.empty("No drivers found.", page)
                        : AgenticToolResult.ok("Driver page loaded", page));
    }
}

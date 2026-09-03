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
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Non-blocking driver metadata tools. */
@Component
@RequiredArgsConstructor
public class DriverTool {
    private final DriverFacade driverFacade;

    /** Look up the driver by id. */
    public Mono<AgenticToolResult<FacadeDriverBO>> lookupDriverByIdReactive(Long driverId, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (driverId == null || driverId <= 0)
            return Mono.just(AgenticToolResult.invalid("Driver ID must be positive."));
        return driverFacade
                .getByIdReactive(tenantId, driverId)
                .map(value -> AgenticToolResult.ok("Driver loaded", value))
                .defaultIfEmpty(AgenticToolResult.notFound("Driver not found for ID: " + driverId));
    }

    /** Look up the drivers for the given ids. */
    public Mono<AgenticToolResult<List<FacadeDriverBO>>> lookupDriversByIdsReactive(
            List<Long> driverIds, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        List<Long> ids = AgenticToolUtil.normalizeIds(driverIds);
        if (ids.isEmpty()) return Mono.just(AgenticToolResult.invalid("No valid driver IDs provided."));
        return driverFacade
                .listByIdsReactive(tenantId, ids)
                .collectList()
                .map(values -> values.isEmpty()
                        ? AgenticToolResult.empty("No drivers found for IDs: " + ids, List.of())
                        : AgenticToolResult.ok("Drivers loaded", values));
    }

    /** Search drivers matching the request. */
    public Mono<AgenticToolResult<OffsetPage<FacadeDriverBO>>> searchDriversReactive(
            String driverName, long offset, int limit, ToolContext context) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(context);
        if (offset < 0) return Mono.just(AgenticToolResult.invalid("Offset must be non-negative."));
        if (limit < 1 || limit > 200) return Mono.just(AgenticToolResult.invalid("Limit must be between 1 and 200."));
        return driverFacade
                .listReactive(new FacadeDriverOffsetQuery(
                        tenantId, driverName, null, null, null, null, null, null, null, null, offset, limit, List.of()))
                .map(page -> page.items().isEmpty()
                        ? AgenticToolResult.empty("No drivers found.", page)
                        : AgenticToolResult.ok("Driver page loaded", page));
    }
}

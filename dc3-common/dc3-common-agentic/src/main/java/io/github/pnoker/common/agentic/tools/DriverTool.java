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
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverDeviceStatusSummaryBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Driver-domain tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverTool {

    private final DriverFacade driverFacade;

    private final Optional<StatusHealthFacade> statusHealthFacade;

    @Tool(description = "Look up a driver by its numeric ID. Returns driver name, code, service name, host, type, and enable status.")
    @AgenticToolMetadata(domain = "driver", title = "Query driver by ID")
    public AgenticToolResult<FacadeDriverBO> lookupDriverById(
            @ToolParam(description = "The numeric driver ID") Long driverId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "lookupDriverById", tenantId, driverId);
        FacadeDriverBO bo = driverFacade.getById(tenantId, driverId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("Driver not found for ID: " + driverId);
        }
        return AgenticToolResult.ok("Driver loaded", bo);
    }

    @Tool(description = "Batch look up drivers by numeric IDs. Returns up to 50 tenant-scoped drivers.")
    @AgenticToolMetadata(domain = "driver", title = "Batch query drivers by IDs")
    public AgenticToolResult<List<FacadeDriverBO>> lookupDriversByIds(
            @ToolParam(description = "The numeric driver IDs") List<Long> driverIds,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        List<Long> ids = AgenticToolUtil.normalizeIds(driverIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverIds={}", "lookupDriversByIds", tenantId, ids);
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid driver IDs provided.");
        }
        List<FacadeDriverBO> drivers = driverFacade.listByIds(tenantId, ids);
        if (Objects.isNull(drivers) || drivers.isEmpty()) {
            return AgenticToolResult.empty("No drivers found for IDs: " + ids, List.of());
        }
        return AgenticToolResult.ok("Drivers loaded", drivers);
    }

    @Tool(description = "Resolve the driver that owns a given device. Returns the driver details.")
    @AgenticToolMetadata(domain = "driver", title = "Query device driver")
    public AgenticToolResult<FacadeDriverBO> lookupDriverByDeviceId(
            @ToolParam(description = "The device ID") Long deviceId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "lookupDriverByDeviceId", tenantId,
                deviceId);
        FacadeDriverBO bo = driverFacade.getByDeviceId(tenantId, deviceId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("No driver found for device ID: " + deviceId);
        }
        return AgenticToolResult.ok("Driver loaded for device " + deviceId, bo);
    }

    @Tool(description = "Search for drivers with optional name filter. Returns a paginated list.")
    @AgenticToolMetadata(domain = "driver", title = "Search drivers")
    public AgenticToolResult<FacadePage<FacadeDriverBO>> searchDrivers(
            @ToolParam(description = "Driver name filter (partial match), or null to skip") String driverName,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverName={}, page={}, size={}", "searchDrivers",
                tenantId, driverName, page, size);

        FacadeDriverQuery query = new FacadeDriverQuery();
        query.setDriverName(driverName);
        query.setTenantId(tenantId);
        query.setPage(AgenticToolUtil.page(page, size));

        FacadePage<FacadeDriverBO> result = driverFacade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No drivers found.", result);
        }
        return AgenticToolResult.ok("Driver page loaded", result);
    }

    @Tool(description = "Get driver online/offline statuses for driver IDs. Returns up to 50 tenant-scoped statuses.")
    @AgenticToolMetadata(domain = "driver", title = "Get driver statuses")
    public AgenticToolResult<Map<Long, String>> getDriverStatusesByIds(
            @ToolParam(description = "The numeric driver IDs") List<Long> driverIds,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        List<Long> ids = AgenticToolUtil.normalizeIds(driverIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverIds={}", "getDriverStatusesByIds", tenantId, ids);
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE);
        }
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid driver IDs provided.");
        }
        Map<Long, String> statuses = facade.selectDriverStatusesByIds(tenantId, ids);
        if (AgenticToolUtil.isEmpty(statuses)) {
            return AgenticToolResult.empty("No driver statuses found.", Map.of());
        }
        return AgenticToolResult.ok("Driver statuses loaded", statuses);
    }

    @Tool(description = "Get the online/offline device count summary under a driver.")
    @AgenticToolMetadata(domain = "driver", title = "Get driver device status summary")
    public AgenticToolResult<FacadeDriverDeviceStatusSummaryBO> getDriverDeviceStatusSummary(
            @ToolParam(description = "The driver ID") Long driverId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "getDriverDeviceStatusSummary", tenantId,
                driverId);
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE);
        }
        FacadeDriverDeviceStatusSummaryBO summary = facade.getDriverDeviceStatusSummary(tenantId, driverId);
        if (Objects.isNull(summary)) {
            return AgenticToolResult.empty("No driver device status summary found for driver ID: " + driverId,
                    null);
        }
        return AgenticToolResult.ok("Driver device status summary loaded", summary);
    }

}

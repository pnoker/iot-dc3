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

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Point-domain tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class PointTool {

    private final PointFacade pointFacade;

    public PointTool(PointFacade pointFacade) {
        this.pointFacade = pointFacade;
    }

    @Tool(description = "Look up a point (data point / metric) by its numeric ID. Returns point name, code, type, read/write flag, unit, base value, and multiplier.")
    public AgenticToolResult<FacadePointBO> lookupPointById(
            @ToolParam(description = "The numeric point ID") Long pointId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, pointId={}", "lookupPointById", tenantId, pointId);
        recordTool(toolContext, "lookupPointById", "Query point by ID");
        FacadePointBO bo = pointFacade.selectById(tenantId, pointId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("Point not found for ID: " + pointId);
        }
        return AgenticToolResult.ok("Point loaded", bo);
    }

    @Tool(description = "Batch look up points by numeric IDs. Returns up to 50 tenant-scoped points.")
    public AgenticToolResult<List<FacadePointBO>> lookupPointsByIds(
            @ToolParam(description = "The numeric point IDs") List<Long> pointIds,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(pointIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, pointIds={}", "lookupPointsByIds", tenantId, ids);
        recordTool(toolContext, "lookupPointsByIds", "Batch query points by IDs");
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid point IDs provided.");
        }
        List<FacadePointBO> points = pointFacade.selectByIds(tenantId, ids);
        if (Objects.isNull(points) || points.isEmpty()) {
            return AgenticToolResult.empty("No points found for IDs: " + ids, List.of());
        }
        return AgenticToolResult.ok("Points loaded", points);
    }

    @Tool(description = "Search for points with optional filters. Returns a paginated list.")
    public AgenticToolResult<FacadePage<FacadePointBO>> searchPoints(
            @ToolParam(description = "Point name filter (partial match), or null to skip") String pointName,
            @ToolParam(description = "Profile ID filter, or null to skip") Long profileId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, pointName={}, profileId={}, page={}, size={}",
                "searchPoints", tenantId, pointName, profileId, page, size);
        recordTool(toolContext, "searchPoints", "Search points");

        FacadePointQuery query = new FacadePointQuery();
        query.setPointName(pointName);
        query.setProfileId(profileId);
        query.setTenantId(tenantId);
        Pages p = new Pages();
        p.setCurrent(page);
        p.setSize(size);
        query.setPage(p);

        FacadePage<FacadePointBO> result = pointFacade.selectByPage(query);
        if (Objects.isNull(result) || Objects.isNull(result.getRecords()) || result.getRecords().isEmpty()) {
            return AgenticToolResult.empty("No points found.", result);
        }
        return AgenticToolResult.ok("Point page loaded", result);
    }

    @Tool(description = "List points bound to a specific device ID. Use this before reading or writing values when the user knows the device but not the point ID.")
    public AgenticToolResult<FacadePage<FacadePointBO>> listPointsByDeviceId(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, page={}, size={}", "listPointsByDeviceId",
                tenantId, deviceId, page, size);
        recordTool(toolContext, "listPointsByDeviceId", "List points by device");

        FacadePointQuery query = new FacadePointQuery();
        query.setDeviceId(deviceId);
        query.setTenantId(tenantId);
        Pages p = new Pages();
        p.setCurrent(page);
        p.setSize(size);
        query.setPage(p);

        FacadePage<FacadePointBO> result = pointFacade.selectByPage(query);
        if (Objects.isNull(result) || Objects.isNull(result.getRecords()) || result.getRecords().isEmpty()) {
            return AgenticToolResult.empty("No points found for device ID: " + deviceId, result);
        }
        return AgenticToolResult.ok("Point page loaded for device " + deviceId, result);
    }

    @Tool(description = "List points under a specific profile/template ID. Use this when the user wants all metrics defined by a template.")
    public AgenticToolResult<FacadePage<FacadePointBO>> listPointsByProfileId(
            @ToolParam(description = "The profile/template ID") Long profileId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}, page={}, size={}",
                "listPointsByProfileId", tenantId, profileId, page, size);
        recordTool(toolContext, "listPointsByProfileId", "List points by profile");

        FacadePointQuery query = new FacadePointQuery();
        query.setProfileId(profileId);
        query.setTenantId(tenantId);
        Pages p = new Pages();
        p.setCurrent(page);
        p.setSize(size);
        query.setPage(p);

        FacadePage<FacadePointBO> result = pointFacade.selectByPage(query);
        if (Objects.isNull(result) || Objects.isNull(result.getRecords()) || result.getRecords().isEmpty()) {
            return AgenticToolResult.empty("No points found for profile ID: " + profileId, result);
        }
        return AgenticToolResult.ok("Point page loaded for profile " + profileId, result);
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "point", description);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().limit(50).toList();
    }

}

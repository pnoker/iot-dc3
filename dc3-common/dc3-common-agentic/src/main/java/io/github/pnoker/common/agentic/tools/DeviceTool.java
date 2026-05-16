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
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.api.StatusHealthFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Device-domain tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DeviceTool {

    private static final String STATUS_UNAVAILABLE = "Status and health tools are not available in this deployment mode.";

    private final DeviceFacade deviceFacade;

    private final PointFacade pointFacade;

    private final PointValueFacade pointValueFacade;

    private final Optional<StatusHealthFacade> statusHealthFacade;

    public DeviceTool(DeviceFacade deviceFacade, PointFacade pointFacade, PointValueFacade pointValueFacade,
                      Optional<StatusHealthFacade> statusHealthFacade) {
        this.deviceFacade = deviceFacade;
        this.pointFacade = pointFacade;
        this.pointValueFacade = pointValueFacade;
        this.statusHealthFacade = statusHealthFacade;
    }

    @Tool(description = "Look up a device by its numeric ID. Returns device name, code, driver ID, enable status, and profile IDs.")
    public AgenticToolResult<FacadeDeviceBO> lookupDeviceById(
            @ToolParam(description = "The numeric device ID") Long deviceId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "lookupDeviceById", tenantId, deviceId);
        recordTool(toolContext, "lookupDeviceById", "Query device by ID");
        FacadeDeviceBO bo = deviceFacade.selectById(tenantId, deviceId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("Device not found for ID: " + deviceId);
        }
        return AgenticToolResult.ok("Device loaded", bo);
    }

    @Tool(description = "Batch look up devices by numeric IDs. Returns up to 50 tenant-scoped devices.")
    public AgenticToolResult<List<FacadeDeviceBO>> lookupDevicesByIds(
            @ToolParam(description = "The numeric device IDs") List<Long> deviceIds,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(deviceIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceIds={}", "lookupDevicesByIds", tenantId, ids);
        recordTool(toolContext, "lookupDevicesByIds", "Batch query devices by IDs");
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid device IDs provided.");
        }
        List<FacadeDeviceBO> devices = deviceFacade.selectByIds(tenantId, ids);
        if (Objects.isNull(devices) || devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for IDs: " + ids, List.of());
        }
        return AgenticToolResult.ok("Devices loaded", devices);
    }

    @Tool(description = "Search for devices with optional filters. Supports filtering by device name, code, or driver ID. Returns a paginated list of devices.")
    public AgenticToolResult<FacadePage<FacadeDeviceBO>> searchDevices(
            @ToolParam(description = "Device name filter (partial match), or null to skip") String deviceName,
            @ToolParam(description = "Device code filter, or null to skip") String deviceCode,
            @ToolParam(description = "Driver ID filter, or null to skip") Long driverId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug(
                "Agentic tool invoked, tool={}, tenantId={}, deviceName={}, deviceCode={}, driverId={}, page={}, size={}",
                "searchDevices", tenantId, deviceName, deviceCode, driverId, page, size);
        recordTool(toolContext, "searchDevices", "Search devices");

        FacadeDeviceQuery query = new FacadeDeviceQuery();
        query.setDeviceName(deviceName);
        query.setDeviceCode(deviceCode);
        query.setDriverId(driverId);
        query.setTenantId(tenantId);
        Pages p = new Pages();
        p.setCurrent(page);
        p.setSize(size);
        query.setPage(p);

        FacadePage<FacadeDeviceBO> result = deviceFacade.selectByPage(query);
        if (Objects.isNull(result) || Objects.isNull(result.getRecords()) || result.getRecords().isEmpty()) {
            return AgenticToolResult.empty("No devices found.", result);
        }
        return AgenticToolResult.ok("Device page loaded", result);
    }

    @Tool(description = "List all devices attached to a given driver ID.")
    public AgenticToolResult<List<FacadeDeviceBO>> listDevicesByDriverId(
            @ToolParam(description = "The driver ID") Long driverId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "listDevicesByDriverId", tenantId,
                driverId);
        recordTool(toolContext, "listDevicesByDriverId", "List devices by driver");
        List<FacadeDeviceBO> devices = deviceFacade.selectByDriverId(tenantId, driverId);
        if (Objects.isNull(devices) || devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for driver ID: " + driverId, List.of());
        }
        return AgenticToolResult.ok("Devices loaded for driver " + driverId, devices);
    }

    @Tool(description = "List all devices that use a given profile (device template) ID.")
    public AgenticToolResult<List<FacadeDeviceBO>> listDevicesByProfileId(
            @ToolParam(description = "The profile ID") Long profileId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "listDevicesByProfileId", tenantId,
                profileId);
        recordTool(toolContext, "listDevicesByProfileId", "List devices by profile");
        List<FacadeDeviceBO> devices = deviceFacade.selectByProfileId(tenantId, profileId);
        if (Objects.isNull(devices) || devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for profile ID: " + profileId, List.of());
        }
        return AgenticToolResult.ok("Devices loaded for profile " + profileId, devices);
    }

    @Tool(description = "Get a latest-value snapshot for points bound to a device. Returns point metadata and latest values for up to the requested limit.")
    public AgenticToolResult<DeviceLatestPointValues> getDeviceLatestPointValues(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "Maximum number of points to include") int limit,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        int size = Math.max(1, Math.min(limit, 50));
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, limit={}",
                "getDeviceLatestPointValues", tenantId, deviceId, size);
        recordTool(toolContext, "getDeviceLatestPointValues", "Get device latest point values");
        try {
            FacadeDeviceBO device = deviceFacade.selectById(tenantId, deviceId);
            if (Objects.isNull(device)) {
                return AgenticToolResult.notFound("Device not found for ID: " + deviceId);
            }

            FacadePointQuery query = new FacadePointQuery();
            query.setTenantId(tenantId);
            query.setDeviceId(deviceId);
            Pages page = new Pages();
            page.setCurrent(1);
            page.setSize(size);
            query.setPage(page);
            FacadePage<FacadePointBO> points = pointFacade.selectByPage(query);
            if (Objects.isNull(points) || Objects.isNull(points.getRecords()) || points.getRecords().isEmpty()) {
                return AgenticToolResult.empty("No points found for device " + deviceId,
                        new DeviceLatestPointValues(device, List.of()));
            }

            List<PointLatestValue> values = new ArrayList<>();
            for (FacadePointBO point : points.getRecords()) {
                FacadePointValueBO value = pointValueFacade.lastValue(tenantId, deviceId, point.getId());
                values.add(new PointLatestValue(point, value));
            }
            return AgenticToolResult.ok("Device latest point values loaded",
                    new DeviceLatestPointValues(device, values));
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, limit={}",
                    "getDeviceLatestPointValues", tenantId, deviceId, size, e);
            return AgenticToolResult.error("Error retrieving device latest values: " + e.getMessage());
        }
    }

    @Tool(description = "Get device online/offline statuses for device IDs. Returns up to 50 tenant-scoped statuses.")
    public AgenticToolResult<Map<Long, String>> getDeviceStatusesByIds(
            @ToolParam(description = "The numeric device IDs") List<Long> deviceIds,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(deviceIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceIds={}", "getDeviceStatusesByIds", tenantId, ids);
        recordTool(toolContext, "getDeviceStatusesByIds", "Get device statuses");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(STATUS_UNAVAILABLE);
        }
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid device IDs provided.");
        }
        Map<Long, String> statuses = facade.selectDeviceStatusesByIds(tenantId, ids);
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return AgenticToolResult.empty("No device statuses found.", Map.of());
        }
        return AgenticToolResult.ok("Device statuses loaded", statuses);
    }

    @Tool(description = "Get device online/offline statuses for devices bound to a profile/template.")
    public AgenticToolResult<Map<Long, String>> getDeviceStatusesByProfileId(
            @ToolParam(description = "The profile/template ID") Long profileId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "getDeviceStatusesByProfileId",
                tenantId, profileId);
        recordTool(toolContext, "getDeviceStatusesByProfileId", "Get device statuses by profile");
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(STATUS_UNAVAILABLE);
        }
        Map<Long, String> statuses = facade.selectDeviceStatusesByProfileId(tenantId, profileId);
        if (Objects.isNull(statuses) || statuses.isEmpty()) {
            return AgenticToolResult.empty("No device statuses found for profile ID: " + profileId, Map.of());
        }
        return AgenticToolResult.ok("Device statuses loaded for profile " + profileId, statuses);
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "device", description);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().limit(50).toList();
    }

    public record DeviceLatestPointValues(FacadeDeviceBO device, List<PointLatestValue> points) {

        public DeviceLatestPointValues {
            points = List.copyOf(Objects.requireNonNullElse(points, List.of()));
        }

    }

    public record PointLatestValue(FacadePointBO point, FacadePointValueBO value) {
    }

}

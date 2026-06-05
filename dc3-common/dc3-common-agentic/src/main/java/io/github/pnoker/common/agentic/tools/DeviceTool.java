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
import lombok.RequiredArgsConstructor;
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
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTool {

    private final DeviceFacade deviceFacade;

    private final PointFacade pointFacade;

    private final PointValueFacade pointValueFacade;

    private final Optional<StatusHealthFacade> statusHealthFacade;

    @Tool(description = "Look up a device by its numeric ID. Returns device name, code, driver ID, enable status, and profile IDs.")
    @AgenticToolMetadata(domain = "device", title = "Query device by ID")
    public AgenticToolResult<FacadeDeviceBO> lookupDeviceById(
            @ToolParam(description = "The numeric device ID") Long deviceId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "lookupDeviceById", tenantId, deviceId);
        FacadeDeviceBO bo = deviceFacade.getById(tenantId, deviceId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("Device not found for ID: " + deviceId);
        }
        return AgenticToolResult.ok("Device loaded", bo);
    }

    @Tool(description = "Batch look up devices by numeric IDs. Returns up to 50 tenant-scoped devices.")
    @AgenticToolMetadata(domain = "device", title = "Batch query devices by IDs")
    public AgenticToolResult<List<FacadeDeviceBO>> lookupDevicesByIds(
            @ToolParam(description = "The numeric device IDs") List<Long> deviceIds,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        List<Long> ids = AgenticToolUtil.normalizeIds(deviceIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceIds={}", "lookupDevicesByIds", tenantId, ids);
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid device IDs provided.");
        }
        List<FacadeDeviceBO> devices = deviceFacade.listByIds(tenantId, ids);
        if (Objects.isNull(devices) || devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for IDs: " + ids, List.of());
        }
        return AgenticToolResult.ok("Devices loaded", devices);
    }

    @Tool(description = "Search for devices with optional filters. Supports filtering by device name, code, or driver ID. Returns a paginated list of devices.")
    @AgenticToolMetadata(domain = "device", title = "Search devices")
    public AgenticToolResult<FacadePage<FacadeDeviceBO>> searchDevices(
            @ToolParam(description = "Device name filter (partial match), or null to skip") String deviceName,
            @ToolParam(description = "Device code filter, or null to skip") String deviceCode,
            @ToolParam(description = "Driver ID filter, or null to skip") Long driverId,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug(
                "Agentic tool invoked, tool={}, tenantId={}, deviceName={}, deviceCode={}, driverId={}, page={}, size={}",
                "searchDevices", tenantId, deviceName, deviceCode, driverId, page, size);

        FacadeDeviceQuery query = new FacadeDeviceQuery();
        query.setDeviceName(deviceName);
        query.setDeviceCode(deviceCode);
        query.setDriverId(driverId);
        query.setTenantId(tenantId);
        query.setPage(AgenticToolUtil.page(page, size));

        FacadePage<FacadeDeviceBO> result = deviceFacade.listByPage(query);
        if (!AgenticToolUtil.hasRecords(result)) {
            return AgenticToolResult.empty("No devices found.", result);
        }
        return AgenticToolResult.ok("Device page loaded", result);
    }

    @Tool(description = "List all devices attached to a given driver ID.")
    @AgenticToolMetadata(domain = "device", title = "List devices by driver")
    public AgenticToolResult<List<FacadeDeviceBO>> listDevicesByDriverId(
            @ToolParam(description = "The driver ID") Long driverId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "listDevicesByDriverId", tenantId,
                driverId);
        List<FacadeDeviceBO> devices = deviceFacade.listByDriverId(tenantId, driverId);
        if (Objects.isNull(devices) || devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for driver ID: " + driverId, List.of());
        }
        return AgenticToolResult.ok("Devices loaded for driver " + driverId, devices);
    }

    @Tool(description = "List all devices that use a given profile (device template) ID.")
    @AgenticToolMetadata(domain = "device", title = "List devices by profile")
    public AgenticToolResult<List<FacadeDeviceBO>> listDevicesByProfileId(
            @ToolParam(description = "The profile ID") Long profileId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "listDevicesByProfileId", tenantId,
                profileId);
        List<FacadeDeviceBO> devices = deviceFacade.listByProfileId(tenantId, profileId);
        if (Objects.isNull(devices) || devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for profile ID: " + profileId, List.of());
        }
        return AgenticToolResult.ok("Devices loaded for profile " + profileId, devices);
    }

    @Tool(description = "Get a latest-value snapshot for points bound to a device. Returns point metadata and latest values for up to the requested limit.")
    @AgenticToolMetadata(domain = "device", title = "Get device latest point values")
    public AgenticToolResult<DeviceLatestPointValues> getDeviceLatestPointValues(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "Maximum number of points to include") int limit,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        int size = AgenticToolUtil.clamp(limit, 1, AgenticConstant.ToolLimit.MAX_IDS);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, limit={}",
                "getDeviceLatestPointValues", tenantId, deviceId, size);
        try {
            FacadeDeviceBO device = deviceFacade.getById(tenantId, deviceId);
            if (Objects.isNull(device)) {
                return AgenticToolResult.notFound("Device not found for ID: " + deviceId);
            }

            FacadePointQuery query = new FacadePointQuery();
            query.setTenantId(tenantId);
            query.setDeviceId(deviceId);
            query.setPage(AgenticToolUtil.page(1, size));
            FacadePage<FacadePointBO> points = pointFacade.listByPage(query);
            if (!AgenticToolUtil.hasRecords(points)) {
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
    @AgenticToolMetadata(domain = "device", title = "Get device statuses")
    public AgenticToolResult<Map<Long, String>> getDeviceStatusesByIds(
            @ToolParam(description = "The numeric device IDs") List<Long> deviceIds,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        List<Long> ids = AgenticToolUtil.normalizeIds(deviceIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceIds={}", "getDeviceStatusesByIds", tenantId, ids);
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE);
        }
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid device IDs provided.");
        }
        Map<Long, String> statuses = facade.listDeviceStatusesByIds(tenantId, ids);
        if (AgenticToolUtil.isEmpty(statuses)) {
            return AgenticToolResult.empty("No device statuses found.", Map.of());
        }
        return AgenticToolResult.ok("Device statuses loaded", statuses);
    }

    @Tool(description = "Get device online/offline statuses for devices bound to a profile/template.")
    @AgenticToolMetadata(domain = "device", title = "Get device statuses by profile")
    public AgenticToolResult<Map<Long, String>> getDeviceStatusesByProfileId(
            @ToolParam(description = "The profile/template ID") Long profileId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "getDeviceStatusesByProfileId",
                tenantId, profileId);
        StatusHealthFacade facade = statusHealthFacade.orElse(null);
        if (Objects.isNull(facade)) {
            return AgenticToolResult.unavailable(AgenticConstant.ToolMessage.STATUS_HEALTH_UNAVAILABLE);
        }
        Map<Long, String> statuses = facade.listDeviceStatusesByProfileId(tenantId, profileId);
        if (AgenticToolUtil.isEmpty(statuses)) {
            return AgenticToolResult.empty("No device statuses found for profile ID: " + profileId, Map.of());
        }
        return AgenticToolResult.ok("Device statuses loaded for profile " + profileId, statuses);
    }

    public record DeviceLatestPointValues(FacadeDeviceBO device, List<PointLatestValue> points) {

        public DeviceLatestPointValues {
            points = List.copyOf(Objects.requireNonNullElse(points, List.of()));
        }

    }

    public record PointLatestValue(FacadePointBO point, FacadePointValueBO value) {
    }

}

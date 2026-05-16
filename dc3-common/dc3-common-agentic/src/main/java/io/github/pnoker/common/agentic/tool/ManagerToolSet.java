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
package io.github.pnoker.common.agentic.tool;

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Manager-domain tools exposed to the LLM via Spring AI @Tool.
 * <p>
 * These tools allow the model to query devices, drivers, and points (metrics) through the
 * existing facade interfaces.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class ManagerToolSet {

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final PointFacade pointFacade;

    public ManagerToolSet(DeviceFacade deviceFacade, DriverFacade driverFacade, PointFacade pointFacade) {
        this.deviceFacade = deviceFacade;
        this.driverFacade = driverFacade;
        this.pointFacade = pointFacade;
    }

    // ==================== Device Tools ====================

    @Tool(description = "Look up a device by its numeric ID. Returns device name, code, driver ID, enable status, and profile IDs.")
    public AgenticToolResult<FacadeDeviceBO> lookupDeviceById(@ToolParam(description = "The numeric device ID") Long deviceId,
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
        if (devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for IDs: " + ids, devices);
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
        if (Objects.isNull(result) || result.getRecords().isEmpty()) {
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
        if (devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for driver ID: " + driverId, devices);
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
        if (devices.isEmpty()) {
            return AgenticToolResult.empty("No devices found for profile ID: " + profileId, devices);
        }
        return AgenticToolResult.ok("Devices loaded for profile " + profileId, devices);
    }

    // ==================== Driver Tools ====================

    @Tool(description = "Look up a driver by its numeric ID. Returns driver name, code, service name, host, type, and enable status.")
    public AgenticToolResult<FacadeDriverBO> lookupDriverById(
            @ToolParam(description = "The numeric driver ID") Long driverId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "lookupDriverById", tenantId, driverId);
        recordTool(toolContext, "lookupDriverById", "Query driver by ID");
        FacadeDriverBO bo = driverFacade.selectById(tenantId, driverId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("Driver not found for ID: " + driverId);
        }
        return AgenticToolResult.ok("Driver loaded", bo);
    }

    @Tool(description = "Batch look up drivers by numeric IDs. Returns up to 50 tenant-scoped drivers.")
    public AgenticToolResult<List<FacadeDriverBO>> lookupDriversByIds(
            @ToolParam(description = "The numeric driver IDs") List<Long> driverIds,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        List<Long> ids = normalizeIds(driverIds);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverIds={}", "lookupDriversByIds", tenantId, ids);
        recordTool(toolContext, "lookupDriversByIds", "Batch query drivers by IDs");
        if (ids.isEmpty()) {
            return AgenticToolResult.invalid("No valid driver IDs provided.");
        }
        List<FacadeDriverBO> drivers = driverFacade.selectByIds(tenantId, ids);
        if (drivers.isEmpty()) {
            return AgenticToolResult.empty("No drivers found for IDs: " + ids, drivers);
        }
        return AgenticToolResult.ok("Drivers loaded", drivers);
    }

    @Tool(description = "Resolve the driver that owns a given device. Returns the driver details.")
    public AgenticToolResult<FacadeDriverBO> lookupDriverByDeviceId(
            @ToolParam(description = "The device ID") Long deviceId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "lookupDriverByDeviceId", tenantId,
                deviceId);
        recordTool(toolContext, "lookupDriverByDeviceId", "Query device driver");
        FacadeDriverBO bo = driverFacade.selectByDeviceId(tenantId, deviceId);
        if (Objects.isNull(bo)) {
            return AgenticToolResult.notFound("No driver found for device ID: " + deviceId);
        }
        return AgenticToolResult.ok("Driver loaded for device " + deviceId, bo);
    }

    @Tool(description = "Search for drivers with optional name filter. Returns a paginated list.")
    public AgenticToolResult<FacadePage<FacadeDriverBO>> searchDrivers(
            @ToolParam(description = "Driver name filter (partial match), or null to skip") String driverName,
            @ToolParam(description = "Page number (1-based)") int page,
            @ToolParam(description = "Page size") int size,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverName={}, page={}, size={}", "searchDrivers",
                tenantId, driverName, page, size);
        recordTool(toolContext, "searchDrivers", "Search drivers");

        FacadeDriverQuery query = new FacadeDriverQuery();
        query.setDriverName(driverName);
        query.setTenantId(tenantId);
        Pages p = new Pages();
        p.setCurrent(page);
        p.setSize(size);
        query.setPage(p);

        FacadePage<FacadeDriverBO> result = driverFacade.selectByPage(query);
        if (Objects.isNull(result) || result.getRecords().isEmpty()) {
            return AgenticToolResult.empty("No drivers found.", result);
        }
        return AgenticToolResult.ok("Driver page loaded", result);
    }

    // ==================== Point Tools ====================

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
        if (points.isEmpty()) {
            return AgenticToolResult.empty("No points found for IDs: " + ids, points);
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
        if (Objects.isNull(result) || result.getRecords().isEmpty()) {
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
        if (Objects.isNull(result) || result.getRecords().isEmpty()) {
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
        if (Objects.isNull(result) || result.getRecords().isEmpty()) {
            return AgenticToolResult.empty("No points found for profile ID: " + profileId, result);
        }
        return AgenticToolResult.ok("Point page loaded for profile " + profileId, result);
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "manager", description);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().limit(50).toList();
    }

}

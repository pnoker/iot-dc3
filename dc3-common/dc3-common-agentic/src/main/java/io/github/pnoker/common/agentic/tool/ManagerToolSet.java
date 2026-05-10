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
import java.util.stream.Collectors;

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
    public String lookupDeviceById(@ToolParam(description = "The numeric device ID") Long deviceId,
                                   ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "lookupDeviceById", tenantId, deviceId);
        recordTool(toolContext, "lookupDeviceById", "Query device by ID");
        FacadeDeviceBO bo = deviceFacade.selectById(tenantId, deviceId);
        if (Objects.isNull(bo)) {
            return "Device not found for ID: " + deviceId;
        }
        return formatDevice(bo);
    }

    @Tool(description = "Search for devices with optional filters. Supports filtering by device name, code, or driver ID. Returns a paginated list of devices.")
    public String searchDevices(
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
        return formatDevicePage(result);
    }

    @Tool(description = "List all devices attached to a given driver ID.")
    public String listDevicesByDriverId(@ToolParam(description = "The driver ID") Long driverId,
                                        ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "listDevicesByDriverId", tenantId,
                driverId);
        recordTool(toolContext, "listDevicesByDriverId", "List devices by driver");
        List<FacadeDeviceBO> devices = deviceFacade.selectByDriverId(tenantId, driverId);
        if (devices.isEmpty()) {
            return "No devices found for driver ID: " + driverId;
        }
        return "Devices for driver " + driverId + ":\n"
                + devices.stream().map(this::formatDevice).collect(Collectors.joining("\n"));
    }

    @Tool(description = "List all devices that use a given profile (device template) ID.")
    public String listDevicesByProfileId(@ToolParam(description = "The profile ID") Long profileId,
                                         ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, profileId={}", "listDevicesByProfileId", tenantId,
                profileId);
        recordTool(toolContext, "listDevicesByProfileId", "List devices by profile");
        List<FacadeDeviceBO> devices = deviceFacade.selectByProfileId(tenantId, profileId);
        if (devices.isEmpty()) {
            return "No devices found for profile ID: " + profileId;
        }
        return "Devices for profile " + profileId + ":\n"
                + devices.stream().map(this::formatDevice).collect(Collectors.joining("\n"));
    }

    // ==================== Driver Tools ====================

    @Tool(description = "Look up a driver by its numeric ID. Returns driver name, code, service name, host, type, and enable status.")
    public String lookupDriverById(@ToolParam(description = "The numeric driver ID") Long driverId,
                                   ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, driverId={}", "lookupDriverById", tenantId, driverId);
        recordTool(toolContext, "lookupDriverById", "Query driver by ID");
        FacadeDriverBO bo = driverFacade.selectById(tenantId, driverId);
        if (Objects.isNull(bo)) {
            return "Driver not found for ID: " + driverId;
        }
        return formatDriver(bo);
    }

    @Tool(description = "Resolve the driver that owns a given device. Returns the driver details.")
    public String lookupDriverByDeviceId(@ToolParam(description = "The device ID") Long deviceId,
                                         ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}", "lookupDriverByDeviceId", tenantId,
                deviceId);
        recordTool(toolContext, "lookupDriverByDeviceId", "Query device driver");
        FacadeDriverBO bo = driverFacade.selectByDeviceId(tenantId, deviceId);
        if (Objects.isNull(bo)) {
            return "No driver found for device ID: " + deviceId;
        }
        return formatDriver(bo);
    }

    @Tool(description = "Search for drivers with optional name filter. Returns a paginated list.")
    public String searchDrivers(
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
        return formatDriverPage(result);
    }

    // ==================== Point Tools ====================

    @Tool(description = "Look up a point (data point / metric) by its numeric ID. Returns point name, code, type, read/write flag, unit, base value, and multiplier.")
    public String lookupPointById(@ToolParam(description = "The numeric point ID") Long pointId,
                                  ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, pointId={}", "lookupPointById", tenantId, pointId);
        recordTool(toolContext, "lookupPointById", "Query point by ID");
        FacadePointBO bo = pointFacade.selectById(tenantId, pointId);
        if (Objects.isNull(bo)) {
            return "Point not found for ID: " + pointId;
        }
        return formatPoint(bo);
    }

    @Tool(description = "Search for points with optional filters. Returns a paginated list.")
    public String searchPoints(
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
        return formatPointPage(result);
    }

    // ==================== Formatting Helpers ====================

    private String formatDevice(FacadeDeviceBO d) {
        return String.format("Device[id=%d, name=%s, code=%s, driverId=%d, enabled=%s, profileIds=%s]", d.getId(),
                d.getDeviceName(), d.getDeviceCode(), d.getDriverId(), d.getEnableFlag(), d.getProfileIds());
    }

    private String formatDevicePage(FacadePage<FacadeDeviceBO> page) {
        if (Objects.isNull(page) || page.getRecords().isEmpty()) {
            return "No devices found.";
        }
        String items = page.getRecords().stream().map(this::formatDevice).collect(Collectors.joining("\n"));
        return String.format("Page %d/%d (total %d):\n%s", page.getCurrent(), page.getPages(), page.getTotal(), items);
    }

    private String formatDriver(FacadeDriverBO d) {
        return String.format("Driver[id=%d, name=%s, code=%s, serviceName=%s, host=%s, type=%s, enabled=%s]", d.getId(),
                d.getDriverName(), d.getDriverCode(), d.getServiceName(), d.getServiceHost(), d.getDriverTypeFlag(),
                d.getEnableFlag());
    }

    private String formatDriverPage(FacadePage<FacadeDriverBO> page) {
        if (Objects.isNull(page) || page.getRecords().isEmpty()) {
            return "No drivers found.";
        }
        String items = page.getRecords().stream().map(this::formatDriver).collect(Collectors.joining("\n"));
        return String.format("Page %d/%d (total %d):\n%s", page.getCurrent(), page.getPages(), page.getTotal(), items);
    }

    private String formatPoint(FacadePointBO p) {
        return String.format(
                "Point[id=%d, name=%s, code=%s, type=%s, rw=%s, unit=%s, base=%s, multiple=%s, profileId=%d]",
                p.getId(), p.getPointName(), p.getPointCode(), p.getPointTypeFlag(), p.getRwFlag(), p.getUnit(),
                p.getBaseValue(), p.getMultiple(), p.getProfileId());
    }

    private String formatPointPage(FacadePage<FacadePointBO> page) {
        if (Objects.isNull(page) || page.getRecords().isEmpty()) {
            return "No points found.";
        }
        String items = page.getRecords().stream().map(this::formatPoint).collect(Collectors.joining("\n"));
        return String.format("Page %d/%d (total %d):\n%s", page.getCurrent(), page.getPages(), page.getTotal(), items);
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "manager", description);
    }

}

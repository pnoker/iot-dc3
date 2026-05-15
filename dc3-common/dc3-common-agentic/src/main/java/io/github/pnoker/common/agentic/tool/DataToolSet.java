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
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
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
 * Data-domain tools exposed to the LLM via Spring AI @Tool.
 * <p>
 * Delegates to the facade layer ({@link PointValueFacade} and
 * {@link PointValueCommandFacade}) so that calls follow the project's local/gRPC
 * dual-mode convention.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DataToolSet {

    private final PointValueFacade pointValueFacade;

    private final PointValueCommandFacade pointValueCommandFacade;

    private final DeviceFacade deviceFacade;

    private final PointFacade pointFacade;

    private final ActionService actionService;

    public DataToolSet(PointValueFacade pointValueFacade, PointValueCommandFacade pointValueCommandFacade,
                       DeviceFacade deviceFacade, PointFacade pointFacade, ActionService actionService) {
        this.pointValueFacade = pointValueFacade;
        this.pointValueCommandFacade = pointValueCommandFacade;
        this.deviceFacade = deviceFacade;
        this.pointFacade = pointFacade;
        this.actionService = actionService;
    }

    @Tool(description = "Get the latest point value for a specific device and point. Returns the current value.")
    public String getLatestPointValue(@ToolParam(description = "The device ID") Long deviceId,
                                      @ToolParam(description = "The point (metric) ID") Long pointId,
                                      ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "getLatestPointValue",
                tenantId, deviceId, pointId);
        recordTool(toolContext, "getLatestPointValue", "Get latest point value");
        try {
            FacadePointValueBO value = pointValueFacade.lastValue(tenantId, deviceId, pointId);
            if (Objects.isNull(value)) {
                return "No latest value found for device " + deviceId + " point " + pointId;
            }
            return String.format("Device %d / Point %d: value=%s, rawValue=%s, time=%d", value.getDeviceId(),
                    value.getPointId(), value.getValue(), value.getRawValue(), value.getCreateTime());
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "getLatestPointValue",
                    tenantId, deviceId, pointId, e);
            return "Error retrieving latest value: " + e.getMessage();
        }
    }

    @Tool(description = "Get historical point values for a specific device and point. Returns a list of value strings together with a chart-renderable JSON block.")
    public String getPointValueHistory(@ToolParam(description = "The device ID") Long deviceId,
                                       @ToolParam(description = "The point (metric) ID") Long pointId,
                                       @ToolParam(description = "Number of historical records to retrieve") int count,
                                       ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                "getPointValueHistory", tenantId, deviceId, pointId, count);
        recordTool(toolContext, "getPointValueHistory", "Get point value history");
        try {
            List<String> history = pointValueFacade.history(tenantId, deviceId, pointId, count);
            if (Objects.isNull(history) || history.isEmpty()) {
                return "No history data found for device " + deviceId + " point " + pointId;
            }
            String summary = "History values (" + history.size() + " records): " + String.join(", ", history);
            String chart = buildHistoryChartFence(deviceId, pointId, history);
            return chart.isEmpty() ? summary : summary + "\n\n" + chart;
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                    "getPointValueHistory", tenantId, deviceId, pointId, count, e);
            return "Error retrieving history: " + e.getMessage();
        }
    }

    /**
     * Render a {@code ```chart:line``` } fence so the assistant frontend can plot the
     * history without an extra tool round-trip. The facade currently returns values
     * only (newest → oldest), so we use the array index as the x axis. Non-numeric
     * entries are dropped; if nothing remains we return an empty string and the
     * caller skips the fence.
     */
    private String buildHistoryChartFence(Long deviceId, Long pointId, List<String> history) {
        StringBuilder dataPoints = new StringBuilder();
        int rendered = 0;
        // Reverse so x=0 is the oldest sample — easier to read left-to-right.
        for (int i = history.size() - 1; i >= 0; i--) {
            String raw = history.get(i);
            if (Objects.isNull(raw)) {
                continue;
            }
            try {
                double value = Double.parseDouble(raw.trim());
                if (rendered > 0) {
                    dataPoints.append(',');
                }
                dataPoints.append('[').append(rendered).append(',').append(value).append(']');
                rendered++;
            } catch (NumberFormatException ignored) {
                // skip non-numeric entries
            }
        }
        if (rendered == 0) {
            return "";
        }
        return "```chart:line\n"
                + "{\"title\":\"Device " + deviceId + " / Point " + pointId + "\","
                + "\"xLabel\":\"index (oldest → newest)\","
                + "\"xType\":\"linear\","
                + "\"series\":[{\"name\":\"value\",\"data\":[" + dataPoints + "]}]}\n"
                + "```";
    }

    @Tool(description = "Get a latest-value snapshot for points bound to a device. Returns point metadata and latest values for up to the requested limit.")
    public String getDeviceLatestPointValues(@ToolParam(description = "The device ID") Long deviceId,
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
                return "Device not found for ID: " + deviceId;
            }

            FacadePointQuery query = new FacadePointQuery();
            query.setTenantId(tenantId);
            query.setDeviceId(deviceId);
            Pages page = new Pages();
            page.setCurrent(1);
            page.setSize(size);
            query.setPage(page);
            FacadePage<FacadePointBO> points = pointFacade.selectByPage(query);
            if (Objects.isNull(points) || points.getRecords().isEmpty()) {
                return "No points found for device " + deviceId;
            }

            StringBuilder builder = new StringBuilder();
            builder.append("Latest values for device ").append(deviceId).append(" (")
                    .append(device.getDeviceName()).append("):\n");
            builder.append("| Point ID | Point Name | Code | Value | Raw Value | Unit | Time |\n");
            builder.append("| --- | --- | --- | --- | --- | --- | --- |\n");
            for (FacadePointBO point : points.getRecords()) {
                FacadePointValueBO value = pointValueFacade.lastValue(tenantId, deviceId, point.getId());
                builder.append("| ")
                        .append(point.getId()).append(" | ")
                        .append(escape(point.getPointName())).append(" | ")
                        .append(escape(point.getPointCode())).append(" | ")
                        .append(Objects.isNull(value) ? "" : escape(value.getValue())).append(" | ")
                        .append(Objects.isNull(value) ? "" : escape(value.getRawValue())).append(" | ")
                        .append(escape(point.getUnit())).append(" | ")
                        .append(Objects.isNull(value) ? "" : value.getCreateTime()).append(" |\n");
            }
            return builder.toString();
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, limit={}",
                    "getDeviceLatestPointValues", tenantId, deviceId, size, e);
            return "Error retrieving device latest values: " + e.getMessage();
        }
    }

    @Tool(description = "Send a read command to a device for a specific point. The driver will read the current value from the physical device.")
    public String readPointValue(@ToolParam(description = "The device ID") Long deviceId,
                                 @ToolParam(description = "The point (metric) ID to read") Long pointId,
                                 ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue", tenantId,
                deviceId, pointId);
        recordTool(toolContext, "readPointValue", "Send point read command");
        try {
            boolean success = pointValueCommandFacade.read(tenantId, deviceId, pointId);
            return success ? "Read command sent successfully for device " + deviceId + " point " + pointId
                    : "Read command failed for device " + deviceId + " point " + pointId;
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue", tenantId,
                    deviceId, pointId, e);
            return "Error sending read command: " + e.getMessage();
        }
    }

    @Tool(description = "Send a write command to a device for a specific point. Sets the point to the specified value on the physical device.")
    public String writePointValue(@ToolParam(description = "The device ID") Long deviceId,
                                  @ToolParam(description = "The point (metric) ID to write") Long pointId,
                                  @ToolParam(description = "The value to write (as a string)") String value,
                                  ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, valueLength={}",
                "writePointValue", tenantId, deviceId, pointId, Objects.isNull(value) ? 0 : value.length());
        recordTool(toolContext, "writePointValue", "Prepare point write command");
        try {
            if (AgenticRequestContext.confirmActions(toolContext)) {
                RequestHeader.UserHeader header = AgenticRequestContext.requireUserHeader();
                String conversationId = AgenticRequestContext.requireConversationId(toolContext);
                String actionId = actionService.createWritePointValueAction(conversationId, deviceId, pointId, value,
                        header);
                return "Write command is pending user confirmation. actionId=" + actionId
                        + ". Ask the user to confirm before executing it.";
            }
            boolean success = pointValueCommandFacade.write(tenantId, deviceId, pointId, value);
            return success
                    ? "Write command sent successfully for device " + deviceId + " point " + pointId + " value=" + value
                    : "Write command failed for device " + deviceId + " point " + pointId;
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "writePointValue", tenantId,
                    deviceId, pointId, e);
            return "Error sending write command: " + e.getMessage();
        }
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "data", description);
    }

    private String escape(String value) {
        return Objects.toString(value, "").replace("|", "\\|").replace("\n", " ");
    }

}

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
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Point-value tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Slf4j
@Component
public class PointValueTool {

    private final PointValueFacade pointValueFacade;

    private final PointValueCommandFacade pointValueCommandFacade;

    private final ActionService actionService;

    public PointValueTool(PointValueFacade pointValueFacade, PointValueCommandFacade pointValueCommandFacade,
                          ActionService actionService) {
        this.pointValueFacade = pointValueFacade;
        this.pointValueCommandFacade = pointValueCommandFacade;
        this.actionService = actionService;
    }

    @Tool(description = "Get the latest point value for a specific device and point. Returns the current value.")
    public AgenticToolResult<FacadePointValueBO> getLatestPointValue(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "The point (metric) ID") Long pointId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "getLatestPointValue",
                tenantId, deviceId, pointId);
        recordTool(toolContext, "getLatestPointValue", "Get latest point value");
        try {
            FacadePointValueBO value = pointValueFacade.lastValue(tenantId, deviceId, pointId);
            if (Objects.isNull(value)) {
                return AgenticToolResult.empty("No latest value found for device " + deviceId + " point " + pointId,
                        null);
            }
            return AgenticToolResult.ok("Latest point value loaded", value);
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "getLatestPointValue",
                    tenantId, deviceId, pointId, e);
            return AgenticToolResult.error("Error retrieving latest value: " + e.getMessage());
        }
    }

    @Tool(description = "Get historical point values for a specific device and point. Returns raw values and chart-ready numeric points as structured data.")
    public AgenticToolResult<PointValueHistory> getPointValueHistory(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "The point (metric) ID") Long pointId,
            @ToolParam(description = "Number of historical records to retrieve") int count,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                "getPointValueHistory", tenantId, deviceId, pointId, count);
        recordTool(toolContext, "getPointValueHistory", "Get point value history");
        int size = Math.max(1, Math.min(count, 200));
        try {
            List<String> history = pointValueFacade.history(tenantId, deviceId, pointId, size);
            if (Objects.isNull(history) || history.isEmpty()) {
                return AgenticToolResult.empty("No history data found for device " + deviceId + " point " + pointId,
                        new PointValueHistory(deviceId, pointId, size, List.of(), null));
            }
            PointValueHistory result = new PointValueHistory(deviceId, pointId, size, history,
                    buildHistoryChart(deviceId, pointId, history));
            return AgenticToolResult.ok("Point value history loaded", result);
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                    "getPointValueHistory", tenantId, deviceId, pointId, size, e);
            return AgenticToolResult.error("Error retrieving history: " + e.getMessage());
        }
    }

    @Tool(description = "Send a read command to a device for a specific point. The driver will read the current value from the physical device.")
    public AgenticToolResult<PointCommandResult> readPointValue(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "The point (metric) ID to read") Long pointId,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue", tenantId,
                deviceId, pointId);
        recordTool(toolContext, "readPointValue", "Send point read command");
        try {
            boolean success = pointValueCommandFacade.read(tenantId, deviceId, pointId);
            PointCommandResult result = new PointCommandResult(deviceId, pointId, null, success, false, null);
            if (success) {
                return AgenticToolResult.ok("Read command sent", result);
            }
            return AgenticToolResult.error("Read command failed for device " + deviceId + " point " + pointId);
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue", tenantId,
                    deviceId, pointId, e);
            return AgenticToolResult.error("Error sending read command: " + e.getMessage());
        }
    }

    @Tool(description = "Send a write command to a device for a specific point. Sets the point to the specified value on the physical device.")
    public AgenticToolResult<PointCommandResult> writePointValue(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "The point (metric) ID to write") Long pointId,
            @ToolParam(description = "The value to write (as a string)") String value,
            ToolContext toolContext) {
        Long tenantId = AgenticRequestContext.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, valueLength={}",
                "writePointValue", tenantId, deviceId, pointId, Objects.isNull(value) ? 0 : value.length());
        recordTool(toolContext, "writePointValue", "Prepare point write command");
        try {
            if (Objects.isNull(deviceId) || Objects.isNull(pointId)) {
                return AgenticToolResult.invalid("Device ID and point ID are required for point write commands.");
            }
            if (StringUtils.isBlank(value)) {
                return AgenticToolResult.invalid("Point write value is required.");
            }
            RequestHeader.UserHeader header = AgenticRequestContext.requireUserHeader();
            String conversationId = AgenticRequestContext.requireConversationId(toolContext);
            String actionId = actionService.createWritePointValueAction(conversationId, deviceId, pointId, value,
                    header);
            return AgenticToolResult.ok("Write command is pending user confirmation",
                    new PointCommandResult(deviceId, pointId, value, false, true, actionId));
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "writePointValue", tenantId,
                    deviceId, pointId, e);
            return AgenticToolResult.error("Error sending write command: " + e.getMessage());
        }
    }

    private HistoryChart buildHistoryChart(Long deviceId, Long pointId, List<String> history) {
        List<List<Number>> dataPoints = new ArrayList<>();
        int rendered = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            String raw = history.get(i);
            if (Objects.isNull(raw)) {
                continue;
            }
            try {
                double value = Double.parseDouble(raw.trim());
                dataPoints.add(List.of(rendered, value));
                rendered++;
            } catch (NumberFormatException ignored) {
                // Keep non-numeric values in the raw history; only chart data skips them.
            }
        }
        if (rendered == 0) {
            return null;
        }
        return new HistoryChart("line", "Device " + deviceId + " / Point " + pointId, "index (oldest to newest)",
                "linear", List.of(new ChartSeries("value", dataPoints)));
    }

    private void recordTool(ToolContext toolContext, String toolName, String description) {
        AgenticRequestContext.recordToolInvocation(toolContext, toolName, "point-value", description);
    }

    public record PointValueHistory(Long deviceId, Long pointId, int requestedCount, List<String> values,
                                    HistoryChart chart) {

        public PointValueHistory {
            values = List.copyOf(Objects.requireNonNullElse(values, List.of()));
        }

    }

    public record HistoryChart(String type, String title, String xLabel, String xType, List<ChartSeries> series) {

        public HistoryChart {
            series = List.copyOf(Objects.requireNonNullElse(series, List.of()));
        }

    }

    public record ChartSeries(String name, List<List<Number>> data) {

        public ChartSeries {
            data = List.copyOf(Objects.requireNonNullElse(data, List.of()));
        }

    }

    public record PointCommandResult(Long deviceId, Long pointId, String value, boolean sent,
                                     boolean pendingConfirmation, String actionId) {
    }

}

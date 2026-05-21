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

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.agentic.annotation.AgenticToolMetadata;
import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.agentic.utils.AgenticVisualizationUtil;
import io.github.pnoker.common.constant.service.AgenticConstant;
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
import java.util.Map;
import java.util.Objects;

/**
 * Point-value tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueTool {

    private final PointValueFacade pointValueFacade;

    private final PointValueCommandFacade pointValueCommandFacade;

    private final ActionService actionService;

    @Tool(description = "Get the latest point value for a specific device and point. Returns the current value.")
    @AgenticToolMetadata(domain = "point-value", title = "Get latest point value")
    public AgenticToolResult<FacadePointValueBO> getLatestPointValue(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "The point (metric) ID") Long pointId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "getLatestPointValue",
                tenantId, deviceId, pointId);
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
    @AgenticToolMetadata(domain = "point-value", title = "Get point value history")
    public AgenticToolResult<PointValueHistory> getPointValueHistory(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "The point (metric) ID") Long pointId,
            @ToolParam(description = "Number of historical records to retrieve") int count,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                "getPointValueHistory", tenantId, deviceId, pointId, count);
        int size = AgenticToolUtil.clamp(count, 1, AgenticConstant.ToolLimit.MAX_HISTORY_RECORDS);
        try {
            List<String> history = pointValueFacade.history(tenantId, deviceId, pointId, size);
            if (AgenticToolUtil.isEmpty(history)) {
                return AgenticToolResult.empty("No history data found for device " + deviceId + " point " + pointId,
                        new PointValueHistory(deviceId, pointId, size, List.of(), null,
                                AgenticVisualizationUtil.NumericSummary.empty(0)));
            }
            AgenticVisualizationUtil.NumericSeries numericSeries =
                    AgenticVisualizationUtil.numericSeriesFromNewestFirst(history);
            PointValueHistory result = new PointValueHistory(deviceId, pointId, size, history,
                    buildHistoryChart(deviceId, pointId, numericSeries), numericSeries.summary());
            return AgenticToolResult.ok("Point value history loaded", result,
                    buildHistoryVisualizations(deviceId, pointId, numericSeries));
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                    "getPointValueHistory", tenantId, deviceId, pointId, size, e);
            return AgenticToolResult.error("Error retrieving history: " + e.getMessage());
        }
    }

    @Tool(description = "Send a read command to a device for a specific point. The driver will read the current value from the physical device.")
    @AgenticToolMetadata(domain = "point-value", title = "Send point read command")
    public AgenticToolResult<PointCommandResult> readPointValue(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "The point (metric) ID to read") Long pointId,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue", tenantId,
                deviceId, pointId);
        try {
            boolean success = pointValueCommandFacade.dispatchRead(tenantId, deviceId, pointId);
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

    @Tool(description = "Prepare a point write command for a specific device and point. This tool never writes directly; it creates a pending action that requires explicit user confirmation before execution.")
    @AgenticToolMetadata(domain = "point-value", title = "Prepare point write command")
    public AgenticToolResult<PointCommandResult> writePointValue(
            @ToolParam(description = "The device ID") Long deviceId,
            @ToolParam(description = "The point (metric) ID to write") Long pointId,
            @ToolParam(description = "The value to write (as a string)") String value,
            ToolContext toolContext) {
        Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
        log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, valueLength={}",
                "writePointValue", tenantId, deviceId, pointId, Objects.isNull(value) ? 0 : value.length());
        try {
            if (Objects.isNull(deviceId) || Objects.isNull(pointId)) {
                return AgenticToolResult.invalid("Device ID and point ID are required for point write commands.");
            }
            if (StringUtils.isBlank(value)) {
                return AgenticToolResult.invalid("Point write value is required.");
            }
            RequestHeader.UserHeader header = AgenticToolContextUtil.requireUserHeader(toolContext);
            String conversationId = AgenticToolContextUtil.requireConversationId(toolContext);
            String actionId = actionService.createWritePointValueAction(conversationId, deviceId, pointId, value,
                    header);
            return AgenticToolResult.ok("Write command is pending user confirmation",
                    new PointCommandResult(deviceId, pointId, value, false, true, actionId));
        } catch (Exception e) {
            log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "writePointValue", tenantId,
                    deviceId, pointId, e);
            return AgenticToolResult.error("Error preparing write command: " + e.getMessage());
        }
    }

    private HistoryChart buildHistoryChart(Long deviceId, Long pointId,
                                           AgenticVisualizationUtil.NumericSeries numericSeries) {
        if (Objects.isNull(numericSeries) || numericSeries.dataset().isEmpty()) {
            return null;
        }
        List<List<Number>> dataPoints = new ArrayList<>();
        for (Map<String, Object> row : numericSeries.dataset()) {
            Object index = row.get(AgenticVisualizationUtil.FIELD_INDEX);
            Object value = row.get(AgenticVisualizationUtil.FIELD_VALUE);
            if (index instanceof Number indexNumber && value instanceof Number valueNumber) {
                dataPoints.add(List.of(indexNumber, valueNumber));
            }
        }
        return new HistoryChart(AgenticConstant.Visualization.Type.LINE, "Device " + deviceId + " / Point " + pointId,
                "index (oldest to newest)", AgenticConstant.Visualization.Scale.LINEAR,
                List.of(new ChartSeries("value", dataPoints)));
    }

    private List<AgenticVisualizationSpec> buildHistoryVisualizations(Long deviceId, Long pointId,
                                                                      AgenticVisualizationUtil.NumericSeries series) {
        if (Objects.isNull(series) || series.dataset().isEmpty()) {
            return List.of();
        }
        Map<String, Object> meta = AgenticVisualizationUtil.pointHistoryMeta(deviceId, pointId, "calValue");
        AgenticVisualizationSpec.Encode encode = AgenticVisualizationSpec.Encode.xy(
                AgenticVisualizationUtil.FIELD_INDEX, AgenticVisualizationUtil.FIELD_VALUE);
        encode.setColor(AgenticVisualizationUtil.FIELD_SERIES);
        List<AgenticVisualizationSpec.Annotation> annotations = Objects.nonNull(series.summary().average())
                ? List.of(AgenticVisualizationUtil.yAnnotation(series.summary().average(), "Average"))
                : List.of();
        AgenticVisualizationSpec line = AgenticVisualizationUtil.line(
                "point-value-history-" + deviceId + "-" + pointId,
                "Device " + deviceId + " / Point " + pointId,
                "Point value history",
                series.dataset(),
                encode,
                meta,
                annotations);
        AgenticVisualizationSpec stat = AgenticVisualizationUtil.stat(
                "point-value-history-summary-" + deviceId + "-" + pointId,
                "Point value summary",
                "Numeric summary of the returned history window",
                AgenticVisualizationUtil.statRow(series.summary()),
                meta);
        return List.of(line, stat);
    }

    public record PointValueHistory(Long deviceId, Long pointId, int requestedCount, List<String> values,
                                    HistoryChart chart, AgenticVisualizationUtil.NumericSummary summary) {

        public PointValueHistory {
            values = List.copyOf(Objects.requireNonNullElse(values, List.of()));
            summary = Objects.requireNonNullElseGet(summary, () -> AgenticVisualizationUtil.NumericSummary.empty(0));
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

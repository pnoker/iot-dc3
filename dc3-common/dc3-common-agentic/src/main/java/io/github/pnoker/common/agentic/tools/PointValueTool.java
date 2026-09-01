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
import io.github.pnoker.common.agentic.entity.model.AgenticVisualizationSpec;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.agentic.utils.AgenticToolContextUtil;
import io.github.pnoker.common.agentic.utils.AgenticToolUtil;
import io.github.pnoker.common.agentic.utils.AgenticVisualizationUtil;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Point-value tools exposed to the LLM via Spring AI @Tool.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueTool {

    private final PointValueFacade pointValueFacade;

    private final PointCommandFacade pointCommandFacade;

    private final ActionService actionService;

    /** Reactive latest-value lookup for the non-blocking tool registry. */
    public Mono<AgenticToolResult<FacadePointValueBO>> getLatestPointValueReactive(
            Long deviceId, Long pointId, ToolContext toolContext) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
            if (deviceId == null || pointId == null) {
                return Mono.just(AgenticToolResult.invalid("Device ID and point ID are required."));
            }
            log.debug("Agentic reactive tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}",
                    "getLatestPointValue", tenantId, deviceId, pointId);
            return pointValueFacade.lastValue(tenantId, deviceId, pointId)
                    .map(value -> AgenticToolResult.ok("Latest point value loaded", value))
                    .defaultIfEmpty(AgenticToolResult.empty(
                            "No latest value found for device " + deviceId + " point " + pointId, null))
                    .onErrorResume(error -> {
                        log.warn("Agentic reactive tool failed, tool={}, tenantId={}, deviceId={}, pointId={}",
                                "getLatestPointValue", tenantId, deviceId, pointId, error);
                        return Mono.just(AgenticToolResult.error("Error retrieving latest value: " + error.getMessage()));
                    });
        });
    }

    /** Reactive history lookup for the non-blocking tool registry. */
    public Mono<AgenticToolResult<PointValueHistory>> getPointValueHistoryReactive(
            Long deviceId, Long pointId, int count, ToolContext toolContext) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
            if (deviceId == null || pointId == null) {
                return Mono.just(AgenticToolResult.invalid("Device ID and point ID are required."));
            }
            int size = AgenticToolUtil.clamp(count, 1, AgenticConstant.ToolLimit.MAX_HISTORY_RECORDS);
            return pointValueFacade.history(tenantId, deviceId, pointId, null, size)
                    .map(history -> {
                        if (history.items().isEmpty()) {
                            return AgenticToolResult.empty(
                                    "No history data found for device " + deviceId + " point " + pointId,
                                    new PointValueHistory(deviceId, pointId, size, List.of(), null,
                                            AgenticVisualizationUtil.NumericSummary.empty(0)));
                        }
                        List<String> values = history.items().stream().map(FacadePointValueBO::getValue).toList();
                        AgenticVisualizationUtil.NumericSeries numericSeries =
                                AgenticVisualizationUtil.numericSeriesFromNewestFirst(values);
                        PointValueHistory result = new PointValueHistory(deviceId, pointId, size, values,
                                buildHistoryChart(deviceId, pointId, numericSeries), numericSeries.summary());
                        return AgenticToolResult.ok("Point value history loaded", result,
                                buildHistoryVisualizations(deviceId, pointId, numericSeries));
                    })
                    .onErrorResume(error -> {
                        log.warn("Agentic reactive tool failed, tool={}, tenantId={}, deviceId={}, pointId={}, count={}",
                                "getPointValueHistory", tenantId, deviceId, pointId, size, error);
                        return Mono.just(AgenticToolResult.error("Error retrieving history: " + error.getMessage()));
                    });
        });
    }

    /**
     * Read point value.
     *
     * @param deviceId    device identifier
     * @param pointId     point identifier
     * @param toolContext tool context
     * @return read point value result
     */
    public Mono<AgenticToolResult<PointCommandResult>> readPointValueReactive(
            Long deviceId, Long pointId,
            ToolContext toolContext) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
            if (deviceId == null || pointId == null) {
                return Mono.just(AgenticToolResult.invalid("Device ID and point ID are required."));
            }
            log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue", tenantId,
                    deviceId, pointId);
            return pointCommandFacade.submitRead(tenantId, deviceId, pointId, PointCommandSourceEnum.AGENTIC)
                    .map(commandId -> AgenticToolResult.ok("Read command accepted",
                            new PointCommandResult(deviceId, pointId, commandId, true, false, null)))
                    .onErrorResume(error -> {
                        log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "readPointValue",
                                tenantId, deviceId, pointId, error);
                        return Mono.just(AgenticToolResult.error("Error sending read command: " + error.getMessage()));
                    });
        });
    }

    /**
     * Write point value.
     *
     * @param deviceId    device identifier
     * @param pointId     point identifier
     * @param value       value
     * @param toolContext tool context
     * @return write point value result
     */
    public Mono<AgenticToolResult<PointCommandResult>> writePointValueReactive(Long deviceId, Long pointId, String value,
            ToolContext toolContext) {
        return Mono.defer(() -> {
            Long tenantId = AgenticToolContextUtil.requireTenantId(toolContext);
            log.debug("Agentic tool invoked, tool={}, tenantId={}, deviceId={}, pointId={}, valueLength={}",
                    "writePointValue", tenantId, deviceId, pointId, Objects.isNull(value) ? 0 : value.length());
            if (Objects.isNull(deviceId) || Objects.isNull(pointId)) {
                return Mono.just(AgenticToolResult.invalid("Device ID and point ID are required for point write commands."));
            }
            if (StringUtils.isBlank(value)) {
                return Mono.just(AgenticToolResult.invalid("Point write value is required."));
            }
            RequestHeader.PrincipalHeader header = AgenticToolContextUtil.requirePrincipalHeader(toolContext);
            String conversationId = AgenticToolContextUtil.requireConversationId(toolContext);
            return actionService.createWritePointValueAction(conversationId, deviceId, pointId, value, header)
                    .map(actionId -> AgenticToolResult.ok("Write command is pending user confirmation",
                            new PointCommandResult(deviceId, pointId, value, false, true, actionId)))
                    .onErrorResume(error -> {
                        log.warn("Agentic tool failed, tool={}, tenantId={}, deviceId={}, pointId={}", "writePointValue",
                                tenantId, deviceId, pointId, error);
                        return Mono.just(AgenticToolResult.error("Error preparing write command: " + error.getMessage()));
                    });
        });
    }

    /**
     * Build a line history chart from a numeric series, mapping each row to an
     * (index, value) data point. Returns null when the series is empty.
     *
     * @param deviceId      the device id, for the chart title
     * @param pointId       the point id, for the chart title
     * @param numericSeries the numeric series to plot
     * @return the history chart, or null when there is no data
     */
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

    /**
     * Build line and stat visualization specs for a point-value history, including a
     * y-axis average annotation when the series summary has one. Returns an empty list
     * when the series is empty.
     *
     * @param deviceId the device id
     * @param pointId  the point id
     * @param series   the numeric series to visualize
     * @return the visualization specs, or an empty list when there is no data
     */
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
                "point-value-history" + SymbolConstant.HYPHEN + deviceId + SymbolConstant.HYPHEN + pointId,
                "Device " + deviceId + " / Point " + pointId,
                "Point value history",
                series.dataset(),
                encode,
                meta,
                annotations);
        AgenticVisualizationSpec stat = AgenticVisualizationUtil.stat(
                "point-value-history-summary" + SymbolConstant.HYPHEN + deviceId + SymbolConstant.HYPHEN + pointId,
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

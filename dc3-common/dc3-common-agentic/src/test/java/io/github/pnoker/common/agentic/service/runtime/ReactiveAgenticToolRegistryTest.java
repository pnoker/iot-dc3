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
package io.github.pnoker.common.agentic.service.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.pnoker.common.agentic.entity.model.AgenticToolResult;
import io.github.pnoker.common.agentic.tools.CommandTool;
import io.github.pnoker.common.agentic.tools.DeviceTool;
import io.github.pnoker.common.agentic.tools.DriverTool;
import io.github.pnoker.common.agentic.tools.EventTool;
import io.github.pnoker.common.agentic.tools.HistoryTool;
import io.github.pnoker.common.agentic.tools.InsightTool;
import io.github.pnoker.common.agentic.tools.PointTool;
import io.github.pnoker.common.agentic.tools.PointValueTool;
import io.github.pnoker.common.agentic.tools.ProfileTool;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

class ReactiveAgenticToolRegistryTest {
    @Test
    void allRegisteredToolsAreUniqueAndUseOffsetPagination() {
        var registry = new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                mock(InsightTool.class),
                mock(HistoryTool.class),
                new ObjectMapper());
        var tools = registry.tools();
        assertThat(tools.keySet()).doesNotHaveDuplicates();
        tools.values().stream()
                .filter(tool -> tool.definition().name().startsWith("search")
                        || tool.definition().name().startsWith("list"))
                .forEach(tool -> {
                    assertThat(tool.definition().inputSchema()).contains("offset", "limit");
                    assertThat(tool.definition().inputSchema()).doesNotContain("current", "size", "page");
                });
    }

    @Test
    void everyToolInputSchemaIsParseableJson() {
        var registry = new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                mock(InsightTool.class),
                mock(HistoryTool.class),
                new ObjectMapper());
        registry.tools().forEach((name, tool) -> {
            String schema = tool.definition().inputSchema();
            org.assertj.core.api.Assertions.assertThatCode(() -> new ObjectMapper().readTree(schema))
                    .as("tool %s must expose a parseable JSON schema, got: %s", name, schema)
                    .doesNotThrowAnyException();
        });
    }

    @Test
    void registryExposesTheExactToolSurfaceSoNoToolGoesOrphan() {
        var registry = new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                mock(InsightTool.class),
                mock(HistoryTool.class),
                new ObjectMapper());
        assertThat(registry.tools().keySet())
                .containsExactlyInAnyOrder(
                        // point value read/write
                        "writePointValue",
                        "readPointValue",
                        "getLatestPointValue",
                        "getPointValueHistory",
                        // point lookup/search
                        "lookupPointById",
                        "lookupPointsByIds",
                        "searchPoints",
                        "listPointsByDevice",
                        "listPointsByProfile",
                        // profile lookup/search
                        "lookupProfileById",
                        "lookupProfilesByIds",
                        "searchProfiles",
                        "listProfilesByDeviceId",
                        // command lookup/search
                        "lookupCommandById",
                        "lookupCommandsByIds",
                        "searchCommands",
                        "listCommandsByDeviceId",
                        "listCommandsByProfileId",
                        // event lookup/search
                        "lookupEventById",
                        "lookupEventsByIds",
                        "searchEvents",
                        "listEventsByDeviceId",
                        "listEventsByProfileId",
                        // device lookup/search
                        "lookupDeviceById",
                        "lookupDevicesByIds",
                        "searchDevices",
                        "listDevicesByDriverId",
                        "listDevicesByProfileId",
                        "getDeviceLatestPointValues",
                        // driver lookup/search
                        "lookupDriverById",
                        "lookupDriversByIds",
                        "searchDrivers",
                        // insight: alert analytics
                        "getAlertStats",
                        "getLatestAlerts",
                        "pageAlerts",
                        "getAlertTrend",
                        "getAlertTopSources",
                        "getAlertTypeDistribution",
                        "getAlertAgingBacklog",
                        "getAlertMtta",
                        // insight: dashboard stats
                        "getTodayStats",
                        "getStatsTimeseries",
                        "getTopEntities",
                        "getLatestValueStream",
                        "getLatencyHistogram",
                        "getHourlyActivity",
                        "getProtocolHealth",
                        // insight: analytics agent surface
                        "analyticsQueryLatest",
                        "analyticsQueryHistory",
                        "analyticsComputeStats",
                        "analyticsComparePeriods",
                        "analyticsRankEntities",
                        "analyticsTrendAnalysis",
                        "analyticsThresholdReport",
                        "analyticsCorrelate",
                        "analyticsDataQualityReport",
                        // insight: point command closed loop
                        "getPointCommandStatus",
                        // command/event history traceability
                        "lookupCommandHistoryByRecordId",
                        "searchCommandHistories",
                        "lookupEventHistoryByRecordId",
                        "searchEventHistories");
    }

    @Test
    void malformedArgumentsBecomeStructuredErrors() {
        var registry = new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                mock(InsightTool.class),
                mock(HistoryTool.class),
                new ObjectMapper());
        StepVerifier.create(registry.tools().get("searchDevices").call("{", new ToolContext(Map.of())))
                .assertNext(result -> {
                    assertThat(result).isInstanceOf(AgenticToolResult.class);
                    assertThat(((AgenticToolResult<?>) result).success()).isFalse();
                })
                .verifyComplete();
    }
}

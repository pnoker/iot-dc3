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
import io.github.pnoker.common.agentic.tools.SystemTool;
import io.github.pnoker.common.agentic.tools.TenantTool;
import io.github.pnoker.common.agentic.tools.UserTool;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

class ReactiveAgenticToolRegistryTest {

    private static ReactiveAgenticToolRegistry fullRegistry() {
        return new ReactiveAgenticToolRegistry(
                mock(PointValueTool.class),
                mock(PointTool.class),
                mock(ProfileTool.class),
                mock(CommandTool.class),
                mock(EventTool.class),
                mock(DeviceTool.class),
                mock(DriverTool.class),
                mock(InsightTool.class),
                mock(HistoryTool.class),
                mock(TenantTool.class),
                mock(UserTool.class),
                mock(SystemTool.class),
                new ObjectMapper(),
                null);
    }

    @Test
    void allRegisteredToolsAreUniqueAndUseOffsetPagination() {
        var registry = fullRegistry();
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
        var registry = fullRegistry();
        registry.tools().forEach((name, tool) -> {
            String schema = tool.definition().inputSchema();
            org.assertj.core.api.Assertions.assertThatCode(() -> new ObjectMapper().readTree(schema))
                    .as("tool %s must expose a parseable JSON schema, got: %s", name, schema)
                    .doesNotThrowAnyException();
        });
    }

    @Test
    void registryExposesTheExactToolSurfaceSoNoToolGoesOrphan() {
        var registry = fullRegistry();
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
                        "searchEventHistories",
                        // tenant/user/system context lookups (formerly the @Tool channel)
                        "getCurrentTenantInfo",
                        "getCurrentUserProfile",
                        "getSystemHealth",
                        "getDeviceStatuses",
                        "getDeviceStatusesByProfile",
                        "getDriverStatuses",
                        "getDriverDeviceStatusSummary");
    }

    @Test
    void malformedArgumentsBecomeStructuredErrors() {
        var registry = fullRegistry();
        StepVerifier.create(registry.tools().get("searchDevices").call("{", new ToolContext(Map.of())))
                .assertNext(result -> {
                    assertThat(result).isInstanceOf(AgenticToolResult.class);
                    assertThat(((AgenticToolResult<?>) result).success()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void toolsSnapshotIsImmutableAndSharedAcrossCalls() {
        var registry = fullRegistry();
        var first = registry.tools();
        var second = registry.tools();
        assertThat(second).isSameAs(first);
        org.assertj.core.api.Assertions.assertThatCode(() -> first.put("rogue", mock(ReactiveAgenticTool.class)))
                .as("the snapshot must reject registration after construction")
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void analyticsSchemasSpellOutSeriesAndWindowItems() {
        var registry = fullRegistry();
        for (String name : new String[] {
            "analyticsQueryLatest",
            "analyticsQueryHistory",
            "analyticsComputeStats",
            "analyticsComparePeriods",
            "analyticsRankEntities",
            "analyticsTrendAnalysis",
            "analyticsThresholdReport",
            "analyticsCorrelate",
            "analyticsDataQualityReport"
        }) {
            String schema = registry.tools().get(name).definition().inputSchema();
            assertThat(schema)
                    .as("analytics tool %s must declare structured series/window items", name)
                    .containsAnyOf("series", "seriesA", "window");
            if (schema.contains("series") || schema.contains("seriesA")) {
                assertThat(schema).contains("deviceId", "pointId", "deviceName", "pointName");
            }
            if (schema.contains("window")) {
                assertThat(schema).contains("fromIso", "toIso", "rangeHours");
            }
        }
    }

    @Test
    void registeredDomainsAreExplicitNotHeuristic() {
        var registry = fullRegistry();
        Map<String, String> expectedDomains = Map.ofEntries(
                Map.entry("searchPoints", "point"),
                Map.entry("writePointValue", "pointValue"),
                Map.entry("searchProfiles", "profile"),
                Map.entry("searchCommands", "command"),
                Map.entry("searchEvents", "event"),
                Map.entry("searchDevices", "device"),
                Map.entry("searchDrivers", "driver"),
                Map.entry("getAlertStats", "insight"),
                Map.entry("searchCommandHistories", "history"),
                Map.entry("getCurrentTenantInfo", "tenant"),
                Map.entry("getCurrentUserProfile", "user"),
                Map.entry("getSystemHealth", "system"));
        expectedDomains.forEach(
                (toolName, domain) -> assertThat(registry.tools().get(toolName).domain())
                        .as("tool %s must declare its domain explicitly", toolName)
                        .isEqualTo(domain));
    }

    @Test
    void numberCoercionAcceptsLlmFriendlyIntegerForms() {
        assertThat(ReactiveAgenticToolRegistry.number(10)).isEqualTo(10L);
        assertThat(ReactiveAgenticToolRegistry.number(10.0)).isEqualTo(10L);
        assertThat(ReactiveAgenticToolRegistry.number(1e3)).isEqualTo(1000L);
        assertThat(ReactiveAgenticToolRegistry.number("42")).isEqualTo(42L);
        assertThat(ReactiveAgenticToolRegistry.number("10.0")).isEqualTo(10L);
        assertThat(ReactiveAgenticToolRegistry.number("1e3")).isEqualTo(1000L);
        assertThat(ReactiveAgenticToolRegistry.number(null)).isNull();
        assertThat(ReactiveAgenticToolRegistry.number("abc")).isNull();
        assertThat(ReactiveAgenticToolRegistry.number(10.7)).isNull();
        assertThat(ReactiveAgenticToolRegistry.number("-7")).isEqualTo(-7L);
    }
}

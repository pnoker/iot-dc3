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

import io.github.pnoker.common.agentic.tools.CommandTool;
import io.github.pnoker.common.agentic.tools.DeviceTool;
import io.github.pnoker.common.agentic.tools.DriverTool;
import io.github.pnoker.common.agentic.tools.EventTool;
import io.github.pnoker.common.agentic.tools.HistoryTool;
import io.github.pnoker.common.agentic.tools.InsightTool;
import io.github.pnoker.common.agentic.tools.PointTool;
import io.github.pnoker.common.agentic.tools.PointValueTool;
import io.github.pnoker.common.agentic.tools.ProfileTool;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/** Registry for tools that must remain non-blocking end to end. */
@Component
public class ReactiveAgenticToolRegistry {

    private static final ToolDefinition WRITE_POINT_VALUE = DefaultToolDefinition.builder()
            .name("writePointValue")
            .description("Prepare a point write command for explicit user confirmation.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"pointId":{"type":"integer"},"value":{"type":"string"}},"required":["deviceId","pointId","value"]}
                    """)
            .build();
    private static final ToolDefinition READ_POINT_VALUE = DefaultToolDefinition.builder()
            .name("readPointValue")
            .description("Send a point read command and return its command identifier.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"pointId":{"type":"integer"}},"required":["deviceId","pointId"]}
                    """)
            .build();
    private static final ToolDefinition GET_LATEST_POINT_VALUE = DefaultToolDefinition.builder()
            .name("getLatestPointValue")
            .description("Get the latest value for a device point without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"pointId":{"type":"integer"}},"required":["deviceId","pointId"]}
                    """)
            .build();
    private static final ToolDefinition GET_POINT_VALUE_HISTORY = DefaultToolDefinition.builder()
            .name("getPointValueHistory")
            .description(
                    "Get recent point values and chart-ready numeric summaries without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"pointId":{"type":"integer"},"count":{"type":"integer","minimum":1}},"required":["deviceId","pointId","count"]}
                    """)
            .build();
    private static final ToolDefinition SEARCH_POINTS = DefaultToolDefinition.builder()
            .name("searchPoints")
            .description(
                    "Search tenant-scoped points with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"pointName":{"type":"string"},"profileId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LIST_POINTS_BY_DEVICE = DefaultToolDefinition.builder()
            .name("listPointsByDevice")
            .description(
                    "List points bound to a device with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["deviceId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LIST_POINTS_BY_PROFILE = DefaultToolDefinition.builder()
            .name("listPointsByProfile")
            .description(
                    "List points under a profile with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"profileId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["profileId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LOOKUP_POINT = DefaultToolDefinition.builder()
            .name("lookupPointById")
            .description("Look up one tenant-scoped point without blocking the agent runtime.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"pointId\":{\"type\":\"integer\"}},\"required\":[\"pointId\"]}")
            .build();
    private static final ToolDefinition LOOKUP_POINTS = DefaultToolDefinition.builder()
            .name("lookupPointsByIds")
            .description("Look up tenant-scoped points by IDs without blocking the agent runtime.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"pointIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}},\"required\":[\"pointIds\"]}")
            .build();
    private static final ToolDefinition SEARCH_PROFILES = DefaultToolDefinition.builder()
            .name("searchProfiles")
            .description(
                    "Search tenant-scoped profiles with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"profileName":{"type":"string"},"profileCode":{"type":"string"},"profileType":{"type":"string"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LOOKUP_PROFILE = DefaultToolDefinition.builder()
            .name("lookupProfileById")
            .description("Look up one tenant-scoped profile without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"profileId":{"type":"integer"}},"required":["profileId"]}
                    """)
            .build();
    private static final ToolDefinition LOOKUP_PROFILES = DefaultToolDefinition.builder()
            .name("lookupProfilesByIds")
            .description("Look up tenant-scoped profiles by IDs without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"profileIds":{"type":"array","items":{"type":"integer"}}},"required":["profileIds"]}
                    """)
            .build();
    private static final ToolDefinition LIST_PROFILES_BY_DEVICE = DefaultToolDefinition.builder()
            .name("listProfilesByDeviceId")
            .description(
                    "List profiles bound to a device with canonical offset pagination without blocking the agent runtime.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["deviceId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition SEARCH_COMMANDS = DefaultToolDefinition.builder()
            .name("searchCommands")
            .description("Search tenant-scoped commands with canonical offset pagination.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"commandName\":{\"type\":\"string\"},\"profileId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}")
            .build();
    private static final ToolDefinition LOOKUP_COMMAND = DefaultToolDefinition.builder()
            .name("lookupCommandById")
            .description("Look up one tenant-scoped command without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"commandId\":{\"type\":\"integer\"}},\"required\":[\"commandId\"]}")
            .build();
    private static final ToolDefinition LOOKUP_COMMANDS = DefaultToolDefinition.builder()
            .name("lookupCommandsByIds")
            .description("Look up tenant-scoped commands by IDs without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"commandIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}},\"required\":[\"commandIds\"]}")
            .build();
    private static final ToolDefinition LIST_COMMANDS_BY_DEVICE = DefaultToolDefinition.builder()
            .name("listCommandsByDeviceId")
            .description("List commands bound to a device with canonical offset pagination without blocking.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["deviceId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LIST_COMMANDS_BY_PROFILE = DefaultToolDefinition.builder()
            .name("listCommandsByProfileId")
            .description("List commands under a profile with canonical offset pagination without blocking.")
            .inputSchema("""
                    {"type":"object","properties":{"profileId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["profileId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition SEARCH_EVENTS = DefaultToolDefinition.builder()
            .name("searchEvents")
            .description("Search tenant-scoped events with canonical offset pagination.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"eventName\":{\"type\":\"string\"},\"profileId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}")
            .build();
    private static final ToolDefinition LOOKUP_EVENT = DefaultToolDefinition.builder()
            .name("lookupEventById")
            .description("Look up one tenant-scoped event without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"eventId\":{\"type\":\"integer\"}},\"required\":[\"eventId\"]}")
            .build();
    private static final ToolDefinition LOOKUP_EVENTS = DefaultToolDefinition.builder()
            .name("lookupEventsByIds")
            .description("Look up tenant-scoped events by IDs without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"eventIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}},\"required\":[\"eventIds\"]}")
            .build();
    private static final ToolDefinition LIST_EVENTS_BY_DEVICE = DefaultToolDefinition.builder()
            .name("listEventsByDeviceId")
            .description("List events bound to a device with canonical offset pagination without blocking.")
            .inputSchema("""
                    {"type":"object","properties":{"deviceId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["deviceId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition LIST_EVENTS_BY_PROFILE = DefaultToolDefinition.builder()
            .name("listEventsByProfileId")
            .description("List events under a profile with canonical offset pagination without blocking.")
            .inputSchema("""
                    {"type":"object","properties":{"profileId":{"type":"integer"},"offset":{"type":"integer","minimum":0},"limit":{"type":"integer","minimum":1,"maximum":200}},"required":["profileId","offset","limit"]}
                    """)
            .build();
    private static final ToolDefinition DEVICE_LATEST_VALUES = DefaultToolDefinition.builder()
            .name("getDeviceLatestPointValues")
            .description("Load a device latest-point snapshot without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"deviceId\":{\"type\":\"integer\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50}},\"required\":[\"deviceId\",\"limit\"]}")
            .build();
    private static final ToolDefinition LOOKUP_DEVICE = DefaultToolDefinition.builder()
            .name("lookupDeviceById")
            .description("Look up one tenant-scoped device without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"deviceId\":{\"type\":\"integer\"}},\"required\":[\"deviceId\"]}")
            .build();
    private static final ToolDefinition LOOKUP_DEVICES = DefaultToolDefinition.builder()
            .name("lookupDevicesByIds")
            .description("Look up tenant-scoped devices by IDs without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"deviceIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}},\"required\":[\"deviceIds\"]}")
            .build();
    private static final ToolDefinition SEARCH_DEVICES = DefaultToolDefinition.builder()
            .name("searchDevices")
            .description("Search tenant-scoped devices with canonical offset pagination.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"deviceName\":{\"type\":\"string\"},\"deviceCode\":{\"type\":\"string\"},\"driverId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}")
            .build();
    private static final ToolDefinition LIST_DEVICES_BY_DRIVER = DefaultToolDefinition.builder()
            .name("listDevicesByDriverId")
            .description("List devices for a driver with canonical offset pagination without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"driverId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"driverId\",\"offset\",\"limit\"]}")
            .build();
    private static final ToolDefinition LIST_DEVICES_BY_PROFILE = DefaultToolDefinition.builder()
            .name("listDevicesByProfileId")
            .description("List devices for a profile with canonical offset pagination without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"profileId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"profileId\",\"offset\",\"limit\"]}")
            .build();
    private static final ToolDefinition LOOKUP_DRIVER = DefaultToolDefinition.builder()
            .name("lookupDriverById")
            .description("Look up one tenant-scoped driver without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"driverId\":{\"type\":\"integer\"}},\"required\":[\"driverId\"]}")
            .build();
    private static final ToolDefinition LOOKUP_DRIVERS = DefaultToolDefinition.builder()
            .name("lookupDriversByIds")
            .description("Look up tenant-scoped drivers by IDs without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"driverIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}},\"required\":[\"driverIds\"]}")
            .build();

    private static ToolDefinition insight(String name, String description, String schema) {
        return DefaultToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(schema)
                .build();
    }

    private static final ToolDefinition GET_ALERT_STATS = insight(
            "getAlertStats",
            "Get the tenant's alert totals with a breakdown by alarm type.",
            "{\"type\":\"object\",\"properties\":{}}");
    private static final ToolDefinition GET_LATEST_ALERTS = insight(
            "getLatestAlerts",
            "Get the most recent alerts, newest first.",
            "{\"type\":\"object\",\"properties\":{\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}}}");
    private static final ToolDefinition PAGE_ALERTS = insight(
            "pageAlerts",
            "Page through alerts with optional source, type and confirm filters.",
            "{\"type\":\"object\",\"properties\":{\"source\":{\"type\":\"string\",\"enum\":[\"device\",\"driver\"]},\"sourceId\":{\"type\":\"integer\"},\"alarmTypeFlag\":{\"type\":\"integer\"},\"confirmFlag\":{\"type\":\"integer\"},\"rangeKey\":{\"type\":\"string\",\"enum\":[\"today\",\"24h\",\"7d\",\"30d\"]},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}");
    private static final ToolDefinition GET_ALERT_TREND = insight(
            "getAlertTrend",
            "Get the daily device/driver alert trend over the last days.",
            "{\"type\":\"object\",\"properties\":{\"days\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":365}}}");
    private static final ToolDefinition GET_ALERT_TOP_SOURCES = insight(
            "getAlertTopSources",
            "Get the noisiest alert sources over the last days.",
            "{\"type\":\"object\",\"properties\":{\"days\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":365},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}}}");
    private static final ToolDefinition GET_ALERT_TYPE_DISTRIBUTION = insight(
            "getAlertTypeDistribution",
            "Get the alert type distribution over the last days.",
            "{\"type\":\"object\",\"properties\":{\"days\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":365}}}");
    private static final ToolDefinition GET_ALERT_AGING_BACKLOG = insight(
            "getAlertAgingBacklog",
            "Get the unconfirmed alert aging backlog buckets (under 1h, 1-6h, 6-24h, over 24h).",
            "{\"type\":\"object\",\"properties\":{}}");
    private static final ToolDefinition GET_ALERT_MTTA = insight(
            "getAlertMtta",
            "Get the mean-time-to-acknowledge trend over the last days.",
            "{\"type\":\"object\",\"properties\":{\"days\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":365}}}");
    private static final ToolDefinition GET_TODAY_STATS = insight(
            "getTodayStats",
            "Get today's, yesterday's and cumulative entity counts with day-over-day change.",
            "{\"type\":\"object\",\"properties\":{}}");
    private static final ToolDefinition GET_STATS_TIMESERIES = insight(
            "getStatsTimeseries",
            "Get bucketed entity counts (hour or day granularity) over a rolling window.",
            "{\"type\":\"object\",\"properties\":{\"granularity\":{\"type\":\"string\",\"enum\":[\"hour\",\"day\"]},\"rangeHours\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":8760}}}");
    private static final ToolDefinition GET_TOP_ENTITIES = insight(
            "getTopEntities",
            "Get the most active devices, points or drivers over a rolling window.",
            "{\"type\":\"object\",\"properties\":{\"dimension\":{\"type\":\"string\",\"enum\":[\"device\",\"point\",\"driver\"]},\"rangeHours\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":8760},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}}}");
    private static final ToolDefinition GET_LATEST_VALUE_STREAM = insight(
            "getLatestValueStream",
            "Get the latest point value stream across devices.",
            "{\"type\":\"object\",\"properties\":{\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}}}");
    private static final ToolDefinition GET_LATENCY_HISTOGRAM = insight(
            "getLatencyHistogram",
            "Get the point-value collection latency histogram over a rolling window.",
            "{\"type\":\"object\",\"properties\":{\"rangeHours\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":8760}}}");
    private static final ToolDefinition GET_HOURLY_ACTIVITY = insight(
            "getHourlyActivity",
            "Get the hour-of-week activity grid over a rolling window.",
            "{\"type\":\"object\",\"properties\":{\"rangeHours\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":8760}}}");
    private static final ToolDefinition GET_PROTOCOL_HEALTH = insight(
            "getProtocolHealth",
            "Get per-protocol fleet health: driver and device counts per protocol service.",
            "{\"type\":\"object\",\"properties\":{}}");
    private static final ToolDefinition ANALYTICS_QUERY_LATEST = insight(
            "analyticsQueryLatest",
            "Analytics: latest values for up to 20 series.",
            "{\"type\":\"object\",\"properties\":{\"series\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"deviceId\":{\"type\":\"integer\"},\"pointId\":{\"type\":\"integer\"},\"deviceName\":{\"type\":\"string\"},\"pointName\":{\"type\":\"string\"}}}}},\"required\":[\"series\"]}");
    private static final ToolDefinition ANALYTICS_QUERY_HISTORY = insight(
            "analyticsQueryHistory",
            "Analytics: RAW or M4 downsampled history per series.",
            "{\"type\":\"object\",\"properties\":{\"series\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}},\"window\":{\"type\":\"object\",\"properties\":{\"fromIso\":{\"type\":\"string\"},\"toIso\":{\"type\":\"string\"},\"rangeHours\":{\"type\":\"integer\"}}},\"mode\":{\"type\":\"string\",\"enum\":[\"RAW\",\"M4\"]},\"maxPoints\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":1000}}}");
    private static final ToolDefinition ANALYTICS_COMPUTE_STATS = insight(
            "analyticsComputeStats",
            "Analytics: mean, std dev, min, max and percentiles per series.",
            "{\"type\":\"object\",\"properties\":{\"series\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}},\"window\":{\"type\":\"object\"},\"percentiles\":{\"type\":\"array\",\"items\":{\"type\":\"number\"}}}}");
    private static final ToolDefinition ANALYTICS_COMPARE_PERIODS = insight(
            "analyticsComparePeriods",
            "Analytics: compare a current window against a baseline window per series.",
            "{\"type\":\"object\",\"properties\":{\"series\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}},\"current\":{\"type\":\"object\"},\"baseline\":{\"type\":\"object\"}}}");
    private static final ToolDefinition ANALYTICS_RANK_ENTITIES = insight(
            "analyticsRankEntities",
            "Analytics: rank devices, points or drivers by activity or aggregate metric.",
            "{\"type\":\"object\",\"properties\":{\"dimension\":{\"type\":\"string\",\"enum\":[\"DEVICE\",\"POINT\",\"DRIVER\"]},\"metric\":{\"type\":\"string\",\"enum\":[\"ACTIVITY\",\"MEAN\",\"MAX\",\"MIN\"]},\"window\":{\"type\":\"object\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50}}}");
    private static final ToolDefinition ANALYTICS_TREND_ANALYSIS = insight(
            "analyticsTrendAnalysis",
            "Analytics: per-series slope, first/last value and total change percent.",
            "{\"type\":\"object\",\"properties\":{\"series\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}},\"window\":{\"type\":\"object\"},\"buckets\":{\"type\":\"integer\",\"minimum\":2,\"maximum\":200}}}");
    private static final ToolDefinition ANALYTICS_THRESHOLD_REPORT = insight(
            "analyticsThresholdReport",
            "Analytics: exceedance report above or below a threshold per series.",
            "{\"type\":\"object\",\"properties\":{\"series\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}},\"window\":{\"type\":\"object\"},\"operator\":{\"type\":\"string\",\"enum\":[\"GREATER\",\"LESS\"]},\"threshold\":{\"type\":\"number\"}}}");
    private static final ToolDefinition ANALYTICS_CORRELATE = insight(
            "analyticsCorrelate",
            "Analytics: pearson correlation between two series.",
            "{\"type\":\"object\",\"properties\":{\"seriesA\":{\"type\":\"object\"},\"seriesB\":{\"type\":\"object\"},\"window\":{\"type\":\"object\"},\"alignBucketSeconds\":{\"type\":\"integer\",\"minimum\":10,\"maximum\":86400}}}");
    private static final ToolDefinition ANALYTICS_DATA_QUALITY_REPORT = insight(
            "analyticsDataQualityReport",
            "Analytics: data coverage and silent series report.",
            "{\"type\":\"object\",\"properties\":{\"window\":{\"type\":\"object\"},\"silentMinutes\":{\"type\":\"integer\",\"minimum\":5,\"maximum\":1440}}}");
    private static final ToolDefinition GET_POINT_COMMAND_STATUS = insight(
            "getPointCommandStatus",
            "Look up the execution status of a point command by the command identifier returned by readPointValue.",
            "{\"type\":\"object\",\"properties\":{\"commandId\":{\"type\":\"string\"}},\"required\":[\"commandId\"]}");

    private static final ToolDefinition LOOKUP_COMMAND_HISTORY = DefaultToolDefinition.builder()
            .name("lookupCommandHistoryByRecordId")
            .description("Look up one custom command execution record by its record ID without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"recordId\":{\"type\":\"string\"}},\"required\":[\"recordId\"]}")
            .build();
    private static final ToolDefinition SEARCH_COMMAND_HISTORIES = DefaultToolDefinition.builder()
            .name("searchCommandHistories")
            .description("Search custom command execution records with canonical offset pagination.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"deviceId\":{\"type\":\"integer\"},\"commandId\":{\"type\":\"integer\"},\"commandCode\":{\"type\":\"string\"},\"status\":{\"type\":\"string\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}")
            .build();
    private static final ToolDefinition LOOKUP_EVENT_HISTORY = DefaultToolDefinition.builder()
            .name("lookupEventHistoryByRecordId")
            .description("Look up one reported event record by its record ID without blocking.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"recordId\":{\"type\":\"string\"}},\"required\":[\"recordId\"]}")
            .build();
    private static final ToolDefinition SEARCH_EVENT_HISTORIES = DefaultToolDefinition.builder()
            .name("searchEventHistories")
            .description("Search reported event records with canonical offset pagination.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"deviceId\":{\"type\":\"integer\"},\"eventId\":{\"type\":\"integer\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}")
            .build();
    private static final ToolDefinition SEARCH_DRIVERS = DefaultToolDefinition.builder()
            .name("searchDrivers")
            .description("Search tenant-scoped drivers with canonical offset pagination.")
            .inputSchema(
                    "{\"type\":\"object\",\"properties\":{\"driverName\":{\"type\":\"string\"},\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":200}},\"required\":[\"offset\",\"limit\"]}")
            .build();

    private final PointValueTool pointValueTool;
    private final PointTool pointTool;
    private final ProfileTool profileTool;
    private final CommandTool commandTool;
    private final EventTool eventTool;
    private final DeviceTool deviceTool;
    private final DriverTool driverTool;
    private final InsightTool insightTool;
    private final HistoryTool historyTool;
    private final ObjectMapper objectMapper;

    /** reactive agentic tool registry. */
    public ReactiveAgenticToolRegistry(PointValueTool pointValueTool, ObjectMapper objectMapper) {
        this(pointValueTool, null, null, null, null, null, null, null, null, objectMapper);
    }

    /** reactive agentic tool registry. */
    public ReactiveAgenticToolRegistry(PointValueTool pointValueTool, PointTool pointTool, ObjectMapper objectMapper) {
        this(pointValueTool, pointTool, null, null, null, null, null, null, null, objectMapper);
    }

    /** reactive agentic tool registry. */
    public ReactiveAgenticToolRegistry(
            PointValueTool pointValueTool, PointTool pointTool, ProfileTool profileTool, ObjectMapper objectMapper) {
        this(pointValueTool, pointTool, profileTool, null, null, null, null, null, null, objectMapper);
    }

    /** Create the registry over the platform tool set. */
    @Autowired
    public ReactiveAgenticToolRegistry(
            PointValueTool pointValueTool,
            PointTool pointTool,
            ProfileTool profileTool,
            CommandTool commandTool,
            EventTool eventTool,
            DeviceTool deviceTool,
            DriverTool driverTool,
            InsightTool insightTool,
            HistoryTool historyTool,
            ObjectMapper objectMapper) {
        this.pointValueTool = pointValueTool;
        this.pointTool = pointTool;
        this.profileTool = profileTool;
        this.commandTool = commandTool;
        this.eventTool = eventTool;
        this.deviceTool = deviceTool;
        this.driverTool = driverTool;
        this.insightTool = insightTool;
        this.historyTool = historyTool;
        this.objectMapper = objectMapper;
    }

    /** Return the registered tools keyed by name. */
    public Map<String, ReactiveAgenticTool> tools() {
        if (objectMapper == null) {
            return Map.of();
        }
        Map<String, ReactiveAgenticTool> tools = new LinkedHashMap<>();
        if (pointValueTool != null) {
            tools.put("writePointValue", new ReactiveAgenticTool() {
                @Override
                public ToolDefinition definition() {
                    return WRITE_POINT_VALUE;
                }

                @Override
                public Mono<?> call(String arguments, ToolContext context) {
                    try {
                        Map<String, Object> values = objectMapper.readValue(arguments, Map.class);
                        Long deviceId = number(values.get("deviceId"));
                        Long pointId = number(values.get("pointId"));
                        String value = values.get("value") == null
                                ? null
                                : values.get("value").toString();
                        return pointValueTool.writePointValueReactive(deviceId, pointId, value, context);
                    } catch (RuntimeException error) {
                        return Mono.error(error);
                    }
                }

                private Long number(Object value) {
                    if (value instanceof Number number) {
                        return number.longValue();
                    }
                    return value == null ? null : Long.valueOf(value.toString());
                }
            });
            tools.put("readPointValue", new ReactiveAgenticTool() {
                @Override
                public ToolDefinition definition() {
                    return READ_POINT_VALUE;
                }

                @Override
                public Mono<?> call(String arguments, ToolContext context) {
                    try {
                        Map<String, Object> values = objectMapper.readValue(arguments, Map.class);
                        return pointValueTool.readPointValueReactive(
                                number(values.get("deviceId")), number(values.get("pointId")), context);
                    } catch (RuntimeException error) {
                        return Mono.error(error);
                    }
                }

                private Long number(Object value) {
                    if (value instanceof Number number) return number.longValue();
                    return value == null ? null : Long.valueOf(value.toString());
                }
            });
            tools.put("getLatestPointValue", new ReactiveAgenticTool() {
                @Override
                public ToolDefinition definition() {
                    return GET_LATEST_POINT_VALUE;
                }

                @Override
                public Mono<?> call(String arguments, ToolContext context) {
                    try {
                        Map<String, Object> values = objectMapper.readValue(arguments, Map.class);
                        return pointValueTool.getLatestPointValueReactive(
                                number(values.get("deviceId")), number(values.get("pointId")), context);
                    } catch (RuntimeException error) {
                        return Mono.error(error);
                    }
                }

                private Long number(Object value) {
                    return value instanceof Number n
                            ? n.longValue()
                            : value == null ? null : Long.valueOf(value.toString());
                }
            });
            tools.put("getPointValueHistory", new ReactiveAgenticTool() {
                @Override
                public ToolDefinition definition() {
                    return GET_POINT_VALUE_HISTORY;
                }

                @Override
                public Mono<?> call(String arguments, ToolContext context) {
                    try {
                        Map<String, Object> values = objectMapper.readValue(arguments, Map.class);
                        Object count = values.get("count");
                        int size = count instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(count));
                        return pointValueTool.getPointValueHistoryReactive(
                                number(values.get("deviceId")), number(values.get("pointId")), size, context);
                    } catch (RuntimeException error) {
                        return Mono.error(error);
                    }
                }

                private Long number(Object value) {
                    return value instanceof Number n
                            ? n.longValue()
                            : value == null ? null : Long.valueOf(value.toString());
                }
            });
        }
        if (pointTool != null) {
            tools.put(
                    "lookupPointById",
                    pointTool(
                            LOOKUP_POINT,
                            (values, context) ->
                                    pointTool.lookupPointByIdReactive(number(values.get("pointId")), context)));
            tools.put(
                    "lookupPointsByIds",
                    pointTool(
                            LOOKUP_POINTS,
                            (values, context) ->
                                    pointTool.lookupPointsByIdsReactive(numbers(values.get("pointIds")), context)));
            tools.put(
                    "searchPoints",
                    pointTool(
                            SEARCH_POINTS,
                            (values, context) -> pointTool.searchPointsReactive(
                                    string(values.get("pointName")),
                                    number(values.get("profileId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listPointsByDevice",
                    pointTool(
                            LIST_POINTS_BY_DEVICE,
                            (values, context) -> pointTool.listPointsByDeviceIdReactive(
                                    number(values.get("deviceId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listPointsByProfile",
                    pointTool(
                            LIST_POINTS_BY_PROFILE,
                            (values, context) -> pointTool.listPointsByProfileIdReactive(
                                    number(values.get("profileId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
        }
        if (profileTool != null) {
            tools.put(
                    "lookupProfileById",
                    profileTool(
                            LOOKUP_PROFILE,
                            (values, context) ->
                                    profileTool.lookupProfileByIdReactive(number(values.get("profileId")), context)));
            tools.put(
                    "lookupProfilesByIds",
                    profileTool(
                            LOOKUP_PROFILES,
                            (values, context) -> profileTool.lookupProfilesByIdsReactive(
                                    numbers(values.get("profileIds")), context)));
            tools.put(
                    "searchProfiles",
                    profileTool(
                            SEARCH_PROFILES,
                            (values, context) -> profileTool.searchProfilesReactive(
                                    string(values.get("profileName")),
                                    string(values.get("profileCode")),
                                    string(values.get("profileType")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listProfilesByDeviceId",
                    profileTool(
                            LIST_PROFILES_BY_DEVICE,
                            (values, context) -> profileTool.listProfilesByDeviceIdReactive(
                                    number(values.get("deviceId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
        }
        if (commandTool != null) {
            tools.put(
                    "lookupCommandById",
                    commandToolTool(
                            LOOKUP_COMMAND,
                            (values, context) ->
                                    commandTool.lookupCommandByIdReactive(number(values.get("commandId")), context)));
            tools.put(
                    "lookupCommandsByIds",
                    commandToolTool(
                            LOOKUP_COMMANDS,
                            (values, context) -> commandTool.lookupCommandsByIdsReactive(
                                    numbers(values.get("commandIds")), context)));
            tools.put(
                    "searchCommands",
                    commandToolTool(
                            SEARCH_COMMANDS,
                            (values, context) -> commandTool.searchCommandsReactive(
                                    string(values.get("commandName")),
                                    number(values.get("profileId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listCommandsByDeviceId",
                    commandToolTool(
                            LIST_COMMANDS_BY_DEVICE,
                            (values, context) -> commandTool.listCommandsByDeviceIdReactive(
                                    number(values.get("deviceId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listCommandsByProfileId",
                    commandToolTool(
                            LIST_COMMANDS_BY_PROFILE,
                            (values, context) -> commandTool.listCommandsByProfileIdReactive(
                                    number(values.get("profileId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
        }
        if (eventTool != null) {
            tools.put(
                    "lookupEventById",
                    eventToolTool(
                            LOOKUP_EVENT,
                            (values, context) ->
                                    eventTool.lookupEventByIdReactive(number(values.get("eventId")), context)));
            tools.put(
                    "lookupEventsByIds",
                    eventToolTool(
                            LOOKUP_EVENTS,
                            (values, context) ->
                                    eventTool.lookupEventsByIdsReactive(numbers(values.get("eventIds")), context)));
            tools.put(
                    "searchEvents",
                    eventToolTool(
                            SEARCH_EVENTS,
                            (values, context) -> eventTool.searchEventsReactive(
                                    string(values.get("eventName")),
                                    number(values.get("profileId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listEventsByDeviceId",
                    eventToolTool(
                            LIST_EVENTS_BY_DEVICE,
                            (values, context) -> eventTool.listEventsByDeviceIdReactive(
                                    number(values.get("deviceId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listEventsByProfileId",
                    eventToolTool(
                            LIST_EVENTS_BY_PROFILE,
                            (values, context) -> eventTool.listEventsByProfileIdReactive(
                                    number(values.get("profileId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
        }
        if (deviceTool != null) {
            tools.put(
                    "lookupDeviceById",
                    deviceToolTool(
                            LOOKUP_DEVICE,
                            (values, context) ->
                                    deviceTool.lookupDeviceByIdReactive(number(values.get("deviceId")), context)));
            tools.put(
                    "lookupDevicesByIds",
                    deviceToolTool(
                            LOOKUP_DEVICES,
                            (values, context) ->
                                    deviceTool.lookupDevicesByIdsReactive(numbers(values.get("deviceIds")), context)));
            tools.put(
                    "searchDevices",
                    deviceToolTool(
                            SEARCH_DEVICES,
                            (values, context) -> deviceTool.searchDevicesReactive(
                                    string(values.get("deviceName")),
                                    string(values.get("deviceCode")),
                                    number(values.get("driverId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listDevicesByDriverId",
                    deviceToolTool(
                            LIST_DEVICES_BY_DRIVER,
                            (values, context) -> deviceTool.listDevicesByDriverIdReactive(
                                    number(values.get("driverId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "listDevicesByProfileId",
                    deviceToolTool(
                            LIST_DEVICES_BY_PROFILE,
                            (values, context) -> deviceTool.listDevicesByProfileIdReactive(
                                    number(values.get("profileId")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "getDeviceLatestPointValues",
                    deviceToolTool(
                            DEVICE_LATEST_VALUES,
                            (values, context) -> deviceTool.getDeviceLatestPointValuesReactive(
                                    number(values.get("deviceId")), intValue(values.get("limit")), context)));
        }
        if (driverTool != null) {
            tools.put(
                    "lookupDriverById",
                    driverToolTool(
                            LOOKUP_DRIVER,
                            (values, context) ->
                                    driverTool.lookupDriverByIdReactive(number(values.get("driverId")), context)));
            tools.put(
                    "lookupDriversByIds",
                    driverToolTool(
                            LOOKUP_DRIVERS,
                            (values, context) ->
                                    driverTool.lookupDriversByIdsReactive(numbers(values.get("driverIds")), context)));
            tools.put(
                    "searchDrivers",
                    driverToolTool(
                            SEARCH_DRIVERS,
                            (values, context) -> driverTool.searchDriversReactive(
                                    string(values.get("driverName")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
        }
        if (insightTool != null) {
            tools.put(
                    "getAlertStats",
                    insightToolDef(GET_ALERT_STATS, (values, context) -> insightTool.getAlertStats(context)));
            tools.put(
                    "getLatestAlerts",
                    insightToolDef(
                            GET_LATEST_ALERTS,
                            (values, context) -> insightTool.getLatestAlerts(intOrNull(values.get("limit")), context)));
            tools.put(
                    "pageAlerts",
                    insightToolDef(
                            PAGE_ALERTS,
                            (values, context) -> insightTool.pageAlerts(
                                    string(values.get("source")),
                                    number(values.get("sourceId")),
                                    intOrNull(values.get("alarmTypeFlag")),
                                    intOrNull(values.get("confirmFlag")),
                                    string(values.get("rangeKey")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "getAlertTrend",
                    insightToolDef(
                            GET_ALERT_TREND,
                            (values, context) -> insightTool.getAlertTrend(intOrNull(values.get("days")), context)));
            tools.put(
                    "getAlertTopSources",
                    insightToolDef(
                            GET_ALERT_TOP_SOURCES,
                            (values, context) -> insightTool.getAlertTopSources(
                                    intOrNull(values.get("days")), intOrNull(values.get("limit")), context)));
            tools.put(
                    "getAlertTypeDistribution",
                    insightToolDef(
                            GET_ALERT_TYPE_DISTRIBUTION,
                            (values, context) ->
                                    insightTool.getAlertTypeDistribution(intOrNull(values.get("days")), context)));
            tools.put(
                    "getAlertAgingBacklog",
                    insightToolDef(
                            GET_ALERT_AGING_BACKLOG, (values, context) -> insightTool.getAlertAgingBacklog(context)));
            tools.put(
                    "getAlertMtta",
                    insightToolDef(
                            GET_ALERT_MTTA,
                            (values, context) -> insightTool.getAlertMtta(intOrNull(values.get("days")), context)));
            tools.put(
                    "getTodayStats",
                    insightToolDef(GET_TODAY_STATS, (values, context) -> insightTool.getTodayStats(context)));
            tools.put(
                    "getStatsTimeseries",
                    insightToolDef(
                            GET_STATS_TIMESERIES,
                            (values, context) -> insightTool.getStatsTimeseries(
                                    string(values.get("granularity")), intOrNull(values.get("rangeHours")), context)));
            tools.put(
                    "getTopEntities",
                    insightToolDef(
                            GET_TOP_ENTITIES,
                            (values, context) -> insightTool.getTopEntities(
                                    string(values.get("dimension")),
                                    intOrNull(values.get("rangeHours")),
                                    intOrNull(values.get("limit")),
                                    context)));
            tools.put(
                    "getLatestValueStream",
                    insightToolDef(
                            GET_LATEST_VALUE_STREAM,
                            (values, context) ->
                                    insightTool.getLatestValueStream(intOrNull(values.get("limit")), context)));
            tools.put(
                    "getLatencyHistogram",
                    insightToolDef(
                            GET_LATENCY_HISTOGRAM,
                            (values, context) ->
                                    insightTool.getLatencyHistogram(intOrNull(values.get("rangeHours")), context)));
            tools.put(
                    "getHourlyActivity",
                    insightToolDef(
                            GET_HOURLY_ACTIVITY,
                            (values, context) ->
                                    insightTool.getHourlyActivity(intOrNull(values.get("rangeHours")), context)));
            tools.put(
                    "getProtocolHealth",
                    insightToolDef(GET_PROTOCOL_HEALTH, (values, context) -> insightTool.getProtocolHealth(context)));
            tools.put(
                    "analyticsQueryLatest",
                    insightToolDef(
                            ANALYTICS_QUERY_LATEST,
                            (values, context) -> insightTool.analyticsQueryLatest(values, context)));
            tools.put(
                    "analyticsQueryHistory",
                    insightToolDef(
                            ANALYTICS_QUERY_HISTORY,
                            (values, context) -> insightTool.analyticsQueryHistory(values, context)));
            tools.put(
                    "analyticsComputeStats",
                    insightToolDef(
                            ANALYTICS_COMPUTE_STATS,
                            (values, context) -> insightTool.analyticsComputeStats(values, context)));
            tools.put(
                    "analyticsComparePeriods",
                    insightToolDef(
                            ANALYTICS_COMPARE_PERIODS,
                            (values, context) -> insightTool.analyticsComparePeriods(values, context)));
            tools.put(
                    "analyticsRankEntities",
                    insightToolDef(
                            ANALYTICS_RANK_ENTITIES,
                            (values, context) -> insightTool.analyticsRankEntities(values, context)));
            tools.put(
                    "analyticsTrendAnalysis",
                    insightToolDef(
                            ANALYTICS_TREND_ANALYSIS,
                            (values, context) -> insightTool.analyticsTrendAnalysis(values, context)));
            tools.put(
                    "analyticsThresholdReport",
                    insightToolDef(
                            ANALYTICS_THRESHOLD_REPORT,
                            (values, context) -> insightTool.analyticsThresholdReport(values, context)));
            tools.put(
                    "analyticsCorrelate",
                    insightToolDef(
                            ANALYTICS_CORRELATE, (values, context) -> insightTool.analyticsCorrelate(values, context)));
            tools.put(
                    "analyticsDataQualityReport",
                    insightToolDef(
                            ANALYTICS_DATA_QUALITY_REPORT,
                            (values, context) -> insightTool.analyticsDataQualityReport(values, context)));
            tools.put(
                    "getPointCommandStatus",
                    insightToolDef(
                            GET_POINT_COMMAND_STATUS,
                            (values, context) ->
                                    insightTool.getPointCommandStatus(string(values.get("commandId")), context)));
        }
        if (historyTool != null) {
            tools.put(
                    "lookupCommandHistoryByRecordId",
                    historyToolDef(
                            LOOKUP_COMMAND_HISTORY,
                            (values, context) -> historyTool.lookupCommandHistoryByRecordId(
                                    string(values.get("recordId")), context)));
            tools.put(
                    "searchCommandHistories",
                    historyToolDef(
                            SEARCH_COMMAND_HISTORIES,
                            (values, context) -> historyTool.searchCommandHistories(
                                    number(values.get("deviceId")) == null
                                            ? null
                                            : values.get("deviceId").toString(),
                                    number(values.get("commandId")) == null
                                            ? null
                                            : values.get("commandId").toString(),
                                    string(values.get("commandCode")),
                                    string(values.get("status")),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
            tools.put(
                    "lookupEventHistoryByRecordId",
                    historyToolDef(
                            LOOKUP_EVENT_HISTORY,
                            (values, context) ->
                                    historyTool.lookupEventHistoryByRecordId(string(values.get("recordId")), context)));
            tools.put(
                    "searchEventHistories",
                    historyToolDef(
                            SEARCH_EVENT_HISTORIES,
                            (values, context) -> historyTool.searchEventHistories(
                                    number(values.get("deviceId")) == null
                                            ? null
                                            : values.get("deviceId").toString(),
                                    number(values.get("eventId")) == null
                                            ? null
                                            : values.get("eventId").toString(),
                                    longValue(values.get("offset")),
                                    intValue(values.get("limit")),
                                    context)));
        }
        tools.replaceAll((name, tool) -> new ReactiveAgenticToolTracing(name, tool));
        return Map.copyOf(tools);
    }

    @FunctionalInterface
    private interface PointCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface
    private interface ProfileCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface
    private interface CommandCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface
    private interface EventCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface
    private interface DeviceCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface
    private interface DriverCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface
    private interface InsightCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    @FunctionalInterface
    private interface HistoryCall {
        Mono<?> invoke(Map<String, Object> values, ToolContext context);
    }

    private ReactiveAgenticTool commandToolTool(ToolDefinition definition, CommandCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private ReactiveAgenticTool eventToolTool(ToolDefinition definition, EventCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private ReactiveAgenticTool deviceToolTool(ToolDefinition definition, DeviceCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private ReactiveAgenticTool driverToolTool(ToolDefinition definition, DriverCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private ReactiveAgenticTool insightToolDef(ToolDefinition definition, InsightCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private ReactiveAgenticTool historyToolDef(ToolDefinition definition, HistoryCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private ReactiveAgenticTool profileTool(ToolDefinition definition, ProfileCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private ReactiveAgenticTool pointTool(ToolDefinition definition, PointCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                try {
                    return call.invoke(objectMapper.readValue(arguments, Map.class), context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                }
            }
        };
    }

    private static Long number(Object value) {
        return value instanceof Number number
                ? number.longValue()
                : value == null ? null : Long.valueOf(value.toString());
    }

    private static long longValue(Object value) {
        return number(value) == null ? 0L : number(value);
    }

    private static int intValue(Object value) {
        return number(value) == null ? 0 : number(value).intValue();
    }

    private static Integer intOrNull(Object value) {
        return number(value) == null ? null : number(value).intValue();
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static java.util.List<Long> numbers(Object value) {
        if (!(value instanceof java.util.List<?> values)) return java.util.List.of();
        return values.stream()
                .map(ReactiveAgenticToolRegistry::number)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}

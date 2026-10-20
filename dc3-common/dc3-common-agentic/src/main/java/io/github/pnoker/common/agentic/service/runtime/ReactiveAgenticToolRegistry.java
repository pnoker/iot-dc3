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
import io.github.pnoker.common.constant.service.AgenticConstant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * Single source of truth for the agentic tool surface. Every tool — reactive platform
 * tools and the tenant/user/system lookups alike — is registered here once; both agent
 * runtimes consume this registry (the explicit OpenAI-compatible loop directly, the
 * Spring AI client path through {@link ReactiveAgenticToolCallback} bridges).
 *
 * <p>The snapshot is built once at construction time and is immutable afterwards; tool
 * implementations are stateless across calls, so sharing one traced instance per tool
 * is safe and keeps per-request rebuild churn off the hot path.</p>
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
public class ReactiveAgenticToolRegistry {

    // ===== Schema fragments (numeric bounds bound to constants, no magic literals) =====

    /** Upper bound of the canonical page size, aligned with the platform page request. */
    private static final int PAGE_LIMIT_MAX = 200;

    private static final String PAGE_PROPS =
            "\"offset\":{\"type\":\"integer\",\"minimum\":0},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":"
                    + PAGE_LIMIT_MAX + "}";

    private static final String IDS_MAX = String.valueOf(AgenticConstant.ToolLimit.MAX_IDS);

    /** Series selector item, mirroring {@code AnalyticsModel.SeriesSelector}. */
    private static final String SERIES_ITEM =
            "{\"type\":\"object\",\"properties\":{\"deviceId\":{\"type\":\"integer\"},\"pointId\":{\"type\":\"integer\"}"
                    + ",\"deviceName\":{\"type\":\"string\"},\"pointName\":{\"type\":\"string\"}}}";

    /** Time window, mirroring {@code AnalyticsModel.TimeRange}. */
    private static final String WINDOW =
            "{\"type\":\"object\",\"properties\":{\"fromIso\":{\"type\":\"string\"},\"toIso\":{\"type\":\"string\"}"
                    + ",\"rangeHours\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":2160}}}";

    private static ToolDefinition toolDef(String name, String description, String props, String required) {
        return DefaultToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema("{\"type\":\"object\",\"properties\":"
                        + (props == null || props.isEmpty() ? "{}" : "{" + props + "}")
                        + (required == null ? "" : ",\"required\":[" + required + "]") + "}")
                .build();
    }

    private static ToolDefinition insight(String name, String description, String schema) {
        return DefaultToolDefinition.builder().name(name).description(description).inputSchema(schema).build();
    }

    private static String pageProps(String leading) {
        return (leading == null || leading.isEmpty() ? "" : leading + ",") + PAGE_PROPS;
    }

    // ===== Point-value tools =====

    private static final ToolDefinition WRITE_POINT_VALUE = toolDef(
            "writePointValue",
            "Prepare a point write command for explicit user confirmation.",
            "\"deviceId\":{\"type\":\"integer\"},\"pointId\":{\"type\":\"integer\"},\"value\":{\"type\":\"string\"}",
            "\"deviceId\",\"pointId\",\"value\"");
    private static final ToolDefinition READ_POINT_VALUE = toolDef(
            "readPointValue",
            "Send a point read command and return its command identifier.",
            "\"deviceId\":{\"type\":\"integer\"},\"pointId\":{\"type\":\"integer\"}",
            "\"deviceId\",\"pointId\"");
    private static final ToolDefinition GET_LATEST_POINT_VALUE = toolDef(
            "getLatestPointValue",
            "Get the latest value for a device point without blocking the agent runtime.",
            "\"deviceId\":{\"type\":\"integer\"},\"pointId\":{\"type\":\"integer\"}",
            "\"deviceId\",\"pointId\"");
    private static final ToolDefinition GET_POINT_VALUE_HISTORY = toolDef(
            "getPointValueHistory",
            "Get recent point values and chart-ready numeric summaries without blocking the agent runtime.",
            "\"deviceId\":{\"type\":\"integer\"},\"pointId\":{\"type\":\"integer\"},\"count\":{\"type\":\"integer\",\"minimum\":1}",
            "\"deviceId\",\"pointId\",\"count\"");

    // ===== Point tools =====

    private static final ToolDefinition SEARCH_POINTS = toolDef(
            "searchPoints",
            "Search tenant-scoped points with canonical offset pagination without blocking the agent runtime.",
            pageProps("\"pointName\":{\"type\":\"string\"},\"profileId\":{\"type\":\"integer\"}"),
            "\"offset\",\"limit\"");
    private static final ToolDefinition LIST_POINTS_BY_DEVICE = toolDef(
            "listPointsByDevice",
            "List points bound to a device with canonical offset pagination without blocking the agent runtime.",
            pageProps("\"deviceId\":{\"type\":\"integer\"}"),
            "\"deviceId\",\"offset\",\"limit\"");
    private static final ToolDefinition LIST_POINTS_BY_PROFILE = toolDef(
            "listPointsByProfile",
            "List points under a profile with canonical offset pagination without blocking the agent runtime.",
            pageProps("\"profileId\":{\"type\":\"integer\"}"),
            "\"profileId\",\"offset\",\"limit\"");
    private static final ToolDefinition LOOKUP_POINT = toolDef(
            "lookupPointById",
            "Look up one tenant-scoped point without blocking the agent runtime.",
            "\"pointId\":{\"type\":\"integer\"}",
            "\"pointId\"");
    private static final ToolDefinition LOOKUP_POINTS = toolDef(
            "lookupPointsByIds",
            "Look up tenant-scoped points by IDs without blocking the agent runtime.",
            "\"pointIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":" + IDS_MAX + "}",
            "\"pointIds\"");

    // ===== Profile tools =====

    private static final ToolDefinition SEARCH_PROFILES = toolDef(
            "searchProfiles",
            "Search tenant-scoped profiles with canonical offset pagination without blocking the agent runtime.",
            pageProps(
                    "\"profileName\":{\"type\":\"string\"},\"profileCode\":{\"type\":\"string\"},\"profileType\":{\"type\":\"string\"}"),
            "\"offset\",\"limit\"");
    private static final ToolDefinition LOOKUP_PROFILE = toolDef(
            "lookupProfileById",
            "Look up one tenant-scoped profile without blocking the agent runtime.",
            "\"profileId\":{\"type\":\"integer\"}",
            "\"profileId\"");
    private static final ToolDefinition LOOKUP_PROFILES = toolDef(
            "lookupProfilesByIds",
            "Look up tenant-scoped profiles by IDs without blocking the agent runtime.",
            "\"profileIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":" + IDS_MAX + "}",
            "\"profileIds\"");
    private static final ToolDefinition LIST_PROFILES_BY_DEVICE = toolDef(
            "listProfilesByDeviceId",
            "List profiles bound to a device with canonical offset pagination without blocking the agent runtime.",
            pageProps("\"deviceId\":{\"type\":\"integer\"}"),
            "\"deviceId\",\"offset\",\"limit\"");

    // ===== Command tools =====

    private static final ToolDefinition SEARCH_COMMANDS = toolDef(
            "searchCommands",
            "Search tenant-scoped commands with canonical offset pagination.",
            pageProps("\"commandName\":{\"type\":\"string\"},\"profileId\":{\"type\":\"integer\"}"),
            "\"offset\",\"limit\"");
    private static final ToolDefinition LOOKUP_COMMAND = toolDef(
            "lookupCommandById",
            "Look up one tenant-scoped command without blocking.",
            "\"commandId\":{\"type\":\"integer\"}",
            "\"commandId\"");
    private static final ToolDefinition LOOKUP_COMMANDS = toolDef(
            "lookupCommandsByIds",
            "Look up tenant-scoped commands by IDs without blocking.",
            "\"commandIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":" + IDS_MAX + "}",
            "\"commandIds\"");
    private static final ToolDefinition LIST_COMMANDS_BY_DEVICE = toolDef(
            "listCommandsByDeviceId",
            "List commands bound to a device with canonical offset pagination without blocking.",
            pageProps("\"deviceId\":{\"type\":\"integer\"}"),
            "\"deviceId\",\"offset\",\"limit\"");
    private static final ToolDefinition LIST_COMMANDS_BY_PROFILE = toolDef(
            "listCommandsByProfileId",
            "List commands under a profile with canonical offset pagination without blocking.",
            pageProps("\"profileId\":{\"type\":\"integer\"}"),
            "\"profileId\",\"offset\",\"limit\"");

    // ===== Event tools =====

    private static final ToolDefinition SEARCH_EVENTS = toolDef(
            "searchEvents",
            "Search tenant-scoped events with canonical offset pagination.",
            pageProps("\"eventName\":{\"type\":\"string\"},\"profileId\":{\"type\":\"integer\"}"),
            "\"offset\",\"limit\"");
    private static final ToolDefinition LOOKUP_EVENT = toolDef(
            "lookupEventById",
            "Look up one tenant-scoped event without blocking.",
            "\"eventId\":{\"type\":\"integer\"}",
            "\"eventId\"");
    private static final ToolDefinition LOOKUP_EVENTS = toolDef(
            "lookupEventsByIds",
            "Look up tenant-scoped events by IDs without blocking.",
            "\"eventIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":" + IDS_MAX + "}",
            "\"eventIds\"");
    private static final ToolDefinition LIST_EVENTS_BY_DEVICE = toolDef(
            "listEventsByDeviceId",
            "List events bound to a device with canonical offset pagination without blocking.",
            pageProps("\"deviceId\":{\"type\":\"integer\"}"),
            "\"deviceId\",\"offset\",\"limit\"");
    private static final ToolDefinition LIST_EVENTS_BY_PROFILE = toolDef(
            "listEventsByProfileId",
            "List events under a profile with canonical offset pagination without blocking.",
            pageProps("\"profileId\":{\"type\":\"integer\"}"),
            "\"profileId\",\"offset\",\"limit\"");

    // ===== Device tools =====

    private static final ToolDefinition DEVICE_LATEST_VALUES = toolDef(
            "getDeviceLatestPointValues",
            "Load a device latest-point snapshot without blocking.",
            "\"deviceId\":{\"type\":\"integer\"},\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50}",
            "\"deviceId\",\"limit\"");
    private static final ToolDefinition LOOKUP_DEVICE = toolDef(
            "lookupDeviceById",
            "Look up one tenant-scoped device without blocking.",
            "\"deviceId\":{\"type\":\"integer\"}",
            "\"deviceId\"");
    private static final ToolDefinition LOOKUP_DEVICES = toolDef(
            "lookupDevicesByIds",
            "Look up tenant-scoped devices by IDs without blocking.",
            "\"deviceIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":" + IDS_MAX + "}",
            "\"deviceIds\"");
    private static final ToolDefinition SEARCH_DEVICES = toolDef(
            "searchDevices",
            "Search tenant-scoped devices with canonical offset pagination.",
            pageProps("\"deviceName\":{\"type\":\"string\"},\"deviceCode\":{\"type\":\"string\"},\"driverId\":{\"type\":\"integer\"}"),
            "\"offset\",\"limit\"");
    private static final ToolDefinition LIST_DEVICES_BY_DRIVER = toolDef(
            "listDevicesByDriverId",
            "List devices for a driver with canonical offset pagination without blocking.",
            pageProps("\"driverId\":{\"type\":\"integer\"}"),
            "\"driverId\",\"offset\",\"limit\"");
    private static final ToolDefinition LIST_DEVICES_BY_PROFILE = toolDef(
            "listDevicesByProfileId",
            "List devices for a profile with canonical offset pagination without blocking.",
            pageProps("\"profileId\":{\"type\":\"integer\"}"),
            "\"profileId\",\"offset\",\"limit\"");

    // ===== Driver tools =====

    private static final ToolDefinition LOOKUP_DRIVER = toolDef(
            "lookupDriverById",
            "Look up one tenant-scoped driver without blocking.",
            "\"driverId\":{\"type\":\"integer\"}",
            "\"driverId\"");
    private static final ToolDefinition LOOKUP_DRIVERS = toolDef(
            "lookupDriversByIds",
            "Look up tenant-scoped drivers by IDs without blocking.",
            "\"driverIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":" + IDS_MAX + "}",
            "\"driverIds\"");
    private static final ToolDefinition SEARCH_DRIVERS = toolDef(
            "searchDrivers",
            "Search tenant-scoped drivers with canonical offset pagination.",
            pageProps("\"driverName\":{\"type\":\"string\"}"),
            "\"offset\",\"limit\"");

    // ===== Insight tools =====

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
            "{\"type\":\"object\",\"properties\":{\"source\":{\"type\":\"string\",\"enum\":[\"device\",\"driver\"]},\"sourceId\":{\"type\":\"integer\"},\"alarmTypeFlag\":{\"type\":\"integer\"},\"confirmFlag\":{\"type\":\"integer\"},\"rangeKey\":{\"type\":\"string\",\"enum\":[\"today\",\"24h\",\"7d\",\"30d\"]},"
                    + PAGE_PROPS + "},\"required\":[\"offset\",\"limit\"]}");
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

    // ===== Analytics tools (schemas mirror the AnalyticsModel request records) =====

    private static final String SERIES_ARRAY =
            "\"series\":{\"type\":\"array\",\"items\":" + SERIES_ITEM + ",\"maxItems\":20}";

    private static final ToolDefinition ANALYTICS_QUERY_LATEST = insight(
            "analyticsQueryLatest",
            "Analytics: latest values for up to 20 series.",
            "{\"type\":\"object\",\"properties\":{" + SERIES_ARRAY + "},\"required\":[\"series\"]}");
    private static final ToolDefinition ANALYTICS_QUERY_HISTORY = insight(
            "analyticsQueryHistory",
            "Analytics: RAW or M4 downsampled history per series.",
            "{\"type\":\"object\",\"properties\":{" + SERIES_ARRAY + ",\"window\":" + WINDOW
                    + ",\"mode\":{\"type\":\"string\",\"enum\":[\"RAW\",\"M4\"]},\"maxPoints\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":1000}}}");
    private static final ToolDefinition ANALYTICS_COMPUTE_STATS = insight(
            "analyticsComputeStats",
            "Analytics: mean, std dev, min, max and percentiles per series.",
            "{\"type\":\"object\",\"properties\":{" + SERIES_ARRAY + ",\"window\":" + WINDOW
                    + ",\"percentiles\":{\"type\":\"array\",\"items\":{\"type\":\"number\",\"minimum\":0,\"maximum\":1}}}}");
    private static final ToolDefinition ANALYTICS_COMPARE_PERIODS = insight(
            "analyticsComparePeriods",
            "Analytics: compare a current window against a baseline window per series.",
            "{\"type\":\"object\",\"properties\":{" + SERIES_ARRAY + ",\"current\":" + WINDOW + ",\"baseline\":"
                    + WINDOW + "}}");
    private static final ToolDefinition ANALYTICS_RANK_ENTITIES = insight(
            "analyticsRankEntities",
            "Analytics: rank devices, points or drivers by activity or aggregate metric.",
            "{\"type\":\"object\",\"properties\":{\"dimension\":{\"type\":\"string\",\"enum\":[\"DEVICE\",\"POINT\",\"DRIVER\"]},\"metric\":{\"type\":\"string\",\"enum\":[\"ACTIVITY\",\"MEAN\",\"MAX\",\"MIN\"]},\"window\":"
                    + WINDOW + ",\"limit\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":50}}}");
    private static final ToolDefinition ANALYTICS_TREND_ANALYSIS = insight(
            "analyticsTrendAnalysis",
            "Analytics: per-series slope, first/last value and total change percent.",
            "{\"type\":\"object\",\"properties\":{" + SERIES_ARRAY + ",\"window\":" + WINDOW
                    + ",\"buckets\":{\"type\":\"integer\",\"minimum\":2,\"maximum\":200}}}");
    private static final ToolDefinition ANALYTICS_THRESHOLD_REPORT = insight(
            "analyticsThresholdReport",
            "Analytics: exceedance report above or below a threshold per series.",
            "{\"type\":\"object\",\"properties\":{" + SERIES_ARRAY + ",\"window\":" + WINDOW
                    + ",\"operator\":{\"type\":\"string\",\"enum\":[\"GREATER\",\"LESS\"]},\"threshold\":{\"type\":\"number\"}}}");
    private static final ToolDefinition ANALYTICS_CORRELATE = insight(
            "analyticsCorrelate",
            "Analytics: pearson correlation between two series.",
            "{\"type\":\"object\",\"properties\":{\"seriesA\":" + SERIES_ITEM + ",\"seriesB\":" + SERIES_ITEM
                    + ",\"window\":" + WINDOW
                    + ",\"alignBucketSeconds\":{\"type\":\"integer\",\"minimum\":10,\"maximum\":86400}}}");
    private static final ToolDefinition ANALYTICS_DATA_QUALITY_REPORT = insight(
            "analyticsDataQualityReport",
            "Analytics: data coverage and silent series report.",
            "{\"type\":\"object\",\"properties\":{\"window\":" + WINDOW
                    + ",\"silentMinutes\":{\"type\":\"integer\",\"minimum\":5,\"maximum\":1440}}}");
    private static final ToolDefinition GET_POINT_COMMAND_STATUS = insight(
            "getPointCommandStatus",
            "Look up the execution status of a point command by the command identifier returned by readPointValue.",
            "{\"type\":\"object\",\"properties\":{\"commandId\":{\"type\":\"string\"}},\"required\":[\"commandId\"]}");

    // ===== History tools =====

    private static final ToolDefinition LOOKUP_COMMAND_HISTORY = toolDef(
            "lookupCommandHistoryByRecordId",
            "Look up one custom command execution record by its record ID without blocking.",
            "\"recordId\":{\"type\":\"string\"}",
            "\"recordId\"");
    private static final ToolDefinition SEARCH_COMMAND_HISTORIES = toolDef(
            "searchCommandHistories",
            "Search custom command execution records with canonical offset pagination.",
            pageProps("\"deviceId\":{\"type\":\"integer\"},\"commandId\":{\"type\":\"integer\"},\"commandCode\":{\"type\":\"string\"},\"status\":{\"type\":\"string\"}"),
            "\"offset\",\"limit\"");
    private static final ToolDefinition LOOKUP_EVENT_HISTORY = toolDef(
            "lookupEventHistoryByRecordId",
            "Look up one reported event record by its record ID without blocking.",
            "\"recordId\":{\"type\":\"string\"}",
            "\"recordId\"");
    private static final ToolDefinition SEARCH_EVENT_HISTORIES = toolDef(
            "searchEventHistories",
            "Search reported event records with canonical offset pagination.",
            pageProps("\"deviceId\":{\"type\":\"integer\"},\"eventId\":{\"type\":\"integer\"}"),
            "\"offset\",\"limit\"");

    // ===== Tenant / user / system tools (formerly the reflective @Tool channel) =====

    private static final ToolDefinition GET_CURRENT_TENANT_INFO = toolDef(
            "getCurrentTenantInfo",
            "Get the current tenant context. Returns only the current tenant ID.",
            null,
            null);
    private static final ToolDefinition GET_CURRENT_USER_PROFILE = toolDef(
            "getCurrentUserProfile",
            "Get the current user profile. Returns only user ID, username, and nickname.",
            null,
            null);
    private static final ToolDefinition GET_SYSTEM_HEALTH = toolDef(
            "getSystemHealth",
            "Get a system health snapshot: center services, infrastructure, driver fleet, and device fleet.",
            null,
            null);
    private static final ToolDefinition GET_DEVICE_STATUSES = toolDef(
            "getDeviceStatuses",
            "Get the online/offline status of devices by their IDs.",
            "\"deviceIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":" + IDS_MAX + "}",
            "\"deviceIds\"");
    private static final ToolDefinition GET_DEVICE_STATUSES_BY_PROFILE = toolDef(
            "getDeviceStatusesByProfile",
            "Get the online/offline status of all devices under a profile.",
            "\"profileId\":{\"type\":\"integer\"}",
            "\"profileId\"");
    private static final ToolDefinition GET_DRIVER_STATUSES = toolDef(
            "getDriverStatuses",
            "Get the online/offline status of drivers by their IDs.",
            "\"driverIds\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"maxItems\":" + IDS_MAX + "}",
            "\"driverIds\"");
    private static final ToolDefinition GET_DRIVER_DEVICE_STATUS_SUMMARY = toolDef(
            "getDriverDeviceStatusSummary",
            "Get the online/offline device status summary for one driver: totals per status.",
            "\"driverId\":{\"type\":\"integer\"}",
            "\"driverId\"");

    private final PointValueTool pointValueTool;
    private final PointTool pointTool;
    private final ProfileTool profileTool;
    private final CommandTool commandTool;
    private final EventTool eventTool;
    private final DeviceTool deviceTool;
    private final DriverTool driverTool;
    private final InsightTool insightTool;
    private final HistoryTool historyTool;
    private final TenantTool tenantTool;
    private final UserTool userTool;
    private final SystemTool systemTool;

    /** Immutable traced snapshot, built once at construction. */
    private final Map<String, ReactiveAgenticTool> toolsSnapshot;

    /** Create the registry over the full platform tool set. */
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
            TenantTool tenantTool,
            UserTool userTool,
            SystemTool systemTool,
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
        this.tenantTool = tenantTool;
        this.userTool = userTool;
        this.systemTool = systemTool;
        this.toolsSnapshot = buildSnapshot(objectMapper);
    }

    /** Return the registered tools keyed by name; the same immutable instance on every call. */
    public Map<String, ReactiveAgenticTool> tools() {
        return toolsSnapshot;
    }

    private Map<String, ReactiveAgenticTool> buildSnapshot(ObjectMapper mapper) {
        if (mapper == null) {
            return Map.of();
        }
        Map<String, ReactiveAgenticTool> tools = new LinkedHashMap<>();
        registerPointValueTools(tools, mapper);
        registerPointTools(tools, mapper);
        registerProfileTools(tools, mapper);
        registerCommandTools(tools, mapper);
        registerEventTools(tools, mapper);
        registerDeviceTools(tools, mapper);
        registerDriverTools(tools, mapper);
        registerInsightTools(tools, mapper);
        registerHistoryTools(tools, mapper);
        registerContextTools(tools, mapper);
        Map<String, ReactiveAgenticTool> traced = new LinkedHashMap<>();
        tools.forEach((name, tool) -> traced.put(name, new ReactiveAgenticToolTracing(name, tool)));
        return java.util.Collections.unmodifiableMap(traced);
    }

    /** Single tool call contract: arguments arrive pre-parsed as a value map. */
    @FunctionalInterface
    public interface ToolCall {

        /**
         * Invoke the tool with the parsed argument values.
         *
         * @param values  argument map parsed from the model's JSON
         * @param context tool context carrying tenant/user identity
         * @return the tool result publisher
         * @throws Exception when argument coercion or invocation fails before publishing
         */
        Mono<?> invoke(Map<String, Object> values, ToolContext context) throws Exception;
    }

    /**
     * Single registration factory: parses the model's JSON arguments once, coerces values
     * through the shared helpers, and turns argument-level failures into
     * {@link AgenticToolResult#invalid} envelopes the model can self-correct from —
     * instead of blowing up the agent loop.
     */
    private static ReactiveAgenticTool tool(ToolDefinition definition, String domain, ObjectMapper mapper, ToolCall call) {
        return new ReactiveAgenticTool() {
            @Override
            public ToolDefinition definition() {
                return definition;
            }

            @Override
            public String domain() {
                return domain;
            }

            @Override
            public Mono<?> call(String arguments, ToolContext context) {
                Map<String, Object> values;
                try {
                    values = mapper.readValue(arguments, Map.class);
                } catch (RuntimeException error) {
                    return Mono.just(AgenticToolResult.invalid(
                            "Invalid arguments JSON for tool '" + definition.name() + "': " + error.getMessage()));
                }
                try {
                    return call.invoke(values, context);
                } catch (RuntimeException error) {
                    return Mono.error(error);
                } catch (Exception error) {
                    // Checked coercion failures from tool lambdas are tool-level errors;
                    // the tracing wrapper degrades them into error envelopes.
                    return Mono.error(error);
                }
            }
        };
    }

    // ===== registration sections =====

    private void registerPointValueTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (pointValueTool == null) {
            return;
        }
        tools.put(
                "writePointValue",
                tool(WRITE_POINT_VALUE, "pointValue", mapper, (values, context) -> pointValueTool
                        .writePointValueReactive(
                                number(values.get("deviceId")),
                                number(values.get("pointId")),
                                string(values.get("value")),
                                context)));
        tools.put(
                "readPointValue",
                tool(READ_POINT_VALUE, "pointValue", mapper, (values, context) -> pointValueTool
                        .readPointValueReactive(
                                number(values.get("deviceId")), number(values.get("pointId")), context)));
        tools.put(
                "getLatestPointValue",
                tool(GET_LATEST_POINT_VALUE, "pointValue", mapper, (values, context) -> pointValueTool
                        .getLatestPointValueReactive(
                                number(values.get("deviceId")), number(values.get("pointId")), context)));
        tools.put(
                "getPointValueHistory",
                tool(GET_POINT_VALUE_HISTORY, "pointValue", mapper, (values, context) -> pointValueTool
                        .getPointValueHistoryReactive(
                                number(values.get("deviceId")),
                                number(values.get("pointId")),
                                intValue(values.get("count")),
                                context)));
    }

    private void registerPointTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (pointTool == null) {
            return;
        }
        tools.put(
                "lookupPointById",
                tool(LOOKUP_POINT, "point", mapper, (values, context) ->
                        pointTool.lookupPointByIdReactive(number(values.get("pointId")), context)));
        tools.put(
                "lookupPointsByIds",
                tool(LOOKUP_POINTS, "point", mapper, (values, context) ->
                        pointTool.lookupPointsByIdsReactive(numbers(values.get("pointIds")), context)));
        tools.put(
                "searchPoints",
                tool(SEARCH_POINTS, "point", mapper, (values, context) -> pointTool.searchPointsReactive(
                        string(values.get("pointName")),
                        number(values.get("profileId")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
        tools.put(
                "listPointsByDevice",
                tool(LIST_POINTS_BY_DEVICE, "point", mapper, (values, context) -> pointTool.listPointsByDeviceIdReactive(
                        number(values.get("deviceId")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
        tools.put(
                "listPointsByProfile",
                tool(LIST_POINTS_BY_PROFILE, "point", mapper, (values, context) -> pointTool
                        .listPointsByProfileIdReactive(
                                number(values.get("profileId")),
                                longValue(values.get("offset")),
                                intValue(values.get("limit")),
                                context)));
    }

    private void registerProfileTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (profileTool == null) {
            return;
        }
        tools.put(
                "lookupProfileById",
                tool(LOOKUP_PROFILE, "profile", mapper, (values, context) ->
                        profileTool.lookupProfileByIdReactive(number(values.get("profileId")), context)));
        tools.put(
                "lookupProfilesByIds",
                tool(LOOKUP_PROFILES, "profile", mapper, (values, context) -> profileTool
                        .lookupProfilesByIdsReactive(numbers(values.get("profileIds")), context)));
        tools.put(
                "searchProfiles",
                tool(SEARCH_PROFILES, "profile", mapper, (values, context) -> profileTool.searchProfilesReactive(
                        string(values.get("profileName")),
                        string(values.get("profileCode")),
                        string(values.get("profileType")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
        tools.put(
                "listProfilesByDeviceId",
                tool(LIST_PROFILES_BY_DEVICE, "profile", mapper, (values, context) -> profileTool
                        .listProfilesByDeviceIdReactive(
                                number(values.get("deviceId")),
                                longValue(values.get("offset")),
                                intValue(values.get("limit")),
                                context)));
    }

    private void registerCommandTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (commandTool == null) {
            return;
        }
        tools.put(
                "lookupCommandById",
                tool(LOOKUP_COMMAND, "command", mapper, (values, context) ->
                        commandTool.lookupCommandByIdReactive(number(values.get("commandId")), context)));
        tools.put(
                "lookupCommandsByIds",
                tool(LOOKUP_COMMANDS, "command", mapper, (values, context) -> commandTool
                        .lookupCommandsByIdsReactive(numbers(values.get("commandIds")), context)));
        tools.put(
                "searchCommands",
                tool(SEARCH_COMMANDS, "command", mapper, (values, context) -> commandTool.searchCommandsReactive(
                        string(values.get("commandName")),
                        number(values.get("profileId")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
        tools.put(
                "listCommandsByDeviceId",
                tool(LIST_COMMANDS_BY_DEVICE, "command", mapper, (values, context) -> commandTool
                        .listCommandsByDeviceIdReactive(
                                number(values.get("deviceId")),
                                longValue(values.get("offset")),
                                intValue(values.get("limit")),
                                context)));
        tools.put(
                "listCommandsByProfileId",
                tool(LIST_COMMANDS_BY_PROFILE, "command", mapper, (values, context) -> commandTool
                        .listCommandsByProfileIdReactive(
                                number(values.get("profileId")),
                                longValue(values.get("offset")),
                                intValue(values.get("limit")),
                                context)));
    }

    private void registerEventTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (eventTool == null) {
            return;
        }
        tools.put(
                "lookupEventById",
                tool(LOOKUP_EVENT, "event", mapper, (values, context) ->
                        eventTool.lookupEventByIdReactive(number(values.get("eventId")), context)));
        tools.put(
                "lookupEventsByIds",
                tool(LOOKUP_EVENTS, "event", mapper, (values, context) ->
                        eventTool.lookupEventsByIdsReactive(numbers(values.get("eventIds")), context)));
        tools.put(
                "searchEvents",
                tool(SEARCH_EVENTS, "event", mapper, (values, context) -> eventTool.searchEventsReactive(
                        string(values.get("eventName")),
                        number(values.get("profileId")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
        tools.put(
                "listEventsByDeviceId",
                tool(LIST_EVENTS_BY_DEVICE, "event", mapper, (values, context) -> eventTool.listEventsByDeviceIdReactive(
                        number(values.get("deviceId")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
        tools.put(
                "listEventsByProfileId",
                tool(LIST_EVENTS_BY_PROFILE, "event", mapper, (values, context) -> eventTool
                        .listEventsByProfileIdReactive(
                                number(values.get("profileId")),
                                longValue(values.get("offset")),
                                intValue(values.get("limit")),
                                context)));
    }

    private void registerDeviceTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (deviceTool == null) {
            return;
        }
        tools.put(
                "lookupDeviceById",
                tool(LOOKUP_DEVICE, "device", mapper, (values, context) ->
                        deviceTool.lookupDeviceByIdReactive(number(values.get("deviceId")), context)));
        tools.put(
                "lookupDevicesByIds",
                tool(LOOKUP_DEVICES, "device", mapper, (values, context) ->
                        deviceTool.lookupDevicesByIdsReactive(numbers(values.get("deviceIds")), context)));
        tools.put(
                "searchDevices",
                tool(SEARCH_DEVICES, "device", mapper, (values, context) -> deviceTool.searchDevicesReactive(
                        string(values.get("deviceName")),
                        string(values.get("deviceCode")),
                        number(values.get("driverId")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
        tools.put(
                "listDevicesByDriverId",
                tool(LIST_DEVICES_BY_DRIVER, "device", mapper, (values, context) -> deviceTool
                        .listDevicesByDriverIdReactive(
                                number(values.get("driverId")),
                                longValue(values.get("offset")),
                                intValue(values.get("limit")),
                                context)));
        tools.put(
                "listDevicesByProfileId",
                tool(LIST_DEVICES_BY_PROFILE, "device", mapper, (values, context) -> deviceTool
                        .listDevicesByProfileIdReactive(
                                number(values.get("profileId")),
                                longValue(values.get("offset")),
                                intValue(values.get("limit")),
                                context)));
        tools.put(
                "getDeviceLatestPointValues",
                tool(DEVICE_LATEST_VALUES, "device", mapper, (values, context) -> deviceTool
                        .getDeviceLatestPointValuesReactive(
                                number(values.get("deviceId")), intValue(values.get("limit")), context)));
    }

    private void registerDriverTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (driverTool == null) {
            return;
        }
        tools.put(
                "lookupDriverById",
                tool(LOOKUP_DRIVER, "driver", mapper, (values, context) ->
                        driverTool.lookupDriverByIdReactive(number(values.get("driverId")), context)));
        tools.put(
                "lookupDriversByIds",
                tool(LOOKUP_DRIVERS, "driver", mapper, (values, context) ->
                        driverTool.lookupDriversByIdsReactive(numbers(values.get("driverIds")), context)));
        tools.put(
                "searchDrivers",
                tool(SEARCH_DRIVERS, "driver", mapper, (values, context) -> driverTool.searchDriversReactive(
                        string(values.get("driverName")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
    }

    private void registerInsightTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (insightTool == null) {
            return;
        }
        tools.put("getAlertStats", tool(GET_ALERT_STATS, "insight", mapper, (values, context) -> insightTool.getAlertStats(context)));
        tools.put(
                "getLatestAlerts",
                tool(GET_LATEST_ALERTS, "insight", mapper, (values, context) ->
                        insightTool.getLatestAlerts(intOrNull(values.get("limit")), context)));
        tools.put(
                "pageAlerts",
                tool(PAGE_ALERTS, "insight", mapper, (values, context) -> insightTool.pageAlerts(
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
                tool(GET_ALERT_TREND, "insight", mapper, (values, context) ->
                        insightTool.getAlertTrend(intOrNull(values.get("days")), context)));
        tools.put(
                "getAlertTopSources",
                tool(GET_ALERT_TOP_SOURCES, "insight", mapper, (values, context) -> insightTool.getAlertTopSources(
                        intOrNull(values.get("days")), intOrNull(values.get("limit")), context)));
        tools.put(
                "getAlertTypeDistribution",
                tool(GET_ALERT_TYPE_DISTRIBUTION, "insight", mapper, (values, context) ->
                        insightTool.getAlertTypeDistribution(intOrNull(values.get("days")), context)));
        tools.put(
                "getAlertAgingBacklog",
                tool(GET_ALERT_AGING_BACKLOG, "insight", mapper, (values, context) -> insightTool.getAlertAgingBacklog(context)));
        tools.put(
                "getAlertMtta",
                tool(GET_ALERT_MTTA, "insight", mapper, (values, context) ->
                        insightTool.getAlertMtta(intOrNull(values.get("days")), context)));
        tools.put(
                "getTodayStats",
                tool(GET_TODAY_STATS, "insight", mapper, (values, context) -> insightTool.getTodayStats(context)));
        tools.put(
                "getStatsTimeseries",
                tool(GET_STATS_TIMESERIES, "insight", mapper, (values, context) -> insightTool.getStatsTimeseries(
                        string(values.get("granularity")), intOrNull(values.get("rangeHours")), context)));
        tools.put(
                "getTopEntities",
                tool(GET_TOP_ENTITIES, "insight", mapper, (values, context) -> insightTool.getTopEntities(
                        string(values.get("dimension")),
                        intOrNull(values.get("rangeHours")),
                        intOrNull(values.get("limit")),
                        context)));
        tools.put(
                "getLatestValueStream",
                tool(GET_LATEST_VALUE_STREAM, "insight", mapper, (values, context) ->
                        insightTool.getLatestValueStream(intOrNull(values.get("limit")), context)));
        tools.put(
                "getLatencyHistogram",
                tool(GET_LATENCY_HISTOGRAM, "insight", mapper, (values, context) ->
                        insightTool.getLatencyHistogram(intOrNull(values.get("rangeHours")), context)));
        tools.put(
                "getHourlyActivity",
                tool(GET_HOURLY_ACTIVITY, "insight", mapper, (values, context) ->
                        insightTool.getHourlyActivity(intOrNull(values.get("rangeHours")), context)));
        tools.put(
                "getProtocolHealth",
                tool(GET_PROTOCOL_HEALTH, "insight", mapper, (values, context) -> insightTool.getProtocolHealth(context)));
        tools.put(
                "analyticsQueryLatest",
                tool(ANALYTICS_QUERY_LATEST, "insight", mapper, (values, context) -> insightTool.analyticsQueryLatest(values, context)));
        tools.put(
                "analyticsQueryHistory",
                tool(ANALYTICS_QUERY_HISTORY, "insight", mapper, (values, context) -> insightTool.analyticsQueryHistory(values, context)));
        tools.put(
                "analyticsComputeStats",
                tool(ANALYTICS_COMPUTE_STATS, "insight", mapper, (values, context) -> insightTool.analyticsComputeStats(values, context)));
        tools.put(
                "analyticsComparePeriods",
                tool(ANALYTICS_COMPARE_PERIODS, "insight", mapper, (values, context) -> insightTool.analyticsComparePeriods(values, context)));
        tools.put(
                "analyticsRankEntities",
                tool(ANALYTICS_RANK_ENTITIES, "insight", mapper, (values, context) -> insightTool.analyticsRankEntities(values, context)));
        tools.put(
                "analyticsTrendAnalysis",
                tool(ANALYTICS_TREND_ANALYSIS, "insight", mapper, (values, context) -> insightTool.analyticsTrendAnalysis(values, context)));
        tools.put(
                "analyticsThresholdReport",
                tool(ANALYTICS_THRESHOLD_REPORT, "insight", mapper, (values, context) -> insightTool.analyticsThresholdReport(values, context)));
        tools.put(
                "analyticsCorrelate",
                tool(ANALYTICS_CORRELATE, "insight", mapper, (values, context) -> insightTool.analyticsCorrelate(values, context)));
        tools.put(
                "analyticsDataQualityReport",
                tool(ANALYTICS_DATA_QUALITY_REPORT, "insight", mapper, (values, context) -> insightTool.analyticsDataQualityReport(values, context)));
        tools.put(
                "getPointCommandStatus",
                tool(GET_POINT_COMMAND_STATUS, "insight", mapper, (values, context) ->
                        insightTool.getPointCommandStatus(string(values.get("commandId")), context)));
    }

    private void registerHistoryTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (historyTool == null) {
            return;
        }
        tools.put(
                "lookupCommandHistoryByRecordId",
                tool(LOOKUP_COMMAND_HISTORY, "history", mapper, (values, context) -> historyTool
                        .lookupCommandHistoryByRecordId(string(values.get("recordId")), context)));
        tools.put(
                "searchCommandHistories",
                tool(SEARCH_COMMAND_HISTORIES, "history", mapper, (values, context) -> historyTool.searchCommandHistories(
                        string(values.get("deviceId")),
                        string(values.get("commandId")),
                        string(values.get("commandCode")),
                        string(values.get("status")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
        tools.put(
                "lookupEventHistoryByRecordId",
                tool(LOOKUP_EVENT_HISTORY, "history", mapper, (values, context) -> historyTool
                        .lookupEventHistoryByRecordId(string(values.get("recordId")), context)));
        tools.put(
                "searchEventHistories",
                tool(SEARCH_EVENT_HISTORIES, "history", mapper, (values, context) -> historyTool.searchEventHistories(
                        string(values.get("deviceId")),
                        string(values.get("eventId")),
                        longValue(values.get("offset")),
                        intValue(values.get("limit")),
                        context)));
    }

    private void registerContextTools(Map<String, ReactiveAgenticTool> tools, ObjectMapper mapper) {
        if (tenantTool != null) {
            tools.put(
                    "getCurrentTenantInfo",
                    tool(GET_CURRENT_TENANT_INFO, "tenant", mapper, (values, context) ->
                            Mono.just(tenantTool.getCurrentTenantInfo(context))));
        }
        if (userTool != null) {
            tools.put(
                    "getCurrentUserProfile",
                    tool(GET_CURRENT_USER_PROFILE, "user", mapper, (values, context) ->
                            Mono.just(userTool.getCurrentUserProfile(context))));
        }
        if (systemTool != null) {
            tools.put(
                    "getSystemHealth",
                    tool(GET_SYSTEM_HEALTH, "system", mapper, (values, context) ->
                            Mono.just(systemTool.getSystemHealth(context))));
            tools.put(
                    "getDeviceStatuses",
                    tool(GET_DEVICE_STATUSES, "system", mapper, (values, context) -> {
                        List<Long> requested = numbers(values.get("deviceIds"));
                        List<Long> bounded = boundedIds(requested);
                        return Mono.just(systemTool.getDeviceStatuses(bounded, context))
                                .map(result -> withTruncationNote(result, requested));
                    }));
            tools.put(
                    "getDeviceStatusesByProfile",
                    tool(GET_DEVICE_STATUSES_BY_PROFILE, "system", mapper, (values, context) -> Mono.just(
                            systemTool.getDeviceStatusesByProfile(number(values.get("profileId")), context))));
            tools.put(
                    "getDriverStatuses",
                    tool(GET_DRIVER_STATUSES, "system", mapper, (values, context) -> {
                        List<Long> requested = numbers(values.get("driverIds"));
                        List<Long> bounded = boundedIds(requested);
                        return Mono.just(systemTool.getDriverStatuses(bounded, context))
                                .map(result -> withTruncationNote(result, requested));
                    }));
            tools.put(
                    "getDriverDeviceStatusSummary",
                    tool(GET_DRIVER_DEVICE_STATUS_SUMMARY, "system", mapper, (values, context) -> Mono.just(
                            systemTool.getDriverDeviceStatusSummary(number(values.get("driverId")), context))));
        }
    }

    /** Cap an id list at {@link AgenticConstant.ToolLimit#MAX_IDS}, keeping order. */
    private static List<Long> boundedIds(List<Long> ids) {
        return ids.size() <= AgenticConstant.ToolLimit.MAX_IDS
                ? ids
                : ids.subList(0, AgenticConstant.ToolLimit.MAX_IDS);
    }

    /** Annotate a tool result when its input id list had to be truncated. */
    private static <T> AgenticToolResult<T> withTruncationNote(AgenticToolResult<T> result, List<Long> requested) {
        if (requested.size() <= AgenticConstant.ToolLimit.MAX_IDS || !result.success()) {
            return result;
        }
        return AgenticToolResult.ok(
                result.message() + " (queried the first " + AgenticConstant.ToolLimit.MAX_IDS + " of "
                        + requested.size() + " requested IDs)",
                result.data(),
                result.visualizations());
    }

    // ===== shared argument coercion =====

    /**
     * Tolerant integer coercion for LLM-produced values: accepts JSON numbers, integer
     * strings and exponent forms ({@code 10.0}, {@code 1e3}); rejects non-integral
     * values ({@code 10.7}) and unparsable strings by returning {@code null}.
     */
    static Long number(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number && isIntegral(number)) {
            return number.longValue();
        }
        try {
            return new java.math.BigDecimal(value.toString()).longValueExact();
        } catch (NumberFormatException | ArithmeticException error) {
            return null;
        }
    }

    private static boolean isIntegral(Number number) {
        double asDouble = number.doubleValue();
        return asDouble == Math.rint(asDouble) && !Double.isInfinite(asDouble);
    }

    private static long longValue(Object value) {
        Long parsed = number(value);
        return parsed == null ? 0L : parsed;
    }

    private static int intValue(Object value) {
        Long parsed = number(value);
        return parsed == null ? 0 : parsed.intValue();
    }

    private static Integer intOrNull(Object value) {
        Long parsed = number(value);
        return parsed == null ? null : parsed.intValue();
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static List<Long> numbers(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(ReactiveAgenticToolRegistry::number)
                .filter(Objects::nonNull)
                .toList();
    }
}

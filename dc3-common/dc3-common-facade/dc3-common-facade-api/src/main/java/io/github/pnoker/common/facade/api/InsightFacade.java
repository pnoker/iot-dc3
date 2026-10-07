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
package io.github.pnoker.common.facade.api;

import reactor.core.publisher.Mono;

/**
 * Protocol-neutral insight facade for agent-facing reads (alert analytics, dashboard stats,
 * analytics queries and point command status lookups). Requests and responses carry JSON
 * payloads so the wire contract stays stable while operations evolve on both ends.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface InsightFacade {

    /**
     * Invoke one insight operation by name with a JSON request payload and return the JSON
     * response payload. Unknown operations fail with {@link IllegalArgumentException}.
     *
     * @param tenantId tenant id
     * @param operation one of the {@link Operations} names
     * @param requestJson JSON-encoded request payload, empty string for no-argument operations
     * @return JSON-encoded response payload mono
     */
    Mono<String> invokeReactive(Long tenantId, String operation, String requestJson);

    /**
     * Canonical insight operation names. The data center routes these to the underlying
     * dashboard, analytics and point-command services.
     */
    interface Operations {

        // ---- alert analytics ----
        String ALERT_STATS = "alert_stats";
        String ALERT_LATEST = "alert_latest";
        String ALERT_PAGE = "alert_page";
        String ALERT_TREND = "alert_trend";
        String ALERT_TOP_SOURCES = "alert_top_sources";
        String ALERT_TYPE_DISTRIBUTION = "alert_type_distribution";
        String ALERT_AGING = "alert_aging";
        String ALERT_MTTA = "alert_mtta";

        // ---- dashboard stats ----
        String STATS_TODAY = "stats_today";
        String STATS_TIMESERIES = "stats_timeseries";
        String STATS_TOP = "stats_top";
        String STATS_LATEST_STREAM = "stats_latest_stream";
        String STATS_LATENCY = "stats_latency";
        String STATS_ACTIVITY = "stats_activity";
        String PROTOCOL_HEALTH = "protocol_health";

        // ---- analytics (agent surface) ----
        String QUERY_LATEST = "query_latest";
        String QUERY_HISTORY = "query_history";
        String COMPUTE_STATS = "compute_stats";
        String COMPARE_PERIODS = "compare_periods";
        String RANK_ENTITIES = "rank_entities";
        String TREND_ANALYSIS = "trend_analysis";
        String THRESHOLD_REPORT = "threshold_report";
        String CORRELATE = "correlate";
        String DATA_QUALITY_REPORT = "data_quality_report";

        // ---- point command closed loop ----
        String POINT_COMMAND_STATUS = "point_command_status";
    }
}

/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.data.constant;

/**
 * Numeric + string constants that used to live inline as {@code Math.max(1,
 * Math.min(..., N))} ceilings and {@code "device"}/{@code "driver"} literal
 * comparisons in {@code DashboardServiceImpl}. Centralised here so tuning
 * the caps (e.g. raising the stream default from 100 to 200 for one busy
 * tenant) is a single-line change, and so tests / controller can reference
 * the same values.
 *
 * <p>Grouped by concern: generic clamps at the top, insight-specific caps
 * below, whitelist sets at the bottom.</p>
 *
 * @author pnoker
 * @since 2026.5.4
 */
public final class DashboardLimits {

    /**
     * Lookback ceiling for daily-trend style queries.
     */
    public static final int MAX_DAYS = 90;

    // ===== Generic clamp ceilings ==============================================
    /**
     * Default Top-N limit cap for ranking endpoints.
     */
    public static final int MAX_LIMIT = 50;
    /**
     * Hard cap on page size for the alert list (avoids accidental DOS via size=999999).
     */
    public static final long MAX_PAGE_SIZE = 200L;
    /**
     * Cap on live-stream / alert-latest batch sizes.
     */
    public static final int MAX_LIVE_SIZE = 100;
    /**
     * 7-day window expressed in hours.
     */
    public static final int MAX_HOURS_7D = 24 * 7;
    /**
     * 30-day window expressed in hours.
     */
    public static final int MAX_HOURS_30D = 24 * 30;
    /**
     * 90-day window expressed in hours.
     */
    public static final int MAX_HOURS_90D = 24 * 90;
    /**
     * Silent threshold clamped to at most 24h (one day of silence).
     */
    public static final int MAX_SILENT_MINUTES = 24 * 60;

    // ===== Insight-specific caps ==============================================
    /**
     * Baseline window for the silent-sources check (how far to look for "was active").
     */
    public static final int MAX_BASELINE_DAYS = 30;
    /**
     * Peer-deviation lookback.
     */
    public static final int MAX_PEER_DAYS = 30;
    /**
     * Correlation edge cap — payload stays manageable on a force-layout graph.
     */
    public static final int MAX_CORRELATION_PAIRS = 30;
    /**
     * Flapping endpoint — count must be at least 2 to be flapping.
     */
    public static final int MIN_FLAPPING_COUNT = 2;
    /**
     * Correlation co-occurrence window in seconds — clamped bounds.
     */
    public static final int MIN_CORRELATION_WINDOW_SEC = 5;
    public static final int MAX_CORRELATION_WINDOW_SEC = 300;
    /**
     * Storm / coverage-gap list limit upper bound (higher than MAX_LIMIT because
     * coverage-gap can legitimately have hundreds of offenders).
     */
    public static final int MAX_COVERAGE_GAP_LIMIT = 200;
    public static final String SOURCE_DEVICE = "device";

    // ===== Whitelist values =================================================
    public static final String SOURCE_DRIVER = "driver";

    private DashboardLimits() {
        // utility class — never instantiated
    }
}

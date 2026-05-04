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

package io.github.pnoker.common.manager.constant;

/**
 * Hoisted numeric + string constants for the topology dashboard Sankey.
 * Previously lived as {@code private static final} inside
 * {@code DashboardServiceImpl}; promoted to a package-level class so
 * tuning the Top-N caps, cache TTL, or mode/range literals is one place
 * to look, and so controller + tests can share the same symbols.
 *
 * @author pnoker
 * @since 2026.5.4
 */
public final class TopologyLimits {

    public static final int TOP_DRIVERS = 10;

    // ===== Top-N caps ==============================================
    // Orders of magnitude: worst-case nodes ≈
    //   TOP_DRIVERS + TOP_DEVICES + |bound profiles| +
    //   (TOP_POINTS_PER_PROFILE × |bound profiles|) + Others
    //   ≈ 10 + 20 + ~30 + 15×30 + ~20 ≈ 530 (G2 sankey comfortable).
    public static final int TOP_DEVICES = 20;
    public static final int TOP_POINTS_PER_PROFILE = 15;
    public static final String MODE_VOLUME = "volume";

    // ===== Modes ==================================================
    public static final String MODE_CARDINALITY = "cardinality";
    public static final String RANGE_TODAY = "today";

    // ===== Range keys =============================================
    public static final String RANGE_24H = "24h";
    public static final String RANGE_7D = "7d";
    public static final String RANGE_30D = "30d";
    public static final String RANGE_DEFAULT = RANGE_7D;
    /**
     * Short TTL — wide enough to coalesce refresh bursts, narrow enough that
     * metadata edits surface on the next natural poll.
     */
    public static final int CACHE_TTL_SECONDS = 60;

    // ===== Cache config ===========================================
    /**
     * Cap on cache entries = (tenant × mode × rangeKey) triples held.
     */
    public static final int CACHE_MAX_SIZE = 200;

    // ===== Fallback bucket keys ==================================
    // Returned when an enum lookup fails (EnableFlagEnum.ofIndex(null) → null,
    // or unknown driver_type_flag int). Frontend matches this on a localised
    // label lookup — it's a stable dictionary token, not user-visible copy.
    public static final String UNKNOWN_BUCKET = "UNKNOWN";

    private TopologyLimits() {
        // utility class — never instantiated
    }
}

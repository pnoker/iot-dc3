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

package io.github.pnoker.common.manager.constant;

/**
 * Hoisted numeric + string constants for the topology dashboard Sankey. Previously lived
 * as {@code private static final} inside {@code DashboardServiceImpl}; promoted to a
 * package-level class so tuning the Top-N caps, cache TTL, or mode/range literals is one
 * place to look, and so controller + tests can share the same symbols.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.4
 */
public final class TopologyLimits {

    public static final int TOP_DRIVERS = 10;

    // ===== Top-N caps ==============================================
    // Orders of magnitude: worst-case nodes ≈
    // TOP_DRIVERS + TOP_DEVICES + |bound profiles| +
    // (TOP_POINTS_PER_PROFILE × |bound profiles|) + Others
    // ≈ 10 + 20 + ~30 + 15×30 + ~20 ≈ 530 (G2 sankey comfortable).
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
     * Short TTL — wide enough to coalesce refresh bursts, narrow enough that metadata
     * edits surface on the next natural poll.
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

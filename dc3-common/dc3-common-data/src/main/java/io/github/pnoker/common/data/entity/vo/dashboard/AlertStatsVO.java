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

package io.github.pnoker.common.data.entity.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Aggregate alert counters for the home page alert card.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Aggregate alert counters for the home page alert card")
public class AlertStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "total alert count", example = "142")
    private long total;

    @Schema(description = "unconfirmed alert count", example = "23")
    private long unconfirmed;

    @Schema(description = "device alert count", example = "98")
    private long deviceAlerts;

    @Schema(description = "driver alert count", example = "44")
    private long driverAlerts;

    @Schema(description = "unconfirmed device alert count", example = "15")
    private long deviceUnconfirmed;

    @Schema(description = "unconfirmed driver alert count", example = "8")
    private long driverUnconfirmed;

    @Schema(description = "todayDeviceAlarms count", example = "12")
    private long todayDeviceAlarms;

    @Schema(description = "todayDriverAlarms count", example = "5")
    private long todayDriverAlarms;

    @Schema(description = "todayDeviceUnconfirmed count", example = "3")
    private long todayDeviceUnconfirmed;

    @Schema(description = "todayDriverUnconfirmed count", example = "1")
    private long todayDriverUnconfirmed;

    @Schema(description = "alert counts bucketed by type")
    private List<BucketVO> byType;

    /**
     * 24-element hourly count series for the alert indicator card sparkline, oldest
     * first. Always length 24 — service zero-pads missing hours.
     */
    @Schema(description = "24-element hourly count series for the sparkline, oldest first, always length 24")
    private List<Long> sparkline24h;

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @Schema(description = "Alert count for a single type bucket")
    public static class BucketVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "bucket key (alarm type)", example = "driver-offline")
        private String key;

        @Schema(description = "alert count in this bucket", example = "42")
        private long count;

    }

}

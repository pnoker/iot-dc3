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
public class AlertStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long total;

    private long unconfirmed;

    private long deviceAlerts;

    private long driverAlerts;

    private long deviceUnconfirmed;

    private long driverUnconfirmed;

    private long todayDeviceAlarms;

    private long todayDriverAlarms;

    private long todayDeviceUnconfirmed;

    private long todayDriverUnconfirmed;

    private List<BucketVO> byType;

    /**
     * 24-element hourly count series for the alert indicator card sparkline, oldest
     * first. Always length 24 — service zero-pads missing hours.
     */
    private List<Long> sparkline24h;

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    public static class BucketVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String key;

        private long count;

    }

}

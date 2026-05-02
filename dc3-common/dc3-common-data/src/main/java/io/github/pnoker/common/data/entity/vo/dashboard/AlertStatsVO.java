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
     * 24-element hourly count series for the alert indicator card sparkline,
     * oldest first. Always length 24 — service zero-pads missing hours.
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

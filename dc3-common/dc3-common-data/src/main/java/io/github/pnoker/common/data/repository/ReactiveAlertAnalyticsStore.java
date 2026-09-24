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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.bo.dashboard.ActivityCellRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AgingBucketRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AlertCountersRow;
import io.github.pnoker.common.data.entity.bo.dashboard.AlertTrendRow;
import io.github.pnoker.common.data.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.data.entity.bo.dashboard.CorrelationPairRow;
import io.github.pnoker.common.data.entity.bo.dashboard.FlappingRow;
import io.github.pnoker.common.data.entity.bo.dashboard.HourCountRow;
import io.github.pnoker.common.data.entity.bo.dashboard.MttaTrendRow;
import io.github.pnoker.common.data.entity.bo.dashboard.PeerAlarmRow;
import io.github.pnoker.common.data.entity.bo.dashboard.PointAlertDailyRow;
import io.github.pnoker.common.data.entity.bo.dashboard.ProtocolHealthRow;
import io.github.pnoker.common.data.entity.bo.dashboard.RecentChangeRow;
import io.github.pnoker.common.data.entity.bo.dashboard.SourceCountRow;
import io.github.pnoker.common.data.entity.bo.dashboard.SourceStatsRow;
import java.time.LocalDateTime;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive, tenant-scoped aggregate reads for dashboard alerts. */
public interface ReactiveAlertAnalyticsStore {

    /** Emit the tenant's headline alert counters. */
    Mono<AlertCountersRow> countAll(long tenantId);

    /**
     * Point-scoped variant of {@link #typeDistribution(long, LocalDateTime)}:
     * alarm-type buckets of one point, counts descending.
     *
     * @param tenantId tenant scope
     * @param pointId  point scope (alarm_target_type_flag=0 rows only)
     * @param from     inclusive lower bound of the window
     * @return alarm-type buckets for the point
     */
    Flux<BucketRow> typeDistributionForPoint(long tenantId, long pointId, LocalDateTime from);

    /**
     * Point-scoped daily alarm counts, days ascending.
     *
     * @param tenantId tenant scope
     * @param pointId  point scope (alarm_target_type_flag=0 rows only)
     * @param from     inclusive lower bound of the window
     * @return one row per day with alarms
     */
    Flux<PointAlertDailyRow> dailyTrendForPoint(long tenantId, long pointId, LocalDateTime from);

    /** Emit alert counts grouped by type. */
    Flux<BucketRow> countByType(long tenantId);

    /** Emit alert counts grouped by source. */
    Flux<SourceStatsRow> countBySource(long tenantId);

    /** Emit per-hour alert counts since the given time. */
    Flux<HourCountRow> hourlyCounts(long tenantId, LocalDateTime from);

    /** Emit today's alert counts grouped by source. */
    Flux<SourceStatsRow> todayBySource(long tenantId, LocalDateTime from);

    /** Emit the daily alert trend since the given time. */
    Flux<AlertTrendRow> dailyTrend(long tenantId, LocalDateTime from);

    /** Emit the daily alert trend of one device since the given time. */
    Flux<AlertTrendRow> dailyTrendForDevice(long tenantId, long deviceId, LocalDateTime from);

    /** Emit the noisiest alert sources since the given time. */
    Flux<SourceCountRow> topSources(long tenantId, LocalDateTime from, int limit);

    /** Emit heatmap activity cells for alert bursts since the given time. */
    Flux<ActivityCellRow> activityHeatmap(long tenantId, LocalDateTime from);

    /** Emit the alert type distribution since the given time. */
    Flux<BucketRow> typeDistribution(long tenantId, LocalDateTime from);

    /** Emit alert-storm sources above the minimum count since the given time. */
    Flux<SourceCountRow> stormSources(long tenantId, LocalDateTime from, int minCount, int limit);

    /** Emit flapping sources above the minimum count since the given time. */
    Flux<FlappingRow> flappingSources(long tenantId, LocalDateTime from, int minCount, int limit);

    /** Emit correlated alarm pairs inside the time window since the given time. */
    Flux<CorrelationPairRow> correlationPairs(long tenantId, LocalDateTime from, int windowSec, int limit);

    /** Emit peer alarm counts for cross-entity correlation. */
    Flux<PeerAlarmRow> peerAlarmCounts(long tenantId, LocalDateTime from);

    /** Emit open-alert aging buckets for the tenant. */
    Mono<AgingBucketRow> agingBuckets(long tenantId);

    /** Emit the mean-time-to-acknowledge trend per day since the given time. */
    Flux<MttaTrendRow> mttaByDay(long tenantId, LocalDateTime from);

    /** Emit per-protocol health rows for the tenant. */
    Flux<ProtocolHealthRow> protocolHealth(long tenantId);

    /** Emit the most recent alert state changes since the given time. */
    Flux<RecentChangeRow> recentChanges(long tenantId, LocalDateTime from, int limit);
}

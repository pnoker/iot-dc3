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

package io.github.pnoker.common.data.mapper;

import io.github.pnoker.common.data.entity.bo.dashboard.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Alert aggregate queries over the unified {@code dc3_entity_alarm} table.
 * Returns typed Row DTOs — see {@code entity/bo/dashboard/} — with MyBatis snake→camel
 * mapping.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Mapper
public interface AlertMapper {

    /**
     * Total and unconfirmed alert counts for a tenant.
     *
     * @param tenantId tenant scope
     * @return single row with total and unconfirmed counts
     */
    AlertCountersRow countAll(@Param("tenantId") Long tenantId);

    /**
     * Alert counts grouped by alarm type flag, descending by count.
     *
     * @param tenantId tenant scope
     * @return buckets keyed by alarm type flag string
     */
    List<BucketRow> countByType(@Param("tenantId") Long tenantId);

    /**
     * Total and unconfirmed alert counts grouped by source (point/device/driver).
     *
     * @param tenantId tenant scope
     * @return per-source total and unconfirmed counts
     */
    List<SourceStatsRow> countBySource(@Param("tenantId") Long tenantId);

    /**
     * The most recent alerts for a tenant, newest first.
     *
     * @param tenantId tenant scope
     * @param limit    maximum number of alerts
     * @return recent alert items
     */
    List<AlertItemRow> latest(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    /**
     * Alert counts bucketed by hour from the anchor onward, ascending by hour. The
     * service layer zero-pads missing hours into a 24-point sparkline.
     *
     * @param tenantId tenant scope
     * @param from     sparkline start anchor (an on-the-hour instant)
     * @return hourly count rows
     */
    List<HourCountRow> hourlyCounts(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    /**
     * Page alert items by source, alarm type, confirm flag, and start time, newest first.
     *
     * @param tenantId      tenant scope
     * @param source        source filter (point/device/driver), or null for all
     * @param alarmTypeFlag alarm type flag filter, or null for all
     * @param confirmFlag   confirm flag filter, or null for all
     * @param from          start time filter
     * @param offset        zero-based page offset
     * @param size          page size
     * @return matching alert items
     */
    List<AlertItemRow> listPaged(@Param("tenantId") Long tenantId, @Param("source") String source,
                                 @Param("alarmTypeFlag") Integer alarmTypeFlag, @Param("confirmFlag") Integer confirmFlag,
                                 @Param("from") LocalDateTime from, @Param("offset") long offset, @Param("size") long size);

    /**
     * Count alerts matching the same filters as {@link #listPaged}, for pagination total.
     *
     * @param tenantId      tenant scope
     * @param source        source filter, or null for all
     * @param alarmTypeFlag alarm type flag filter, or null for all
     * @param confirmFlag   confirm flag filter, or null for all
     * @param from          start time filter
     * @return total matching count
     */
    long countFiltered(@Param("tenantId") Long tenantId, @Param("source") String source,
                       @Param("alarmTypeFlag") Integer alarmTypeFlag, @Param("confirmFlag") Integer confirmFlag,
                       @Param("from") LocalDateTime from);

    /**
     * Per-source total and unconfirmed alert counts since the given start (typically
     * the start of today).
     *
     * @param tenantId tenant scope
     * @param from     day-start instant
     * @return per-source total and unconfirmed counts
     */
    List<SourceStatsRow> todayBySource(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    /**
     * Mark one alert confirmed, scoped by source.
     *
     * @param tenantId tenant scope
     * @param source   source scoping the alert (point/device/driver)
     * @param id       the alert id
     * @return affected row count
     */
    int confirmOne(@Param("tenantId") Long tenantId, @Param("source") String source, @Param("id") Long id);

    /**
     * Mark one alert unconfirmed, scoped by source.
     *
     * @param tenantId tenant scope
     * @param source   source scoping the alert (point/device/driver)
     * @param id       the alert id
     * @return affected row count
     */
    int unconfirmOne(@Param("tenantId") Long tenantId, @Param("source") String source, @Param("id") Long id);

    /**
     * Daily alert trend from the anchor to today, zero-padded for missing days. Note:
     * the {@code deviceCount} column covers point + device sources combined, while
     * {@code driverCount} is driver only.
     *
     * @param tenantId tenant scope
     * @param from     trend start (end-of-day instant N days back)
     * @return daily device (point+device) and driver counts
     */
    List<AlertTrendRow> dailyTrend(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    /**
     * Top-N entities by alert count since the anchor.
     *
     * @param tenantId tenant scope
     * @param from     lookback start
     * @param limit    maximum number of entities
     * @return top entities by alert count
     */
    List<SourceCountRow> topSources(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                    @Param("limit") int limit);

    /**
     * Alert counts on a day-of-week (0=Sunday) by hour-of-day (0-23) grid; the service
     * layer zero-pads to the full 168 cells.
     *
     * @param tenantId tenant scope
     * @param from     lookback start
     * @return activity cells keyed by weekday and hour
     */
    List<ActivityCellRow> activityHeatmap(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    /**
     * Alert counts grouped by the alarm classification string in the extension JSON.
     *
     * @param tenantId tenant scope
     * @param from     lookback start
     * @return buckets keyed by alarm classification string
     */
    List<BucketRow> typeDistribution(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    /**
     * Alert-storm sources: entities whose alert count in the window meets the threshold.
     *
     * @param tenantId tenant scope
     * @param from     lookback start
     * @param minCount minimum alert count for a single entity to qualify as a storm
     * @param limit    maximum number of sources
     * @return storm sources by alert count
     */
    List<SourceCountRow> stormSources(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                      @Param("minCount") int minCount, @Param("limit") int limit);

    // ===== Phase-2 insights ====================================================

    /**
     * Flapping sources: entities where the same alarm type fired repeatedly. Flapping
     * here means repeated same-source/same-type firings, not confirm/recover toggling.
     *
     * @param tenantId tenant scope
     * @param from     lookback start
     * @param minCount minimum repeat count for a single (entity, alarm type) pair
     * @param limit    maximum number of sources
     * @return flapping sources by repeat count
     */
    List<FlappingRow> flappingSources(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                      @Param("minCount") int minCount, @Param("limit") int limit);

    /**
     * Co-occurrence correlation pairs: pairs of alerts from different entities whose
     * create times fall within the window of each other.
     *
     * @param tenantId  tenant scope
     * @param from      lookback start
     * @param windowSec co-occurrence window in seconds
     * @param limit     maximum number of pairs
     * @return correlation pairs by co-occurrence count
     */
    List<CorrelationPairRow> correlationPairs(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                              @Param("windowSec") int windowSec, @Param("limit") int limit);

    /**
     * Alert counts grouped by device within each profile, used for peer (sibling-device)
     * outlier detection by the service layer.
     *
     * @param tenantId tenant scope
     * @param from     lookback start
     * @return per-profile, per-device alert counts
     */
    List<PeerAlarmRow> peerAlarmCounts(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    /**
     * Age distribution of unconfirmed alerts in four buckets: under 1h, 1-6h, 6-24h,
     * over 24h, plus the unconfirmed total.
     *
     * @param tenantId tenant scope
     * @return aging buckets for unconfirmed alerts
     */
    AgingBucketRow agingBuckets(@Param("tenantId") Long tenantId);

    /**
     * Per-day acknowledge latency for confirmed alerts. Despite the MTTA (mean time to
     * acknowledge) name, this returns p50 and p95 percentiles of the create-to-operate
     * delay, not the arithmetic mean.
     *
     * @param tenantId tenant scope
     * @param from     lookback start
     * @return daily p50/p95 acknowledge latency and confirmed counts
     */
    List<MttaTrendRow> mttaByDay(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    /**
     * Protocol/driver deployment coverage per service: total drivers, enabled drivers,
     * and mounted devices. Sourced from the driver/device tables, not the alert table.
     *
     * @param tenantId tenant scope
     * @return per-service deployment coverage rows
     */
    List<ProtocolHealthRow> protocolHealth(@Param("tenantId") Long tenantId);

    /**
     * Recent configuration changes across driver/device/profile tables since the anchor,
     * for correlating changes with subsequent alerts.
     *
     * @param tenantId tenant scope
     * @param from     lookback start
     * @param limit    maximum number of changes
     * @return recent change rows by operate time
     */
    List<RecentChangeRow> recentChanges(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                        @Param("limit") int limit);

}

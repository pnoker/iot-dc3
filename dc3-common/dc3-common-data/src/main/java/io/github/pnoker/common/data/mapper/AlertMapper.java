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

    AlertCountersRow countAll(@Param("tenantId") Long tenantId);

    List<BucketRow> countByType(@Param("tenantId") Long tenantId);

    List<SourceStatsRow> countBySource(@Param("tenantId") Long tenantId);

    List<AlertItemRow> latest(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    List<HourCountRow> hourlyCounts(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    List<AlertItemRow> listPaged(@Param("tenantId") Long tenantId, @Param("source") String source,
                                 @Param("alarmTypeFlag") Integer alarmTypeFlag, @Param("confirmFlag") Integer confirmFlag,
                                 @Param("from") LocalDateTime from, @Param("offset") long offset, @Param("size") long size);

    long countFiltered(@Param("tenantId") Long tenantId, @Param("source") String source,
                       @Param("alarmTypeFlag") Integer alarmTypeFlag, @Param("confirmFlag") Integer confirmFlag,
                       @Param("from") LocalDateTime from);

    List<SourceStatsRow> todayBySource(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    int confirmOne(@Param("tenantId") Long tenantId, @Param("source") String source, @Param("id") Long id);

    int unconfirmOne(@Param("tenantId") Long tenantId, @Param("source") String source, @Param("id") Long id);

    List<AlertTrendRow> dailyTrend(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    List<SourceCountRow> topSources(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                    @Param("limit") int limit);

    List<ActivityCellRow> activityHeatmap(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    List<BucketRow> typeDistribution(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    List<SourceCountRow> stormSources(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                      @Param("minCount") int minCount, @Param("limit") int limit);

    // ===== Phase-2 insights ====================================================

    List<FlappingRow> flappingSources(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                      @Param("minCount") int minCount, @Param("limit") int limit);

    List<CorrelationPairRow> correlationPairs(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                              @Param("windowSec") int windowSec, @Param("limit") int limit);

    List<PeerAlarmRow> peerAlarmCounts(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    AgingBucketRow agingBuckets(@Param("tenantId") Long tenantId);

    List<MttaTrendRow> mttaByDay(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);

    List<ProtocolHealthRow> protocolHealth(@Param("tenantId") Long tenantId);

    List<RecentChangeRow> recentChanges(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from,
                                        @Param("limit") int limit);

}

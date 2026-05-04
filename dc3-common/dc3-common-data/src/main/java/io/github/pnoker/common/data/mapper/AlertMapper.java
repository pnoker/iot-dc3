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

package io.github.pnoker.common.data.mapper;

import io.github.pnoker.common.data.entity.bo.dashboard.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Alert aggregate queries over {@code dc3_device_event} and
 * {@code dc3_driver_event}. Returns typed Row DTOs — see
 * {@code entity/bo/dashboard/} — with MyBatis snake→camel mapping.
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Mapper
public interface AlertMapper {

    AlertCountersRow countAll(@Param("tenantId") Long tenantId);

    List<BucketRow> countByType(@Param("tenantId") Long tenantId);

    List<SourceStatsRow> countBySource(@Param("tenantId") Long tenantId);

    List<AlertItemRow> latest(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    List<HourCountRow> hourlyCounts(@Param("tenantId") Long tenantId,
                                    @Param("from") LocalDateTime from);

    List<AlertItemRow> listPaged(@Param("tenantId") Long tenantId,
                                 @Param("source") String source,
                                 @Param("eventTypeFlag") Integer eventTypeFlag,
                                 @Param("confirmFlag") Integer confirmFlag,
                                 @Param("from") LocalDateTime from,
                                 @Param("offset") long offset,
                                 @Param("size") long size);

    long countFiltered(@Param("tenantId") Long tenantId,
                       @Param("source") String source,
                       @Param("eventTypeFlag") Integer eventTypeFlag,
                       @Param("confirmFlag") Integer confirmFlag,
                       @Param("from") LocalDateTime from);

    List<SourceStatsRow> todayBySource(@Param("tenantId") Long tenantId,
                                       @Param("from") LocalDateTime from);

    int confirmOne(@Param("tenantId") Long tenantId,
                   @Param("source") String source,
                   @Param("id") Long id);

    int unconfirmOne(@Param("tenantId") Long tenantId,
                     @Param("source") String source,
                     @Param("id") Long id);

    List<AlertTrendRow> dailyTrend(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from);

    List<SourceCountRow> topSources(@Param("tenantId") Long tenantId,
                                    @Param("from") LocalDateTime from,
                                    @Param("limit") int limit);

    List<ActivityCellRow> activityHeatmap(@Param("tenantId") Long tenantId,
                                          @Param("from") LocalDateTime from);

    List<BucketRow> typeDistribution(@Param("tenantId") Long tenantId,
                                     @Param("from") LocalDateTime from);

    List<SourceCountRow> stormSources(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("minCount") int minCount,
                                      @Param("limit") int limit);

    // ===== Phase-2 insights ====================================================

    List<FlappingRow> flappingSources(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("minCount") int minCount,
                                      @Param("limit") int limit);

    List<CorrelationPairRow> correlationPairs(@Param("tenantId") Long tenantId,
                                              @Param("from") LocalDateTime from,
                                              @Param("windowSec") int windowSec,
                                              @Param("limit") int limit);

    List<PeerAlarmRow> peerAlarmCounts(@Param("tenantId") Long tenantId,
                                       @Param("from") LocalDateTime from);

    AgingBucketRow agingBuckets(@Param("tenantId") Long tenantId);

    List<MttaTrendRow> mttaByDay(@Param("tenantId") Long tenantId,
                                 @Param("from") LocalDateTime from);

    List<ProtocolHealthRow> protocolHealth(@Param("tenantId") Long tenantId);

    List<RecentChangeRow> recentChanges(@Param("tenantId") Long tenantId,
                                        @Param("from") LocalDateTime from,
                                        @Param("limit") int limit);
}

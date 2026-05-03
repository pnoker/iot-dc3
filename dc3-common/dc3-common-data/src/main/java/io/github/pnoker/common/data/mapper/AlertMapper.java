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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Alert aggregation queries over {@code dc3_device_event} and
 * {@code dc3_driver_event}. Both tables live in the master data source (not
 * Timescale hypertables) and carry a {@code tenant_id} column; all queries
 * are tenant-scoped.
 *
 * <p>SQL lives in {@code resources/mapping/AlertMapper.xml}.</p>
 *
 * @author pnoker
 * @since 2026.5.2
 */
@Mapper
public interface AlertMapper {

    /**
     * Overall counters for the alert stat card, scoped to one tenant.
     */
    Map<String, Object> countAll(@Param("tenantId") Long tenantId);

    /**
     * Per-type breakdown across both event tables. event_type_flag is a
     * SMALLINT; we group on it and return (key, count) rows.
     */
    List<Map<String, Object>> countByType(@Param("tenantId") Long tenantId);

    /**
     * Per-source (device / driver) alert counts, scoped to one tenant.
     * Returns rows with (source, total, unconfirmed).
     */
    List<Map<String, Object>> countBySource(@Param("tenantId") Long tenantId);

    /**
     * Most recent N events across device + driver tables, flagged with source.
     */
    List<Map<String, Object>> latest(@Param("tenantId") Long tenantId, @Param("limit") int limit);

    /**
     * Event counts grouped by the hour bucket they fall into, over a window
     * starting at {@code from}. Service layer aligns the returned rows to a
     * fixed-length sparkline array.
     */
    List<Map<String, Object>> hourlyCounts(@Param("tenantId") Long tenantId,
                                           @Param("from") LocalDateTime from);

    /**
     * Paged event list across device + driver event tables, filterable by
     * source / event type / confirm flag. Backs the settings / events page.
     * {@code source} is {@code null} (both tables), {@code "device"}, or
     * {@code "driver"}. Service layer whitelists it before the call.
     */
    List<Map<String, Object>> listPaged(@Param("tenantId") Long tenantId,
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

    /**
     * Today's ALARM counts per source (device / driver), scoped to one tenant.
     * Returns rows with (source, total, unconfirmed).
     */
    List<Map<String, Object>> todayBySource(@Param("tenantId") Long tenantId,
                                            @Param("from") LocalDateTime from);

    int confirmOne(@Param("tenantId") Long tenantId,
                   @Param("source") String source,
                   @Param("id") Long id);

    /**
     * Flip confirm_flag back to 0 (undo). Same scoping as {@link #confirmOne}.
     */
    int unconfirmOne(@Param("tenantId") Long tenantId,
                     @Param("source") String source,
                     @Param("id") Long id);

    /**
     * Daily event counts split by device/driver source, starting from {@code from}.
     * Returns rows with (date, device_count, driver_count).
     */
    List<Map<String, Object>> dailyTrend(@Param("tenantId") Long tenantId,
                                         @Param("from") LocalDateTime from);

    /**
     * Top N event sources by alarm count, starting from {@code from}.
     * Returns rows with (source, source_id, count).
     */
    List<Map<String, Object>> topSources(@Param("tenantId") Long tenantId,
                                         @Param("from") LocalDateTime from,
                                         @Param("limit") int limit);

    /**
     * ALARM counts bucketed by (day-of-week 0..6, hour-of-day 0..23) over
     * the window starting at {@code from}. Used by the event-overview
     * hourly heatmap. Returns rows with (dow, hour, count).
     */
    List<Map<String, Object>> activityHeatmap(@Param("tenantId") Long tenantId,
                                              @Param("from") LocalDateTime from);

    /**
     * ALARM counts grouped by the {@code event_ext.type} tag (e.g.
     * driver-offline / driver-state-flip / driver-alarm / device-offline /
     * device-alarm) over the window starting at {@code from}. Used by the
     * event-overview type-distribution pie. Returns rows with (type, count).
     */
    List<Map<String, Object>> typeDistribution(@Param("tenantId") Long tenantId,
                                               @Param("from") LocalDateTime from);

    /**
     * Sources whose ALARM count within the window reaches {@code minCount},
     * ordered by count desc. "Storm" sources the operator should look at
     * first. Returns rows with (source, source_id, count).
     */
    List<Map<String, Object>> stormSources(@Param("tenantId") Long tenantId,
                                           @Param("from") LocalDateTime from,
                                           @Param("minCount") int minCount,
                                           @Param("limit") int limit);
}

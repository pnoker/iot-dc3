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
                                        @Param("offset") long offset,
                                        @Param("size") long size);

    long countFiltered(@Param("tenantId") Long tenantId,
                       @Param("source") String source,
                       @Param("eventTypeFlag") Integer eventTypeFlag,
                       @Param("confirmFlag") Integer confirmFlag);

    int confirmOne(@Param("tenantId") Long tenantId,
                   @Param("source") String source,
                   @Param("id") Long id);

    /** Flip confirm_flag back to 0 (undo). Same scoping as {@link #confirmOne}. */
    int unconfirmOne(@Param("tenantId") Long tenantId,
                     @Param("source") String source,
                     @Param("id") Long id);
}

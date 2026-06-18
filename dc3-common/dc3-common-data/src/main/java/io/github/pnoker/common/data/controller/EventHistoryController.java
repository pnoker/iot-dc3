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

package io.github.pnoker.common.data.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.EventHistoryService;
import io.github.pnoker.common.data.entity.builder.EventHistoryBuilder;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventHistoryVO;
import io.github.pnoker.common.data.entity.vo.EventReportVO;
import io.github.pnoker.common.entity.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * REST controller for event report management.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Tag(name = "event_history", description = "Device event audit trail: query historical records of device alarms, state changes, and status transitions with timestamps and event payloads")
@Slf4j
@RestController
@RequestMapping(DataConstant.EVENT_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class EventHistoryController implements BaseController {

    private final EventHistoryService eventHistoryService;

    private final EventHistoryBuilder eventHistoryBuilder;

    /**
     * Record a device event (alarm, state change, or status transition) reported by a
     * device for the current tenant.
     *
     * @param entityVO device event payload (event type, payload, device context) to append to the audit trail
     * @return the ID of the newly created event history record
     */
    @PreAuthorize("@perm.can('event_history', 'list')")
    @Operation(summary = "Report Device Event", description = "Record a device event (alarm, state change, or status transition) reported by a device for the current tenant and return the new record ID. Use when a device reports an event that must be appended to the audit trail.")
    @PostMapping("/report")
    public Mono<R<String>> report(@Validated @RequestBody EventReportVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            String recordId = eventHistoryService.report(tenantId, eventHistoryBuilder.buildBOByVO(entityVO));
            R<String> result = R.ok();
            result.setData(recordId);
            return result;
        }));
    }

    /**
     * Fetch a single device event record by its record ID, tenant-scoped.
     *
     * @param recordId identifier of the event history record to fetch; must belong to the current tenant
     * @return the matched EventHistoryVO with event type, payload and timestamp; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('event_history', 'get')")
    @Operation(summary = "Get Event History by Record ID", description = "Fetch a single device event record by its record ID, tenant-scoped. Returns the event type, payload, and timestamp; use to inspect one specific reported event.")
    @GetMapping("/get_by_record_id")
    public Mono<R<EventHistoryVO>> getByRecordId(@Parameter(description = "Identifier of the event history record to fetch; must belong to the current tenant.", example = "1024") @NotBlank @RequestParam String recordId) {
        return getTenantId().flatMap(tenantId -> async(() ->
                R.ok(eventHistoryService.getByRecordId(tenantId, recordId))));
    }

    /**
     * Page through device event records (alarms, state changes, status transitions) for
     * the current tenant, filtered by the query body.
     *
     * @param queryVO optional query filters; a default empty query is used when null
     * @return a page of EventHistoryVO matching the query, ordered by event time
     */
    @PreAuthorize("@perm.can('event_history', 'list')")
    @Operation(summary = "List Event History Records", description = "Page through device event records (alarms, state changes, status transitions) for the current tenant, filtered by the query body. Use to browse the append-only event audit trail; results are ordered by event time.")
    @PostMapping("/list")
    public Mono<R<Page<EventHistoryVO>>> list(@RequestBody(required = false) EventHistoryQueryVO queryVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventHistoryQueryVO query = Objects.isNull(queryVO) ? new EventHistoryQueryVO() : queryVO;
            return R.ok(eventHistoryService.list(tenantId, query));
        }));
    }

}

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
import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventHistoryVO;
import io.github.pnoker.common.data.entity.vo.EventReportVO;
import io.github.pnoker.common.entity.R;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for event report management.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Tag(name = "event_history", description = "事件历史")
@Slf4j
@RestController
@RequestMapping(DataConstant.EVENT_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class EventHistoryController implements BaseController {

    private final EventHistoryService eventHistoryService;

    private final EventHistoryBuilder eventHistoryBuilder;

    @PreAuthorize("@perm.can('event_history', 'list')")
    @Operation(summary = "事件历史 - report", description = "事件历史 - report")
    @PostMapping("/report")
    public Mono<R<String>> report(@Validated @RequestBody EventReportVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            String recordId = eventHistoryService.report(tenantId, entityVO);
            R<String> result = R.ok();
            result.setData(recordId);
            return result;
        }));
    }

    @PreAuthorize("@perm.can('event_history', 'get')")
    @Operation(summary = "查询EventHistory", description = "根据条件查询EventHistory")
    @GetMapping("/get_by_record_id")
    public Mono<R<EventHistoryVO>> getByRecordId(@NotBlank @RequestParam String recordId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventHistoryDO entityDO = eventHistoryService.getByRecordId(tenantId, recordId);
            return R.ok(eventHistoryBuilder.buildVOByDO(entityDO));
        }));
    }

    @PreAuthorize("@perm.can('event_history', 'list')")
    @Operation(summary = "查询EventHistory列表", description = "分页查询EventHistory列表")
    @PostMapping("/list")
    public Mono<R<Page<EventHistoryVO>>> list(@RequestBody(required = false) EventHistoryQueryVO queryVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventHistoryQueryVO query = Objects.isNull(queryVO) ? new EventHistoryQueryVO() : queryVO;
            Page<EventHistoryDO> page = eventHistoryService.list(tenantId, query);
            return R.ok(eventHistoryBuilder.buildVOPageByDOPage(page));
        }));
    }

}

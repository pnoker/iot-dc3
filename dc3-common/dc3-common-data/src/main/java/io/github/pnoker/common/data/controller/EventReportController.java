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

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.biz.EventReportService;
import io.github.pnoker.common.data.entity.model.EventRecordDO;
import io.github.pnoker.common.data.entity.vo.EventRecordQueryVO;
import io.github.pnoker.common.data.entity.vo.EventReportVO;
import io.github.pnoker.common.entity.R;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
@Slf4j
@RestController
@RequestMapping(DataConstant.EVENT_REPORT_URL_PREFIX)
@RequiredArgsConstructor
public class EventReportController implements BaseController {

    private final EventReportService eventReportService;

    @PostMapping("/report")
    public Mono<R<String>> report(@Validated @RequestBody EventReportVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            String recordId = eventReportService.report(tenantId, entityVO);
            return R.ok(recordId);
        }));
    }

    @GetMapping("/{recordId}")
    public Mono<R<EventRecordDO>> getByRecordId(@NotBlank @PathVariable String recordId) {
        return async(() -> R.ok(eventReportService.getByRecordId(recordId)));
    }

    @PostMapping("/list")
    public Mono<R<Page<EventRecordDO>>> list(@RequestBody(required = false) EventRecordQueryVO queryVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            EventRecordQueryVO query = Objects.isNull(queryVO) ? new EventRecordQueryVO() : queryVO;
            return R.ok(eventReportService.list(tenantId, query));
        }));
    }

}

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
import io.github.pnoker.common.data.entity.bo.NotifyRecordBO;
import io.github.pnoker.common.data.entity.builder.NotifyRecordBuilder;
import io.github.pnoker.common.data.entity.query.NotifyRecordQuery;
import io.github.pnoker.common.data.entity.vo.NotifyRecordVO;
import io.github.pnoker.common.data.service.NotifyRecordService;
import io.github.pnoker.common.entity.R;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Notification delivery record controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.NOTIFY_RECORD_URL_PREFIX)
public class NotifyRecordController implements BaseController {

    private final NotifyRecordBuilder notifyRecordBuilder;

    private final NotifyRecordService notifyRecordService;

    public NotifyRecordController(NotifyRecordBuilder notifyRecordBuilder,
                                  NotifyRecordService notifyRecordService) {
        this.notifyRecordBuilder = notifyRecordBuilder;
        this.notifyRecordService = notifyRecordService;
    }

    @GetMapping("/select_by_id")
    public Mono<R<NotifyRecordVO>> selectById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyRecordBO entityBO = requireTenant(tenantId, notifyRecordService.selectById(id));
            return R.ok(notifyRecordBuilder.buildVOByBO(entityBO));
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<NotifyRecordVO>>> list(@RequestBody(required = false) NotifyRecordQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyRecordQuery query = Objects.isNull(entityQuery) ? new NotifyRecordQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<NotifyRecordBO> entityPageBO = notifyRecordService.selectByPage(query);
            return R.ok(notifyRecordBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}

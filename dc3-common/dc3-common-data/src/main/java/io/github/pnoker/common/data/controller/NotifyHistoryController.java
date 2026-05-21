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

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.builder.NotifyHistoryBuilder;
import io.github.pnoker.common.data.entity.query.NotifyHistoryQuery;
import io.github.pnoker.common.data.entity.vo.NotifyHistoryVO;
import io.github.pnoker.common.data.service.NotifyHistoryService;
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
 * Notification delivery history controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.NOTIFY_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyHistoryController implements BaseController {

    private final NotifyHistoryBuilder notifyHistoryBuilder;

    private final NotifyHistoryService notifyHistoryService;

    @GetMapping("/get_by_id")
    public Mono<R<NotifyHistoryVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyHistoryBO entityBO = requireTenant(tenantId, notifyHistoryService.getById(id));
            return R.ok(notifyHistoryBuilder.buildVOByBO(entityBO));
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<NotifyHistoryVO>>> list(@RequestBody(required = false) NotifyHistoryQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyHistoryQuery query = Objects.isNull(entityQuery) ? new NotifyHistoryQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<NotifyHistoryBO> entityPageBO = notifyHistoryService.list(query);
            return R.ok(notifyHistoryBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}

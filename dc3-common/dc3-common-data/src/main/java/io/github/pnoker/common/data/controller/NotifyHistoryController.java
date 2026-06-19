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
import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.builder.NotifyHistoryBuilder;
import io.github.pnoker.common.data.entity.query.NotifyHistoryQuery;
import io.github.pnoker.common.data.entity.vo.NotifyHistoryVO;
import io.github.pnoker.common.data.service.NotifyHistoryService;
import io.github.pnoker.common.entity.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "notify_history", description = "Notification delivery history: query alert dispatch records including timestamps, target channels, and delivery status for audit and troubleshooting")
@Slf4j
@RestController
@RequestMapping(DataConstant.NOTIFY_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyHistoryController implements BaseController {

    private final NotifyHistoryBuilder notifyHistoryBuilder;

    private final NotifyHistoryService notifyHistoryService;

    /**
     * Return a single dispatched-notification record owned by the current tenant.
     *
     * @param id id of the notification history record to retrieve
     * @return the matched NotifyHistoryVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('notify_history', 'get')")
    @Operation(summary = "Get Notification History by ID", description = "Return a single dispatched-notification record by ID (tenant-scoped). Use to inspect one notification's timestamp, target channel, and delivery status.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<NotifyHistoryVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyHistoryBO entityBO = requireTenant(tenantId, notifyHistoryService.getById(id));
            return R.ok(notifyHistoryBuilder.buildVOByBO(entityBO));
        }));
    }

    /**
     * Page through notification delivery records owned by the current tenant.
     *
     * @param entityQuery optional filter query (channel, delivery status, time range); treated as empty when null
     * @return a page of NotifyHistoryVO matching the query
     */
    @PreAuthorize("@perm.can('notify_history', 'list')")
    @Operation(summary = "List Notification History", description = "Page through notifications already dispatched for the current tenant, filterable by channel and delivery status. Use to audit what was sent and whether each channel succeeded.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
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

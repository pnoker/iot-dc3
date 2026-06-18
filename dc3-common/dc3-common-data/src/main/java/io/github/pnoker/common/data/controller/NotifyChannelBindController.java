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
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.query.NotifyChannelBindQuery;
import io.github.pnoker.common.data.entity.vo.NotifyChannelBindVO;
import io.github.pnoker.common.data.service.NotifyChannelBindService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
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
 * Notification channel binding controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "notify_channel_bind", description = "Notification-to-channel bindings: route specific notification rules to delivery channels for targeted alert distribution")
@Slf4j
@RestController
@RequestMapping(DataConstant.NOTIFY_CHANNEL_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyChannelBindController implements BaseController {

    private final NotifyChannelBindBuilder notifyChannelBindBuilder;

    private final NotifyChannelBindService notifyChannelBindService;

    /**
     * Bind a delivery channel to a notification rule for the current tenant, so alerts
     * from that rule route through the channel.
     *
     * @param entityVO binding payload identifying the notify rule and the delivery channel to link
     * @return add-success status
     */
    @PreAuthorize("@perm.can('notify_channel_bind', 'add')")
    @Operation(summary = "Add Notification Channel Binding", description = "Bind a delivery channel to a notification rule for the current tenant, so alerts from that rule route through the channel. Returns add-success status.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody NotifyChannelBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBindBO entityBO = notifyChannelBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            notifyChannelBindService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * Delete a notification-to-channel binding by ID, scoped to the current tenant.
     *
     * @param id primary key of the binding to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('notify_channel_bind', 'delete')")
    @Operation(summary = "Delete Notification Channel Binding", description = "Delete a notification-to-channel binding by ID, scoped to the current tenant. Use to stop routing a rule's alerts through a channel; ownership is validated before deletion.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, notifyChannelBindService.getById(id));
            notifyChannelBindService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * Update an existing notification-to-channel binding (rule, channel, enable flag) for
     * the current tenant.
     *
     * @param entityVO binding payload with the rule, channel or enable flag to update; ownership is validated before the change is applied
     * @return update-success status
     */
    @PreAuthorize("@perm.can('notify_channel_bind', 'update')")
    @Operation(summary = "Update Notification Channel Binding", description = "Update an existing notification-to-channel binding (rule, channel, enable flag) for the current tenant. Ownership is validated before the change is applied.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody NotifyChannelBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBindBO entityBO = notifyChannelBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, notifyChannelBindService.getById(entityBO.getId()));
            notifyChannelBindService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * Return the full detail of a single notification-to-channel binding by ID, scoped
     * to the current tenant.
     *
     * @param id primary key of the target binding; must belong to the current tenant
     * @return the matched NotifyChannelBindVO with rule, channel, enable flag and extension attributes; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('notify_channel_bind', 'get')")
    @Operation(summary = "Get Notification Channel Binding by ID", description = "Return the full detail of a single notification-to-channel binding by ID, scoped to the current tenant. Returns the rule, channel, enable flag and extension attributes.")
    @GetMapping("/get_by_id")
    public Mono<R<NotifyChannelBindVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBindBO entityBO = requireTenant(tenantId, notifyChannelBindService.getById(id));
            return R.ok(notifyChannelBindBuilder.buildVOByBO(entityBO));
        }));
    }

    /**
     * Page through notification-to-channel bindings for the current tenant, filterable
     * by notifyId, channelId and enable flag.
     *
     * @param entityQuery optional filter and pagination body; a default empty query is used when null
     * @return a page of NotifyChannelBindVO matching the query
     */
    @PreAuthorize("@perm.can('notify_channel_bind', 'list')")
    @Operation(summary = "List Notification Channel Bindings", description = "Page through notification-to-channel bindings for the current tenant, filterable by notifyId, channelId and enable flag. Use to discover which delivery channels a rule fires through.")
    @PostMapping("/list")
    public Mono<R<Page<NotifyChannelBindVO>>> list(@RequestBody(required = false) NotifyChannelBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBindQuery query = Objects.isNull(entityQuery) ? new NotifyChannelBindQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<NotifyChannelBindBO> entityPageBO = notifyChannelBindService.list(query);
            return R.ok(notifyChannelBindBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}

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
import io.github.pnoker.common.data.entity.builder.NotifyHistoryBuilder;
import io.github.pnoker.common.data.entity.query.NotifyHistoryQuery;
import io.github.pnoker.common.data.entity.vo.NotifyHistoryVO;
import io.github.pnoker.common.data.service.NotifyHistoryService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** REST controller exposing notify history endpoints. */
@Tag(name = "notify_history", description = "Notification delivery history")
@RestController
@RequestMapping(DataConstant.NOTIFY_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyHistoryController implements BaseController {

    private final NotifyHistoryBuilder notifyHistoryBuilder;
    private final NotifyHistoryService notifyHistoryService;

    /** Resolve the notify configuration history by its id. */
    @PreAuthorize("@perm.can('notify_history', 'get')")
    @Operation(
            summary = "Get Notification History by ID",
            description = "Return one tenant-owned notification delivery history record for audit and troubleshooting.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_id")
    public Mono<NotifyHistoryVO> getById(
            @Parameter(description = "Primary key of the tenant-owned notification history record")
                    @NotNull
                    @RequestParam("id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> notifyHistoryService.getById(tenantId, id).map(notifyHistoryBuilder::buildVOByBO));
    }

    /** Page notify configuration histories matching the tenant-scoped filters. */
    @PreAuthorize("@perm.can('notify_history', 'list')")
    @Operation(
            summary = "List Notification History",
            description =
                    "List tenant-owned notification delivery history with offset pagination and explicit filters.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list")
    public Mono<OffsetPage<NotifyHistoryVO>> list(@RequestBody(required = false) NotifyHistoryQuery query) {
        return getTenantId()
                .flatMap(tenantId -> notifyHistoryService
                        .list(tenantId, query)
                        .map(page -> OffsetPage.of(
                                page.items().stream()
                                        .map(notifyHistoryBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total())));
    }

    /** Delete the notify configuration history, reporting whether a row was removed. */
    @PreAuthorize("@perm.can('notify_history', 'delete')")
    @Operation(
            summary = "Delete Notification History",
            description = "Delete one tenant-owned notification history record permanently after audit review.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                        @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    public Mono<Boolean> delete(
            @Parameter(description = "Primary key of the tenant-owned notification history record to delete")
                    @NotNull
                    @RequestParam("id")
                    Long id) {
        return getTenantId().flatMap(tenantId -> notifyHistoryService.delete(tenantId, id));
    }
}

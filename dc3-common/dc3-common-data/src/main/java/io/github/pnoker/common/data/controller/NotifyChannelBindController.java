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
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.query.NotifyChannelBindQuery;
import io.github.pnoker.common.data.entity.vo.NotifyChannelBindVO;
import io.github.pnoker.common.data.service.NotifyChannelBindService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "notify_channel_bind", description = "Notification policy channel bindings")
@RestController
@RequestMapping(DataConstant.NOTIFY_CHANNEL_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyChannelBindController implements BaseController {
    private final NotifyChannelBindBuilder builder;
    private final NotifyChannelBindService service;

    @Operation(
            summary = "Add binding",
            description = "Create a tenant-scoped policy channel binding.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('notify_channel_bind', 'add')")
    @PostMapping("/add")
    public Mono<NotifyChannelBindVO> add(@Validated(Add.class) @RequestBody NotifyChannelBindVO request) {
        return context().flatMap(ctx -> {
            NotifyChannelBindBO value = builder.buildBOByVO(request);
            value.setTenantId(ctx.tenant());
            value.setCreatorId(ctx.userId());
            value.setCreatorName(ctx.userName());
            value.setOperatorId(ctx.userId());
            value.setOperatorName(ctx.userName());
            return service.add(value).map(builder::buildVOByBO);
        });
    }

    @Operation(
            summary = "Delete binding",
            description = "Delete a tenant-scoped policy channel binding.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "HIGH")))
    @PreAuthorize("@perm.can('notify_channel_bind', 'delete')")
    @DeleteMapping("/delete")
    public Mono<Void> delete(@Parameter(description = "Tenant-owned binding ID") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenant -> service.delete(tenant, id).then());
    }

    @Operation(
            summary = "Update binding",
            description = "Update a tenant-scoped policy channel binding.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = @ExtensionProperty(name = "riskLevel", value = "MEDIUM")))
    @PreAuthorize("@perm.can('notify_channel_bind', 'update')")
    @PostMapping("/update")
    public Mono<NotifyChannelBindVO> update(@Validated(Update.class) @RequestBody NotifyChannelBindVO request) {
        return context().flatMap(ctx -> {
            NotifyChannelBindBO value = builder.buildBOByVO(request);
            value.setTenantId(ctx.tenant());
            value.setOperatorId(ctx.userId());
            value.setOperatorName(ctx.userName());
            return service.update(value).map(builder::buildVOByBO);
        });
    }

    @Operation(
            summary = "Get binding",
            description = "Return one tenant-scoped policy channel binding.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('notify_channel_bind', 'get')")
    @GetMapping("/get_by_id")
    public Mono<NotifyChannelBindVO> getById(
            @Parameter(description = "Tenant-owned binding ID") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenant -> service.getById(tenant, id).map(builder::buildVOByBO));
    }

    @Operation(
            summary = "List bindings",
            description = "List tenant-scoped policy channel bindings.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('notify_channel_bind', 'list')")
    @PostMapping("/list")
    public Mono<OffsetPage<NotifyChannelBindVO>> list(@RequestBody(required = false) NotifyChannelBindQuery request) {
        return getTenantId()
                .flatMap(tenant -> service.list(tenant, request)
                        .map(page -> OffsetPage.of(
                                page.items().stream().map(builder::buildVOByBO).toList(),
                                page.offset(),
                                page.limit(),
                                page.total())));
    }

    private Mono<Context> context() {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .map(value -> new Context(value.getT1().getT1(), value.getT1().getT2(), value.getT2()));
    }

    private record Context(Long tenant, Long userId, String userName) {}
}

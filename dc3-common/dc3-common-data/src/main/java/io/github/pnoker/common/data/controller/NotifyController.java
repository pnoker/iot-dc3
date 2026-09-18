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
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.query.NotifyQuery;
import io.github.pnoker.common.data.entity.vo.NotifyVO;
import io.github.pnoker.common.data.service.NotifyService;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** REST controller exposing notify endpoints. */
@Tag(name = "notify", description = "Alarm notification policies")
@RestController
@RequestMapping(DataConstant.NOTIFY_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyController implements BaseController {
    private final NotifyBuilder builder;
    private final NotifyService service;

    /** Add one notify configuration and return the stored view. */
    @Operation(
            summary = "Add notify",
            description = "Create a tenant-scoped notification policy.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('notify', 'add')")
    @PostMapping("/add")
    public Mono<NotifyVO> add(@Validated(Add.class) @RequestBody NotifyVO request) {
        return context().flatMap(ctx -> {
            NotifyBO value = builder.buildBOByVO(request);
            value.setTenantId(ctx.tenant());
            value.setCreatorId(ctx.userId());
            value.setCreatorName(ctx.userName());
            value.setOperatorId(ctx.userId());
            value.setOperatorName(ctx.userName());
            return service.add(value).map(builder::buildVOByBO);
        });
    }

    /** Delete the notify configuration. */
    @Operation(
            summary = "Delete notify",
            description = "Delete a tenant-scoped notification policy.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "HIGH")))
    @PreAuthorize("@perm.can('notify', 'delete')")
    @DeleteMapping("/delete")
    public Mono<Void> delete(@Parameter(description = "Tenant-owned notify ID") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenant -> service.delete(tenant, id).then());
    }

    /** Update one notify configuration and emit the updated row. */
    @Operation(
            summary = "Update notify",
            description = "Update a tenant-scoped notification policy.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = @ExtensionProperty(name = "riskLevel", value = "MEDIUM")))
    @PreAuthorize("@perm.can('notify', 'update')")
    @PostMapping("/update")
    public Mono<NotifyVO> update(@Validated(Update.class) @RequestBody NotifyVO request) {
        return context().flatMap(ctx -> {
            NotifyBO value = builder.buildBOByVO(request);
            value.setTenantId(ctx.tenant());
            value.setOperatorId(ctx.userId());
            value.setOperatorName(ctx.userName());
            return service.update(value).map(builder::buildVOByBO);
        });
    }

    /** Resolve the notify configuration by its id. */
    @Operation(
            summary = "Get notify",
            description = "Return one tenant-scoped notification policy.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('notify', 'get')")
    @GetMapping("/get_by_id")
    public Mono<NotifyVO> getById(
            @Parameter(description = "Tenant-owned notify ID") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenant -> service.getById(tenant, id).map(builder::buildVOByBO));
    }

    /** Page notify configurations matching the tenant-scoped filters. */
    @Operation(
            summary = "List notify",
            description = "List tenant-scoped notification policies.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('notify', 'list')")
    @PostMapping("/list")
    public Mono<OffsetPage<NotifyVO>> list(@RequestBody(required = false) NotifyQuery request) {
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

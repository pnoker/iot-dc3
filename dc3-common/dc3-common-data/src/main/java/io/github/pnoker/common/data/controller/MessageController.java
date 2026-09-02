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
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.query.MessageQuery;
import io.github.pnoker.common.data.entity.vo.MessageVO;
import io.github.pnoker.common.data.service.MessageService;
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

@Tag(name = "message", description = "Alarm message templates")
@RestController("dataMessageController")
@RequestMapping(DataConstant.MESSAGE_URL_PREFIX)
@RequiredArgsConstructor
public class MessageController implements BaseController {
    private final MessageBuilder builder;
    private final MessageService service;

    @Operation(
            summary = "Add message",
            description = "Create a tenant-scoped alarm message template.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('message', 'add')")
    @PostMapping("/add")
    public Mono<MessageVO> add(@Validated(Add.class) @RequestBody MessageVO request) {
        return context().flatMap(ctx -> {
            MessageBO value = builder.buildBOByVO(request);
            value.setTenantId(ctx.tenant());
            value.setCreatorId(ctx.userId());
            value.setCreatorName(ctx.userName());
            value.setOperatorId(ctx.userId());
            value.setOperatorName(ctx.userName());
            return service.add(value).map(builder::buildVOByBO);
        });
    }

    @Operation(
            summary = "Delete message",
            description = "Delete a tenant-scoped alarm message template.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "HIGH")))
    @PreAuthorize("@perm.can('message', 'delete')")
    @DeleteMapping("/delete")
    public Mono<Void> delete(@Parameter(description = "Tenant-owned message ID") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenant -> service.delete(tenant, id).then());
    }

    @Operation(
            summary = "Update message",
            description = "Update a tenant-scoped alarm message template.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = @ExtensionProperty(name = "riskLevel", value = "MEDIUM")))
    @PreAuthorize("@perm.can('message', 'update')")
    @PostMapping("/update")
    public Mono<MessageVO> update(@Validated(Update.class) @RequestBody MessageVO request) {
        return context().flatMap(ctx -> {
            MessageBO value = builder.buildBOByVO(request);
            value.setTenantId(ctx.tenant());
            value.setOperatorId(ctx.userId());
            value.setOperatorName(ctx.userName());
            return service.update(value).map(builder::buildVOByBO);
        });
    }

    @Operation(
            summary = "Get message",
            description = "Return one tenant-scoped alarm message template.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('message', 'get')")
    @GetMapping("/get_by_id")
    public Mono<MessageVO> getById(
            @Parameter(description = "Tenant-owned message ID") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenant -> service.getById(tenant, id).map(builder::buildVOByBO));
    }

    @Operation(
            summary = "List messages",
            description = "List tenant-scoped alarm message templates.",
            extensions =
                    @Extension(name = "x-dc3-ai", properties = @ExtensionProperty(name = "riskLevel", value = "LOW")))
    @PreAuthorize("@perm.can('message', 'list')")
    @PostMapping("/list")
    public Mono<OffsetPage<MessageVO>> list(@RequestBody(required = false) MessageQuery request) {
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

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
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.query.MessageQuery;
import io.github.pnoker.common.data.entity.vo.MessageVO;
import io.github.pnoker.common.data.service.MessageService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
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
 * Alarm message template controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "message", description = "Alarm message templates: manage reusable message templates (name, code, level, enabled flag) that alarm rules and notifications can reference")
@Slf4j
@RestController
@RequestMapping(DataConstant.MESSAGE_URL_PREFIX)
@RequiredArgsConstructor
public class MessageController implements BaseController {

    private final MessageBuilder messageBuilder;

    private final MessageService messageService;

    /**
     * Create a new alarm message template scoped to the current tenant.
     *
     * @param entityVO message template payload (name, code, level, enabled flag) to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('message', 'add')")
    @Operation(summary = "Add Message", description = "Create a new alarm message template scoped to the current tenant. Use to register a reusable message (name, code, level, enabled flag) that alarm rules and notifications can reference.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody MessageVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            MessageBO entityBO = messageBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            messageService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a tenant-owned alarm message template by its ID.
     *
     * @param id primary key of the message template to delete; must belong to the current tenant
     * @return delete-success status
     */
    @PreAuthorize("@perm.can('message', 'delete')")
    @Operation(summary = "Delete Message", description = "Delete a tenant-owned alarm message template by its ID. Returns not-found if the record belongs to another tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, messageService.getById(id));
            messageService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update the name, code, level or enabled flag of an existing alarm message template
     * owned by the current tenant.
     *
     * @param entityVO message template payload with the fields to update; tenant scope is enforced from the caller, not the body
     * @return update-success status
     */
    @PreAuthorize("@perm.can('message', 'update')")
    @Operation(summary = "Update Message", description = "Update the name, code, level or enabled flag of an existing alarm message template owned by the current tenant. The tenant scope is enforced from the authenticated caller, not the request body.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody MessageVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            MessageBO entityBO = messageBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, messageService.getById(entityBO.getId()));
            messageService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Return one alarm message template (name, code, level, enabled flag) by its ID,
     * restricted to the current tenant.
     *
     * @param id primary key of the target message template; must belong to the current tenant
     * @return the matched MessageVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('message', 'get')")
    @Operation(summary = "Get Message by ID", description = "Return one alarm message template (name, code, level, enabled flag) by its ID, restricted to the current tenant. Use to inspect a single template before updating or deleting it.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<MessageVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            MessageBO entityBO = requireTenant(tenantId, messageService.getById(id));
            return R.ok(messageBuilder.buildVOByBO(entityBO));
        }));
    }

    /**
     * Page through the current tenant's alarm message templates, filterable by name,
     * code, level and enabled flag.
     *
     * @param entityQuery optional filter and pagination body; a default empty query is used when null
     * @return a page of MessageVO matching the query
     */
    @PreAuthorize("@perm.can('message', 'list')")
    @Operation(summary = "List Messages", description = "Page through the current tenant's alarm message templates, filterable by name, code, level and enabled flag. Use to browse or locate templates for reuse by alarm rules and notification channels.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<MessageVO>>> list(@RequestBody(required = false) MessageQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            MessageQuery query = Objects.isNull(entityQuery) ? new MessageQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<MessageBO> entityPageBO = messageService.list(query);
            return R.ok(messageBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}

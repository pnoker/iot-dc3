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
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.query.RuleQuery;
import io.github.pnoker.common.data.entity.vo.RuleVO;
import io.github.pnoker.common.data.service.RuleService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.SuccessCode;
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
 * Alarm rule controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "rule", description = "Data processing rule definitions: manage filtering, transformation, and routing pipelines that process device data streams through the rule engine")
@Slf4j
@RestController
@RequestMapping(DataConstant.RULE_URL_PREFIX)
@RequiredArgsConstructor
public class RuleController implements BaseController {

    private final RuleBuilder ruleBuilder;

    private final RuleService ruleService;

    /**
     * Create an alarm or automation rule for the current tenant.
     *
     * @param entityVO rule payload (trigger conditions and actions) to create
     * @return add-success status
     */
    @PreAuthorize("@perm.can('rule', 'add')")
    @Operation(summary = "Add Rule", description = "Create an alarm or automation rule (trigger conditions plus actions) for the current tenant. Use to register a new rule the engine evaluates against point values or events.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RuleVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleBO entityBO = ruleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            ruleService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a rule owned by the current tenant.
     *
     * @param id id of the rule to delete
     * @return delete-success status; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('rule', 'delete')")
    @Operation(summary = "Delete Rule", description = "Delete a rule owned by the current tenant by its ID. Tenant ownership is verified before deletion; use to retire a rule that should no longer fire.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, ruleService.getById(id));
            ruleService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update the trigger conditions or actions of a rule owned by the current tenant.
     *
     * @param entityVO rule payload (trigger conditions and actions) to update
     * @return update-success status; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('rule', 'update')")
    @Operation(summary = "Update Rule", description = "Update the trigger conditions or actions of a rule owned by the current tenant. Tenant ownership is verified before the change is applied.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody RuleVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleBO entityBO = ruleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, ruleService.getById(entityBO.getId()));
            ruleService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Return a single rule's full definition owned by the current tenant.
     *
     * @param id id of the rule to retrieve
     * @return the matched RuleVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('rule', 'get')")
    @Operation(summary = "Get Rule by ID", description = "Return one rule's full definition (conditions, actions, owner) for the current tenant. Use to inspect or edit a specific rule before updating or deleting it.")
    @GetMapping("/get_by_id")
    public Mono<R<RuleVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleBO entityBO = requireTenant(tenantId, ruleService.getById(id));
            return R.ok(ruleBuilder.buildVOByBO(entityBO));
        }));
    }

    /**
     * Page through alarm and automation rules owned by the current tenant.
     *
     * @param entityQuery optional filter query (rule name, enable flag, etc.); treated as empty when null
     * @return a page of RuleVO matching the query
     */
    @PreAuthorize("@perm.can('rule', 'list')")
    @Operation(summary = "List Rules", description = "Page through alarm and automation rules owned by the current tenant, filtered by the query body. Use to browse or locate rules for inspection; results are scoped to the caller's tenant.")
    @PostMapping("/list")
    public Mono<R<Page<RuleVO>>> list(@RequestBody(required = false) RuleQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleQuery query = Objects.isNull(entityQuery) ? new RuleQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<RuleBO> entityPageBO = ruleService.list(query);
            return R.ok(ruleBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}

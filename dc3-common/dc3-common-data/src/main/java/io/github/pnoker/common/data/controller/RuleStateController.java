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
import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.builder.RuleStateBuilder;
import io.github.pnoker.common.data.entity.query.RuleStateQuery;
import io.github.pnoker.common.data.entity.vo.RuleStateVO;
import io.github.pnoker.common.data.service.RuleStateService;
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
 * Rule runtime state controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "rule_state", description = "Rule execution states: manage rule engine states including activation, suspension, and runtime statistics for data processing rules")
@Slf4j
@RestController
@RequestMapping(DataConstant.RULE_STATE_URL_PREFIX)
@RequiredArgsConstructor
public class RuleStateController implements BaseController {

    private final RuleStateBuilder ruleStateBuilder;

    private final RuleStateService ruleStateService;

    /**
     * Return the runtime state of a single rule owned by the current tenant.
     *
     * @param id id of the rule state record to retrieve
     * @return the matched RuleStateVO; fails if not found or not tenant-owned
     */
    @PreAuthorize("@perm.can('rule_state', 'get')")
    @Operation(summary = "Get Rule State by ID", description = "Return the runtime state of one rule (enabled/disabled, last-fired, counters) for the current tenant. Use to inspect a single rule's execution status.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<RuleStateVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleStateBO entityBO = requireTenant(tenantId, ruleStateService.getById(id));
            return R.ok(ruleStateBuilder.buildVOByBO(entityBO));
        }));
    }

    /**
     * Page through the runtime states of rules owned by the current tenant.
     *
     * @param entityQuery optional filter query (enable flag, rule id, etc.); treated as empty when null
     * @return a page of RuleStateVO matching the query
     */
    @PreAuthorize("@perm.can('rule_state', 'list')")
    @Operation(summary = "List Rule States", description = "Page through the runtime states of rules (enabled/disabled, last-fired, counters) for the current tenant. Use to survey which rules are active and how often each has fired.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<RuleStateVO>>> list(@RequestBody(required = false) RuleStateQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleStateQuery query = Objects.isNull(entityQuery) ? new RuleStateQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<RuleStateBO> entityPageBO = ruleStateService.list(query);
            return R.ok(ruleStateBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}

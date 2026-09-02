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
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.query.RuleQuery;
import io.github.pnoker.common.data.entity.vo.RuleVO;
import io.github.pnoker.common.data.service.RuleService;
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

/** Reactive tenant-scoped alarm rule controller. */
@Tag(name = "rule", description = "Alarm and automation rule definitions")
@RestController
@RequestMapping(DataConstant.RULE_URL_PREFIX)
@RequiredArgsConstructor
public class RuleController implements BaseController {

    private final RuleBuilder ruleBuilder;
    private final RuleService ruleService;

    @PreAuthorize("@perm.can('rule', 'add')")
    @Operation(
            summary = "Add Rule",
            description = "Create a rule for the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/add")
    public Mono<RuleVO> add(@Validated(Add.class) @RequestBody RuleVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    Long tenantId = tuple.getT1().getT1();
                    RuleBO entityBO = ruleBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tenantId);
                    entityBO.setCreatorId(tuple.getT1().getT2());
                    entityBO.setCreatorName(tuple.getT2());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return ruleService.add(entityBO).map(ruleBuilder::buildVOByBO);
                });
    }

    @PreAuthorize("@perm.can('rule', 'delete')")
    @Operation(
            summary = "Delete Rule",
            description = "Delete one rule owned by the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                        @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    public Mono<Void> delete(@Parameter(description = "Tenant-owned rule ID") @NotNull @RequestParam("id") Long id) {
        return getTenantId()
                .flatMap(tenantId -> ruleService.delete(tenantId, id).then());
    }

    @PreAuthorize("@perm.can('rule', 'update')")
    @Operation(
            summary = "Update Rule",
            description = "Update one rule owned by the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/update")
    public Mono<RuleVO> update(@Validated(Update.class) @RequestBody RuleVO entityVO) {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    RuleBO entityBO = ruleBuilder.buildBOByVO(entityVO);
                    entityBO.setTenantId(tuple.getT1().getT1());
                    entityBO.setOperatorId(tuple.getT1().getT2());
                    entityBO.setOperatorName(tuple.getT2());
                    return ruleService.update(entityBO).map(ruleBuilder::buildVOByBO);
                });
    }

    @PreAuthorize("@perm.can('rule', 'get')")
    @Operation(
            summary = "Get Rule by ID",
            description = "Return one tenant-owned rule.",
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
    public Mono<RuleVO> getById(@Parameter(description = "Tenant-owned rule ID") @NotNull @RequestParam("id") Long id) {
        return getTenantId()
                .flatMap(tenantId -> ruleService.getById(tenantId, id).map(ruleBuilder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('rule', 'list')")
    @Operation(
            summary = "List Rules",
            description = "List tenant-owned rules with offset pagination.",
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
    public Mono<OffsetPage<RuleVO>> list(@RequestBody(required = false) RuleQuery entityQuery) {
        return getTenantId()
                .flatMap(tenantId -> ruleService
                        .list(tenantId, entityQuery)
                        .map(page -> OffsetPage.of(
                                page.items().stream()
                                        .map(ruleBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total())));
    }
}

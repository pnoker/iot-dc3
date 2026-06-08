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
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
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
@Slf4j
@RestController
@RequestMapping(DataConstant.RULE_URL_PREFIX)
@RequiredArgsConstructor
public class RuleController implements BaseController {

    private final RuleBuilder ruleBuilder;

    private final RuleService ruleService;

    @PreAuthorize("@perm.can('rule', 'add')")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody RuleVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleBO entityBO = ruleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            ruleService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('rule', 'delete')")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, ruleService.getById(id));
            ruleService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('rule', 'update')")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody RuleVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleBO entityBO = ruleBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, ruleService.getById(entityBO.getId()));
            ruleService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('rule', 'get')")
    @GetMapping("/get_by_id")
    public Mono<R<RuleVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            RuleBO entityBO = requireTenant(tenantId, ruleService.getById(id));
            return R.ok(ruleBuilder.buildVOByBO(entityBO));
        }));
    }

    @PreAuthorize("@perm.can('rule', 'list')")
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

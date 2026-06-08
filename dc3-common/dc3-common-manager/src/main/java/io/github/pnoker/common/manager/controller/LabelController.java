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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.dal.entity.bo.LabelBO;
import io.github.pnoker.common.dal.entity.builder.LabelBuilder;
import io.github.pnoker.common.dal.entity.query.LabelQuery;
import io.github.pnoker.common.dal.entity.vo.LabelVO;
import io.github.pnoker.common.dal.service.LabelService;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing label management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "label", description = "Label")
@Slf4j
@RestController
@RequestMapping(ManagerConstant.LABEL_URL_PREFIX)
@RequiredArgsConstructor
public class LabelController implements BaseController {

    private final LabelBuilder labelBuilder;

    private final LabelService labelService;

    /**
     * @param entityVO {@link LabelVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('label', 'add')")
    @Operation(summary = "新增标签绑定", description = "新增一条标签绑定记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody LabelVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = labelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            labelService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return R of String
     */
    @PreAuthorize("@perm.can('label', 'delete')")
    @Operation(summary = "删除标签绑定", description = "删除指定ID的标签绑定")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, labelService.getById(id));
            labelService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * @param entityVO {@link LabelVO}
     * @return R of String
     */
    @PreAuthorize("@perm.can('label', 'update')")
    @Operation(summary = "更新标签绑定", description = "更新标签绑定信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody LabelVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = labelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, labelService.getById(entityBO.getId()));
            labelService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return LabelVO {@link LabelVO}
     */
    @PreAuthorize("@perm.can('label', 'get')")
    @Operation(summary = "查询标签绑定", description = "根据ID查询标签绑定详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<LabelVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = requireTenant(tenantId, labelService.getById(id));
            LabelVO entityVO = labelBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * @param entityQuery {@link LabelQuery}
     * @return R Of LabelVO Page
     */
    @PreAuthorize("@perm.can('label', 'list')")
    @Operation(summary = "查询标签绑定列表", description = "分页查询标签绑定列表")
    @PostMapping("/list")
    public Mono<R<Page<LabelVO>>> list(@RequestBody(required = false) LabelQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelQuery query = Objects.isNull(entityQuery) ? new LabelQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<LabelBO> entityPageBO = labelService.list(query);
            Page<LabelVO> entityPageVO = labelBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}

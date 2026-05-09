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
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Controller
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.LABEL_URL_PREFIX)
public class LabelController implements BaseController {

    private final LabelBuilder labelBuilder;

    private final LabelService labelService;

    public LabelController(LabelBuilder labelBuilder, LabelService labelService) {
        this.labelBuilder = labelBuilder;
        this.labelService = labelService;
    }

    /**
     * @param entityVO {@link LabelVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody LabelVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = labelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            labelService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, labelService.selectById(id));
            labelService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    /**
     * @param entityVO {@link LabelVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody LabelVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = labelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, labelService.selectById(entityBO.getId()));
            labelService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * @param id ID
     * @return LabelVO {@link LabelVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<LabelVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelBO entityBO = requireTenant(tenantId, labelService.selectById(id));
            LabelVO entityVO = labelBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * @param entityQuery {@link LabelQuery}
     * @return R Of LabelVO Page
     */
    @PostMapping("/list")
    public Mono<R<Page<LabelVO>>> list(@RequestBody(required = false) LabelQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            LabelQuery query = Objects.isNull(entityQuery) ? new LabelQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<LabelBO> entityPageBO = labelService.selectByPage(query);
            Page<LabelVO> entityPageVO = labelBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}

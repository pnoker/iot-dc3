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

package io.github.pnoker.common.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.builder.ApiBuilder;
import io.github.pnoker.common.auth.entity.query.ApiQuery;
import io.github.pnoker.common.auth.entity.vo.ApiVO;
import io.github.pnoker.common.auth.service.ApiService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * REST controller exposing API management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.API_URL_PREFIX)
@RequiredArgsConstructor
public class ApiController implements BaseController {

    private final ApiBuilder apiBuilder;

    private final ApiService apiService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ApiVO entityVO) {
        return async(() -> {
            ApiBO entityBO = apiBuilder.buildBOByVO(entityVO);
            apiService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        });
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            apiService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ApiVO entityVO) {
        return async(() -> {
            ApiBO entityBO = apiBuilder.buildBOByVO(entityVO);
            apiService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        });
    }

    @GetMapping("/get_by_id")
    public Mono<R<ApiVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            ApiBO entityBO = apiService.getById(id);
            ApiVO entityVO = apiBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    @PostMapping("/list")
    public Mono<R<Page<ApiVO>>> list(@RequestBody(required = false) ApiQuery entityQuery) {
        return async(() -> {
            ApiQuery query = Objects.isNull(entityQuery) ? new ApiQuery() : entityQuery;
            Page<ApiBO> entityPageBO = apiService.list(query);
            Page<ApiVO> entityPageVO = apiBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        });
    }

}

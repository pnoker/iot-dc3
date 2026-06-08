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
 * REST controller exposing API management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "api", description = "API接口")
@Slf4j
@RestController
@RequestMapping(AuthConstant.API_URL_PREFIX)
@RequiredArgsConstructor
public class ApiController implements BaseController {

    private final ApiBuilder apiBuilder;

    private final ApiService apiService;

    @PreAuthorize("@perm.can('api', 'add')")
    @Operation(summary = "新增API接口管理", description = "新增一条API接口管理记录")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ApiVO entityVO) {
        // TODO: RBAC — restrict to administrator role. APIs are system-global entities managed by platform admins.
        return async(() -> {
            ApiBO entityBO = apiBuilder.buildBOByVO(entityVO);
            apiService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        });
    }

    @PreAuthorize("@perm.can('api', 'delete')")
    @Operation(summary = "删除API接口管理", description = "删除指定ID的API接口管理")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        // TODO: RBAC — restrict to administrator role. APIs are system-global entities managed by platform admins.
        return async(() -> {
            apiService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    @PreAuthorize("@perm.can('api', 'update')")
    @Operation(summary = "更新API接口管理", description = "更新API接口管理信息")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ApiVO entityVO) {
        // TODO: RBAC — restrict to administrator role. APIs are system-global entities managed by platform admins.
        return async(() -> {
            ApiBO entityBO = apiBuilder.buildBOByVO(entityVO);
            apiService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        });
    }

    @PreAuthorize("@perm.can('api', 'get')")
    @Operation(summary = "查询API接口管理", description = "根据ID查询API接口管理详细信息")
    @GetMapping("/get_by_id")
    public Mono<R<ApiVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        // Read access to global API data is open to all authenticated users.
        return async(() -> {
            ApiBO entityBO = apiService.getById(id);
            ApiVO entityVO = apiBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    @PreAuthorize("@perm.can('api', 'list')")
    @Operation(summary = "查询API接口管理列表", description = "分页查询API接口管理列表")
    @PostMapping("/list")
    public Mono<R<Page<ApiVO>>> list(@RequestBody(required = false) ApiQuery entityQuery) {
        // Read access to global API data is open to all authenticated users.
        return async(() -> {
            ApiQuery query = Objects.isNull(entityQuery) ? new ApiQuery() : entityQuery;
            Page<ApiBO> entityPageBO = apiService.list(query);
            Page<ApiVO> entityPageVO = apiBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        });
    }

}

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
import io.github.pnoker.common.auth.security.AdminChecker;
import io.github.pnoker.common.auth.service.ApiService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
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
 * REST controller exposing API management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "api", description = "API endpoint registry: manage metadata for REST API endpoints including path, method, auth requirements, and documentation references")
@Slf4j
@RestController
@RequestMapping(AuthConstant.API_URL_PREFIX)
@RequiredArgsConstructor
public class ApiController implements BaseController {

    private final ApiBuilder apiBuilder;

    private final ApiService apiService;

    private final AdminChecker adminChecker;

    @PreAuthorize("@perm.can('api', 'add')")
    @Operation(summary = "Add API Endpoint", description = "Register a new HTTP API endpoint entry that feeds the permission tree and the MCP tool catalog. Restricted to system admins; returns an add-success result.")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody ApiVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            ApiBO entityBO = apiBuilder.buildBOByVO(entityVO);
            apiService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('api', 'delete')")
    @Operation(summary = "Delete API Endpoint", description = "Remove a registered API endpoint by its ID so it no longer appears in the permission tree or tool catalog. Restricted to system admins.")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            apiService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('api', 'update')")
    @Operation(summary = "Update API Endpoint", description = "Modify an existing registered API endpoint's metadata. Restricted to system admins; returns an update-success result.")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody ApiVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            ApiBO entityBO = apiBuilder.buildBOByVO(entityVO);
            apiService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('api', 'get')")
    @Operation(summary = "Get API Endpoint by ID", description = "Fetch one registered API endpoint by its ID. Read access is open to all authenticated users; returns the full API endpoint detail.")
    @GetMapping("/get_by_id")
    public Mono<R<ApiVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        // Read access to global API data is open to all authenticated users.
        return async(() -> {
            ApiBO entityBO = apiService.getById(id);
            ApiVO entityVO = apiBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    @PreAuthorize("@perm.can('api', 'list')")
    @Operation(summary = "List API Endpoints", description = "Page through registered API endpoints with filters from the query body. Read access is open to all authenticated users; returns a page of API endpoints.")
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

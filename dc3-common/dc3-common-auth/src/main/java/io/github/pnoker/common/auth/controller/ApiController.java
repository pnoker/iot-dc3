/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.builder.ApiBuilder;
import io.github.pnoker.common.auth.entity.query.ApiOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.ApiVO;
import io.github.pnoker.common.auth.repository.ApiFilter;
import io.github.pnoker.common.auth.security.ReactiveAdminChecker;
import io.github.pnoker.common.auth.service.ReactiveApiService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** REST endpoints exposing the global API registry. */
@Tag(name = "api", description = "API endpoint registry: manage metadata for REST API endpoints including path, method, auth requirements, and documentation references")
@RestController
@RequestMapping(AuthConstant.API_URL_PREFIX)
@RequiredArgsConstructor
public class ApiController implements BaseController {

    private final ApiBuilder apiBuilder;
    private final ReactiveApiService apiService;
    private final ReactiveAdminChecker adminChecker;

    @PreAuthorize("@perm.can('api', 'add')")
    @Operation(summary = "Add API Endpoint", description = "Register a new HTTP API endpoint entry and return the persisted representation. Restricted to system admins.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "false"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @PostMapping("/add")
    public Mono<ResponseEntity<ApiVO>> add(@Validated(Add.class) @RequestBody ApiVO entityVO) {
        return getPrincipalHeader().flatMap(header -> adminChecker.assertSystemAdmin(header.getTenantId())
                .then(Mono.defer(() -> {
                    ApiBO api = apiBuilder.buildBOByVO(entityVO);
                    api.setCreatorId(header.getUserId());
                    api.setCreatorName(header.getNickName());
                    api.setOperatorId(header.getUserId());
                    api.setOperatorName(header.getNickName());
                    return apiService.add(api);
                }))
                .map(saved -> ResponseEntity.status(201).body(apiBuilder.buildVOByBO(saved))));
    }

    @PreAuthorize("@perm.can('api', 'delete')")
    @Operation(summary = "Delete API Endpoint", description = "Remove a registered API endpoint by its ID. Restricted to system admins.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "HIGH"), @ExtensionProperty(name = "destructive", value = "true"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(@Parameter(description = "API identifier owned by the global registry")
                                             @NotNull @RequestParam("id") Long id) {
        return getPrincipalHeader().flatMap(header -> adminChecker.assertSystemAdmin(header.getTenantId())
                .then(apiService.delete(id, header.getUserId(), header.getNickName())))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PreAuthorize("@perm.can('api', 'update')")
    @Operation(summary = "Update API Endpoint", description = "Replace an existing registered API endpoint definition and return the persisted representation. Restricted to system admins.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @PostMapping("/update")
    public Mono<ResponseEntity<ApiVO>> update(@Validated(Update.class) @RequestBody ApiVO entityVO) {
        return getPrincipalHeader().flatMap(header -> adminChecker.assertSystemAdmin(header.getTenantId())
                .then(Mono.defer(() -> {
                    ApiBO api = apiBuilder.buildBOByVO(entityVO);
                    api.setOperatorId(header.getUserId());
                    api.setOperatorName(header.getNickName());
                    return apiService.update(api);
                }))
                .map(saved -> ResponseEntity.ok(apiBuilder.buildVOByBO(saved))));
    }

    @PreAuthorize("@perm.can('api', 'get')")
    @Operation(summary = "Get API Endpoint by ID", description = "Fetch one registered API endpoint by identifier.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @GetMapping("/get_by_id")
    public Mono<ResponseEntity<ApiVO>> getById(@Parameter(description = "API identifier")
                                               @NotNull @RequestParam("id") Long id) {
        return apiService.getById(id).map(api -> ResponseEntity.ok(apiBuilder.buildVOByBO(api)));
    }

    @PreAuthorize("@perm.can('api', 'list')")
    @Operation(summary = "List API Endpoints", description = "List registered API endpoints with deterministic zero-based offset pagination.", extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @PostMapping("/list")
    public Mono<ResponseEntity<OffsetPage<ApiVO>>> list(@RequestBody(required = false) ApiOffsetRequest request) {
        ApiOffsetRequest query = request == null ? new ApiOffsetRequest() : request;
        ApiFilter filter = new ApiFilter(query.serviceName(), query.apiTypeFlag(), query.apiName(), query.apiCode(),
                query.apiGroup(), query.enableFlag(), new PageRequest(query.offset(), query.limit(), query.sort()));
        return apiService.list(filter)
                .map(page -> ResponseEntity.ok(OffsetPage.of(page.items().stream().map(apiBuilder::buildVOByBO).toList(),
                        page.offset(), page.limit(), page.total())));
    }
}

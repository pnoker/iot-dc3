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

import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.bo.MenuTreeBO;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.builder.MenuBuilder;
import io.github.pnoker.common.auth.entity.query.MenuOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.MenuTreeVO;
import io.github.pnoker.common.auth.entity.vo.MenuVO;
import io.github.pnoker.common.auth.repository.MenuFilter;
import io.github.pnoker.common.auth.security.ReactiveAdminChecker;
import io.github.pnoker.common.auth.service.ReactiveMenuService;
import io.github.pnoker.common.auth.service.ReactiveRoleResourceBindService;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

/** REST endpoints exposing the global navigation menu registry. */
@Tag(name = "menu", description = "Navigation menu hierarchy and routing configuration")
@RestController
@RequestMapping(AuthConstant.MENU_URL_PREFIX)
@RequiredArgsConstructor
public class MenuController implements BaseController {

    private final MenuBuilder menuBuilder;
    private final ReactiveMenuService menuService;
    private final ReactiveAdminChecker adminChecker;
    private final ReactiveRoleResourceBindService roleResourceBindService;

    @PreAuthorize("@perm.can('menu', 'add')")
    @Operation(
            summary = "Add Menu",
            description = "Create a menu node and return the persisted representation.",
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
    public Mono<ResponseEntity<MenuVO>> add(@Validated(Add.class) @RequestBody MenuVO entityVO) {
        return getPrincipalHeader()
                .flatMap(header -> adminChecker
                        .assertSystemAdmin(header.getTenantId())
                        .then(Mono.defer(() -> {
                            MenuBO menu = menuBuilder.buildBOByVO(entityVO);
                            menu.setCreatorId(header.getUserId());
                            menu.setCreatorName(header.getNickName());
                            menu.setOperatorId(header.getUserId());
                            menu.setOperatorName(header.getNickName());
                            return menuService.add(menu);
                        }))
                        .map(saved -> ResponseEntity.status(201).body(menuBuilder.buildVOByBO(saved))));
    }

    @PreAuthorize("@perm.can('menu', 'delete')")
    @Operation(
            summary = "Delete Menu",
            description = "Delete a leaf menu node.",
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
    public Mono<ResponseEntity<Void>> delete(
            @Parameter(description = "Menu identifier") @NotNull @RequestParam("id") Long id) {
        return getPrincipalHeader()
                .flatMap(header -> adminChecker
                        .assertSystemAdmin(header.getTenantId())
                        .then(menuService.delete(id, header.getUserId(), header.getNickName())))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PreAuthorize("@perm.can('menu', 'update')")
    @Operation(
            summary = "Update Menu",
            description = "Replace a menu definition and return the persisted representation.",
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
    public Mono<ResponseEntity<MenuVO>> update(@Validated(Update.class) @RequestBody MenuVO entityVO) {
        return getPrincipalHeader()
                .flatMap(header -> adminChecker
                        .assertSystemAdmin(header.getTenantId())
                        .then(Mono.defer(() -> {
                            MenuBO menu = menuBuilder.buildBOByVO(entityVO);
                            menu.setOperatorId(header.getUserId());
                            menu.setOperatorName(header.getNickName());
                            return menuService.update(menu);
                        }))
                        .map(saved -> ResponseEntity.ok(menuBuilder.buildVOByBO(saved))));
    }

    @PreAuthorize("@perm.can('menu', 'get')")
    @Operation(
            summary = "Get Menu by ID",
            description = "Fetch one menu node by identifier.",
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
    public Mono<ResponseEntity<MenuVO>> getById(
            @Parameter(description = "Menu identifier") @NotNull @RequestParam("id") Long id) {
        return menuService.getById(id).map(menu -> ResponseEntity.ok(menuBuilder.buildVOByBO(menu)));
    }

    @PreAuthorize("@perm.can('menu', 'list')")
    @Operation(
            summary = "List Menus",
            description = "List menus with deterministic zero-based offset pagination.",
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
    public Mono<ResponseEntity<OffsetPage<MenuVO>>> list(@RequestBody(required = false) MenuOffsetRequest request) {
        MenuOffsetRequest query = request == null ? new MenuOffsetRequest() : request;
        return menuService
                .list(filter(query, new PageRequest(query.offset(), query.limit(), query.sort())))
                .map(page -> ResponseEntity.ok(OffsetPage.of(
                        page.items().stream().map(menuBuilder::buildVOByBO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total())));
    }

    @PreAuthorize("@perm.can('menu', 'list')")
    @Operation(
            summary = "List Menu Tree",
            description =
                    "Return the complete menu hierarchy pruned by the current principal's granted menu resources.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                        @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                        @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list_tree")
    public Mono<ResponseEntity<List<MenuTreeVO>>> listTree(@RequestBody(required = false) MenuOffsetRequest request) {
        MenuOffsetRequest query = request == null ? new MenuOffsetRequest() : request;
        MenuFilter filter = filter(query, new PageRequest(0, PageRequest.MAX_LIMIT, query.sort()));
        return getPrincipalHeader()
                .flatMap(header -> menuService
                        .listTree(filter)
                        .collectList()
                        .flatMap(nodes ->
                                filterByPrincipalMenuResources(nodes, header.getPrincipalId(), header.getTenantId())))
                .map(nodes -> ResponseEntity.ok(
                        nodes.stream().map(menuBuilder::buildTreeVOByBO).toList()));
    }

    private MenuFilter filter(MenuOffsetRequest query, PageRequest page) {
        return new MenuFilter(
                query.parentMenuId(),
                query.menuTypeFlag(),
                query.menuName(),
                query.menuCode(),
                query.enableFlag(),
                page);
    }

    private Mono<List<MenuTreeBO>> filterByPrincipalMenuResources(
            List<MenuTreeBO> nodes, Long principalId, Long tenantId) {
        return roleResourceBindService
                .listResourcesByPrincipal(tenantId, principalId)
                .map(ResourceBO::getResourceCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .map(visibleMenuCodes -> {
                    if (visibleMenuCodes.contains("*")) return nodes;
                    return nodes.stream()
                            .map(node -> retainAccessibleMenuNode(node, visibleMenuCodes))
                            .filter(Objects::nonNull)
                            .toList();
                });
    }

    private MenuTreeBO retainAccessibleMenuNode(MenuTreeBO node, Set<String> visibleMenuCodes) {
        List<MenuTreeBO> children = Objects.requireNonNullElse(node.getChildren(), List.<MenuTreeBO>of()).stream()
                .map(child -> retainAccessibleMenuNode(child, visibleMenuCodes))
                .filter(Objects::nonNull)
                .toList();
        boolean selfVisible = visibleMenuCodes.contains("menu:" + Objects.requireNonNullElse(node.getMenuCode(), ""));
        if (!selfVisible && children.isEmpty()) return null;
        node.setChildren(children);
        return node;
    }
}

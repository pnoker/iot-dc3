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
import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.bo.MenuTreeBO;
import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.builder.MenuBuilder;
import io.github.pnoker.common.auth.entity.query.MenuQuery;
import io.github.pnoker.common.auth.entity.vo.MenuTreeVO;
import io.github.pnoker.common.auth.entity.vo.MenuVO;
import io.github.pnoker.common.auth.security.AdminChecker;
import io.github.pnoker.common.auth.service.MenuService;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.SuccessCode;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller exposing menu management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "menu", description = "Navigation menu hierarchy: manage menu trees, items, and routing configurations that define the platform user interface navigation structure")
@Slf4j
@RestController
@RequestMapping(AuthConstant.MENU_URL_PREFIX)
@RequiredArgsConstructor
public class MenuController implements BaseController {

    private final MenuBuilder menuBuilder;

    private final MenuService menuService;

    private final AdminChecker adminChecker;

    private final RoleResourceBindService roleResourceBindService;

    /**
     * Create a new menu node in the settings-sidebar tree.
     *
     * @param entityVO menu payload to create (i18n titles, icon, frontend URL)
     * @return add-success status; requires the system admin role
     */
    @PreAuthorize("@perm.can('menu', 'add')")
    @Operation(summary = "Add Menu", description = "Create a new menu node in the settings-sidebar tree. A menu carries i18n titles, an icon and a frontend URL; requires the system admin role. Returns the new menu ID.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "false"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody MenuVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            MenuBO entityBO = menuBuilder.buildBOByVO(entityVO);
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            menuService.add(entityBO);
            return R.ok(SuccessCode.ADD);
        }));
    }

    /**
     * Delete a menu node by its ID.
     *
     * @param id id of the menu to delete
     * @return delete-success status; requires the system admin role for the owning tenant
     */
    @PreAuthorize("@perm.can('menu', 'delete')")
    @Operation(summary = "Delete Menu", description = "Delete a menu node by its ID. Removes the settings-sidebar entry; requires the system admin role for the owning tenant.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            menuService.delete(id);
            return R.ok(SuccessCode.DELETE);
        }));
    }

    /**
     * Update an existing menu node's label, icon, URL, parent or display order.
     *
     * @param entityVO menu payload to update
     * @return update-success status; requires the system admin role
     */
    @PreAuthorize("@perm.can('menu', 'update')")
    @Operation(summary = "Update Menu", description = "Update an existing menu node's label, icon, URL, parent or display order. Requires the system admin role; returns the updated menu.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody MenuVO entityVO) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            MenuBO entityBO = menuBuilder.buildBOByVO(entityVO);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            menuService.update(entityBO);
            return R.ok(SuccessCode.UPDATE);
        }));
    }

    /**
     * Fetch one menu node by ID.
     *
     * @param id id of the menu to fetch
     * @return the matched MenuVO; menu data is global and readable by any authenticated user
     */
    @PreAuthorize("@perm.can('menu', 'get')")
    @Operation(summary = "Get Menu by ID", description = "Fetch one menu node by ID, including its i18n titles, icon, URL and parent. Menu data is global and readable by any authenticated user.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @GetMapping("/get_by_id")
    public Mono<R<MenuVO>> getById(@Parameter(description = "Primary key of the target record; must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        // Read access to global menu data is open to all authenticated users.
        return async(() -> {
            MenuBO entityBO = menuService.getById(id);
            MenuVO entityVO = menuBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * Page through menu nodes with filters such as code, parent and enable flag.
     *
     * @param entityQuery optional filter criteria; an empty query pages all menus
     * @return a flat page of MenuVO matching the query
     */
    @PreAuthorize("@perm.can('menu', 'list')")
    @Operation(summary = "List Menus", description = "Page through menu nodes with filters such as code, parent and enable flag. Returns a flat page of menus; use for browsing or selecting a target menu.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<R<Page<MenuVO>>> list(@RequestBody(required = false) MenuQuery entityQuery) {
        // Read access to global menu data is open to all authenticated users.
        return async(() -> {
            MenuQuery query = Objects.isNull(entityQuery) ? new MenuQuery() : entityQuery;
            Page<MenuBO> entityPageBO = menuService.list(query);
            Page<MenuVO> entityPageVO = menuBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        });
    }

    /**
     * Return menus as a nested tree pruned to the nodes the current principal can access.
     *
     * @param entityQuery optional filter criteria; an empty query includes the full menu tree before pruning
     * @return a tree of MenuTreeVO retaining only nodes the principal's granted resources cover
     */
    @PreAuthorize("@perm.can('menu', 'list')")
    @Operation(summary = "List Menu Tree", description = "Return menus as a nested tree and prune it to the nodes the current principal can access. " +
            "A node is retained only when the principal's granted resources include 'menu:<code>' (or the '*' wildcard); use to render the user's settings sidebar.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list_tree")
    public Mono<R<List<MenuTreeVO>>> listTree(@RequestBody(required = false) MenuQuery entityQuery) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            List<MenuTreeBO> entityBOList = menuService.listTree(entityQuery);
            entityBOList = filterByPrincipalMenuResources(entityBOList, header.getPrincipalId(), header.getTenantId());
            return R.ok(menuBuilder.buildTreeVOListByBOList(entityBOList));
        }));
    }

    /**
     * Filter the menu tree to nodes the principal can access. A wildcard resource code
     * grants the full tree; otherwise each node is pruned by its {@code menu:<code>}
     * resource, keeping a parent when any child remains visible.
     *
     * @param nodes       the full menu tree
     * @param principalId the principal to filter for
     * @param tenantId    tenant scope
     * @return the access-filtered menu tree
     */
    private List<MenuTreeBO> filterByPrincipalMenuResources(List<MenuTreeBO> nodes, Long principalId, Long tenantId) {
        List<ResourceBO> resources = roleResourceBindService.listResourceByPrincipalId(principalId, tenantId);
        Set<String> visibleMenuCodes = resources.stream()
                .map(ResourceBO::getResourceCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (visibleMenuCodes.contains("*")) {
            return nodes;
        }
        return nodes.stream()
                .map(node -> retainAccessibleMenuNode(node, visibleMenuCodes))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Recursively retain a menu node when it or any descendant is visible, dropping
     * leaf nodes the principal cannot access.
     *
     * @param node              the node to evaluate
     * @param visibleMenuCodes  the set of visible {@code menu:<code>} codes
     * @return the retained node, or null when neither it nor any child is visible
     */
    private MenuTreeBO retainAccessibleMenuNode(MenuTreeBO node, Set<String> visibleMenuCodes) {
        List<MenuTreeBO> children = Objects.requireNonNullElse(node.getChildren(), List.<MenuTreeBO>of())
                .stream()
                .map(child -> retainAccessibleMenuNode(child, visibleMenuCodes))
                .filter(Objects::nonNull)
                .toList();
        boolean selfVisible = visibleMenuCodes.contains("menu:" + Objects.requireNonNullElse(node.getMenuCode(), ""));
        if (!selfVisible && children.isEmpty()) {
            return null;
        }
        node.setChildren(children);
        return node;
    }

}

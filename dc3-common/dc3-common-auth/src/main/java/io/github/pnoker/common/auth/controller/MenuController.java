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
import io.github.pnoker.common.auth.service.MenuService;
import io.github.pnoker.common.auth.security.AdminChecker;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing menu management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Tag(name = "menu", description = "Menus")
@Slf4j
@RestController
@RequestMapping(AuthConstant.MENU_URL_PREFIX)
@RequiredArgsConstructor
public class MenuController implements BaseController {

    private final MenuBuilder menuBuilder;

    private final MenuService menuService;

    private final AdminChecker adminChecker;

    private final RoleResourceBindService roleResourceBindService;

    @PreAuthorize("@perm.can('menu', 'add')")
    @Operation(summary = "Add Menu", description = "Create a menu record")
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody MenuVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            MenuBO entityBO = menuBuilder.buildBOByVO(entityVO);
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            menuService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('menu', 'delete')")
    @Operation(summary = "Delete Menu", description = "Delete a menu record by ID")
    @PostMapping("/delete")
    public Mono<R<String>> delete(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        return getUserHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            menuService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('menu', 'update')")
    @Operation(summary = "Update Menu", description = "Update a menu record")
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody MenuVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            adminChecker.assertSystemAdmin(header.getTenantId());
            MenuBO entityBO = menuBuilder.buildBOByVO(entityVO);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            menuService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @PreAuthorize("@perm.can('menu', 'get')")
    @Operation(summary = "Get Menu by ID", description = "Get menu details by ID")
    @GetMapping("/get_by_id")
    public Mono<R<MenuVO>> getById(@Parameter(description = "Record ID") @NotNull @RequestParam(value = "id") Long id) {
        // Read access to global menu data is open to all authenticated users.
        return async(() -> {
            MenuBO entityBO = menuService.getById(id);
            MenuVO entityVO = menuBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    @PreAuthorize("@perm.can('menu', 'list')")
    @Operation(summary = "List Menus", description = "List menus with pagination")
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

    @PreAuthorize("@perm.can('menu', 'list')")
    @Operation(summary = "List Menu Tree", description = "List menus as a permission-filtered tree")
    @PostMapping("/list_tree")
    public Mono<R<List<MenuTreeVO>>> listTree(@RequestBody(required = false) MenuQuery entityQuery) {
        return getUserHeader().flatMap(header -> async(() -> {
            List<MenuTreeBO> entityBOList = menuService.listTree(entityQuery);
            entityBOList = filterByUserMenuResources(entityBOList, header.getUserId(), header.getTenantId());
            return R.ok(menuBuilder.buildTreeVOListByBOList(entityBOList));
        }));
    }

    private List<MenuTreeBO> filterByUserMenuResources(List<MenuTreeBO> nodes, Long userId, Long tenantId) {
        List<ResourceBO> resources = roleResourceBindService.listResourceByUserId(userId, tenantId);
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

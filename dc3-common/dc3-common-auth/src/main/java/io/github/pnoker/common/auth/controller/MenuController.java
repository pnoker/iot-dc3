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

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.bo.MenuTreeBO;
import io.github.pnoker.common.auth.entity.builder.MenuBuilder;
import io.github.pnoker.common.auth.entity.query.MenuQuery;
import io.github.pnoker.common.auth.entity.vo.MenuTreeVO;
import io.github.pnoker.common.auth.entity.vo.MenuVO;
import io.github.pnoker.common.auth.service.MenuService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * REST controller exposing menu management endpoints.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(AuthConstant.MENU_URL_PREFIX)
@RequiredArgsConstructor
public class MenuController implements BaseController {

    private final MenuBuilder menuBuilder;

    private final MenuService menuService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody MenuVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            MenuBO entityBO = menuBuilder.buildBOByVO(entityVO);
            entityBO.setCreatorId(header.getUserId());
            entityBO.setCreatorName(header.getNickName());
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            menuService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            menuService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody MenuVO entityVO) {
        return getUserHeader().flatMap(header -> async(() -> {
            MenuBO entityBO = menuBuilder.buildBOByVO(entityVO);
            entityBO.setOperatorId(header.getUserId());
            entityBO.setOperatorName(header.getNickName());
            menuService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @GetMapping("/get_by_id")
    public Mono<R<MenuVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            MenuBO entityBO = menuService.getById(id);
            MenuVO entityVO = menuBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    @PostMapping("/list")
    public Mono<R<Page<MenuVO>>> list(@RequestBody(required = false) MenuQuery entityQuery) {
        return async(() -> {
            MenuQuery query = Objects.isNull(entityQuery) ? new MenuQuery() : entityQuery;
            Page<MenuBO> entityPageBO = menuService.list(query);
            Page<MenuVO> entityPageVO = menuBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        });
    }

    @PostMapping("/list_tree")
    public Mono<R<List<MenuTreeVO>>> listTree(@RequestBody(required = false) MenuQuery entityQuery) {
        return async(() -> {
            List<MenuTreeBO> entityBOList = menuService.listTree(entityQuery);
            List<MenuTreeVO> entityVOList = new ArrayList<>(entityBOList.size());
            for (MenuTreeBO node : entityBOList) {
                entityVOList.add(toTreeVO(node));
            }
            return R.ok(entityVOList);
        });
    }

    private MenuTreeVO toTreeVO(MenuTreeBO node) {
        MenuVO flat = menuBuilder.buildVOByBO(node);
        MenuTreeVO out = new MenuTreeVO();
        out.setId(flat.getId());
        out.setParentMenuId(flat.getParentMenuId());
        out.setMenuTypeFlag(flat.getMenuTypeFlag());
        out.setMenuName(flat.getMenuName());
        out.setMenuCode(flat.getMenuCode());
        out.setMenuLevel(flat.getMenuLevel());
        out.setMenuIndex(flat.getMenuIndex());
        out.setMenuExt(flat.getMenuExt());
        out.setEnableFlag(flat.getEnableFlag());
        out.setRemark(flat.getRemark());
        out.setCreatorId(flat.getCreatorId());
        out.setCreatorName(flat.getCreatorName());
        out.setCreateTime(flat.getCreateTime());
        out.setOperatorId(flat.getOperatorId());
        out.setOperatorName(flat.getOperatorName());
        out.setOperateTime(flat.getOperateTime());
        if (Objects.nonNull(node.getChildren())) {
            List<MenuTreeVO> childVOs = new ArrayList<>(node.getChildren().size());
            for (MenuTreeBO child : node.getChildren()) {
                childVOs.add(toTreeVO(child));
            }
            out.setChildren(childVOs);
        }
        return out;
    }

}

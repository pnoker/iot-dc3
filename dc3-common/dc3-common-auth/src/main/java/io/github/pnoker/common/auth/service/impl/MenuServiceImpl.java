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

package io.github.pnoker.common.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.biz.ResourceRegistrySyncService;
import io.github.pnoker.common.auth.dal.MenuManager;
import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.bo.MenuTreeBO;
import io.github.pnoker.common.auth.entity.builder.MenuBuilder;
import io.github.pnoker.common.auth.entity.model.MenuDO;
import io.github.pnoker.common.auth.entity.query.MenuQuery;
import io.github.pnoker.common.auth.service.MenuService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Business service implementation for menu operations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuBuilder menuBuilder;

    private final MenuManager menuManager;

    private final ResourceRegistrySyncService resourceRegistrySyncService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(MenuBO entityBO) {
        checkDuplicate(entityBO, false, true);

        MenuDO entityDO = menuBuilder.buildDOByBO(entityBO);
        if (!menuManager.save(entityDO)) {
            throw new AddException("Failed to create menu");
        }
        resourceRegistrySyncService.syncMenuResource(entityDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getDOById(id, true);

        // Refuse deletion while sub menus exist, otherwise they would be orphaned
        LambdaQueryChainWrapper<MenuDO> wrapper = menuManager.lambdaQuery().eq(MenuDO::getParentMenuId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove menu: some sub menus exists in the menu");
        }

        if (!menuManager.removeById(id)) {
            throw new DeleteException("Failed to remove menu");
        }
        resourceRegistrySyncService.removeMenuResource(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MenuBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        MenuDO entityDO = menuBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!menuManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update menu");
        }
        // Re-read to pick up any DB-side mutations (trigger-updated operate_time, etc).
        MenuDO latest = menuManager.getById(entityBO.getId());
        resourceRegistrySyncService.syncMenuResource(Objects.nonNull(latest) ? latest : entityDO);
    }

    @Override
    public MenuBO getById(Long id) {
        MenuDO entityDO = getDOById(id, true);
        return menuBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<MenuBO> list(MenuQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<MenuDO> entityPageDO = menuManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return menuBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for menu search.
     *
     * @param entityQuery {@link MenuQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link MenuDO}
     */
    private LambdaQueryWrapper<MenuDO> fuzzyQuery(MenuQuery entityQuery) {
        LambdaQueryWrapper<MenuDO> wrapper = Wrappers.<MenuDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getMenuName()), MenuDO::getMenuName, entityQuery.getMenuName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getMenuCode()), MenuDO::getMenuCode, entityQuery.getMenuCode());
        wrapper.eq(Objects.nonNull(entityQuery.getMenuTypeFlag()), MenuDO::getMenuTypeFlag,
                Objects.isNull(entityQuery.getMenuTypeFlag()) ? null : entityQuery.getMenuTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getParentMenuId()), MenuDO::getParentMenuId,
                entityQuery.getParentMenuId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), MenuDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    @Override
    public List<MenuTreeBO> listTree(MenuQuery entityQuery) {
        MenuQuery effective = Objects.requireNonNullElseGet(entityQuery, MenuQuery::new);
        List<MenuDO> rows = menuManager.list(fuzzyQuery(effective));
        return assembleTree(rows);
    }

    private List<MenuTreeBO> assembleTree(List<MenuDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return List.of();
        }
        Map<Long, MenuTreeBO> byId = new HashMap<>(rows.size());
        for (MenuDO row : rows) {
            byId.put(row.getId(), MenuTreeBO.fromBO(menuBuilder.buildBOByDO(row)));
        }
        List<MenuTreeBO> roots = new ArrayList<>();
        for (MenuTreeBO node : byId.values()) {
            Long parentId = node.getParentMenuId();
            MenuTreeBO parent = Objects.isNull(parentId) || parentId == 0L ? null : byId.get(parentId);
            if (Objects.isNull(parent)) {
                roots.add(node);
            } else {
                parent.addChild(node);
            }
        }
        Comparator<MenuTreeBO> order = Comparator
                .comparing(MenuTreeBO::getMenuIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MenuTreeBO::getMenuName, Comparator.nullsLast(Comparator.naturalOrder()));
        sortRecursive(roots, order);
        return roots;
    }

    private void sortRecursive(List<MenuTreeBO> nodes, Comparator<MenuTreeBO> order) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        nodes.sort(order);
        for (MenuTreeBO node : nodes) {
            sortRecursive(node.getChildren(), order);
        }
    }

    /**
     * Check whether a menu is duplicated by parent menu, type, name, and code.
     *
     * @param entityBO       {@link MenuBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(MenuBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<MenuDO> wrapper = Wrappers.<MenuDO>query().lambda();
        wrapper.eq(MenuDO::getParentMenuId, entityBO.getParentMenuId());
        wrapper.eq(MenuDO::getMenuTypeFlag, entityBO.getMenuTypeFlag());
        wrapper.eq(MenuDO::getMenuName, entityBO.getMenuName());
        wrapper.eq(MenuDO::getMenuCode, entityBO.getMenuCode());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        MenuDO one = menuManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Menu has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get menu data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link MenuDO} if found, otherwise {@code null} when {@code throwException}
     * is false
     */
    private MenuDO getDOById(Long id, boolean throwException) {
        MenuDO entityDO = menuManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Menu does not exist");
        }
        return entityDO;
    }

}

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
import io.github.pnoker.common.auth.dal.MenuManager;
import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.builder.MenuBuilder;
import io.github.pnoker.common.auth.entity.model.MenuDO;
import io.github.pnoker.common.auth.entity.query.MenuQuery;
import io.github.pnoker.common.auth.service.MenuService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * Menu Service Impl
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    @Resource
    private MenuBuilder menuBuilder;

    @Resource
    private MenuManager menuManager;

    @Override
    public void save(MenuBO entityBO) {
        checkDuplicate(entityBO, false, true);

        MenuDO entityDO = menuBuilder.buildDOByBO(entityBO);
        if (!menuManager.save(entityDO)) {
            throw new AddException("Failed to create menu");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 
        LambdaQueryChainWrapper<MenuDO> wrapper = menuManager.lambdaQuery().eq(MenuDO::getParentMenuId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove menu: some sub menus exists in the menu");
        }

        if (!menuManager.removeById(id)) {
            throw new DeleteException("Failed to remove menu");
        }
    }

    @Override
    public void update(MenuBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        MenuDO entityDO = menuBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!menuManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update menu");
        }
    }

    @Override
    public MenuBO selectById(Long id) {
        MenuDO entityDO = getDOById(id, true);
        return menuBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<MenuBO> selectByPage(MenuQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<MenuDO> entityPageDO = menuManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return menuBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     *
     *
     * @param entityQuery {@link MenuQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<MenuDO> fuzzyQuery(MenuQuery entityQuery) {
        LambdaQueryWrapper<MenuDO> wrapper = Wrappers.<MenuDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getMenuName()), MenuDO::getMenuName, entityQuery.getMenuName());
        wrapper.eq(MenuDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     *
     *
     * @param entityBO       {@link MenuBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(MenuBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<MenuDO> wrapper = Wrappers.<MenuDO>query().lambda();
        wrapper.eq(MenuDO::getParentMenuId, entityBO.getParentMenuId());
        wrapper.eq(MenuDO::getMenuTypeFlag, entityBO.getMenuTypeFlag());
        wrapper.eq(MenuDO::getMenuName, entityBO.getMenuName());
        wrapper.eq(MenuDO::getMenuCode, entityBO.getMenuCode());
        wrapper.eq(MenuDO::getTenantId, entityBO.getTenantId());
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
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link MenuDO}
     */
    private MenuDO getDOById(Long id, boolean throwException) {
        MenuDO entityDO = menuManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Menu does not exist");
        }
        return entityDO;
    }

}

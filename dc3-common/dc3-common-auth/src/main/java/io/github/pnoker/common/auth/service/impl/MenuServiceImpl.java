/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
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
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * Menu Service Impl
 * </p>
 *
 * @author pnoker
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

        // 删除菜单之前需要检查该菜单是否存在关联
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
     * 构造模糊查询
     *
     * @param entityQuery {@link MenuQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<MenuDO> fuzzyQuery(MenuQuery entityQuery) {
        LambdaQueryWrapper<MenuDO> wrapper = Wrappers.<MenuDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getMenuName()), MenuDO::getMenuName, entityQuery.getMenuName());
        wrapper.eq(MenuDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link MenuBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
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
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
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

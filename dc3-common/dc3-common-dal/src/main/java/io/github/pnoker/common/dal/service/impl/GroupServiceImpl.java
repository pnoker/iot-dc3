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

package io.github.pnoker.common.dal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.dal.dal.GroupManager;
import io.github.pnoker.common.dal.entity.bo.GroupBO;
import io.github.pnoker.common.dal.entity.builder.GroupBuilder;
import io.github.pnoker.common.dal.entity.model.GroupDO;
import io.github.pnoker.common.dal.entity.query.GroupQuery;
import io.github.pnoker.common.dal.service.GroupService;
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
 * Group Service Impl
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Resource
    private GroupBuilder groupBuilder;

    @Resource
    private GroupManager groupManager;

    @Override
    public void save(GroupBO entityBO) {
        checkDuplicate(entityBO, false, true);

        GroupDO entityDO = groupBuilder.buildDOByBO(entityBO);
        if (!groupManager.save(entityDO)) {
            throw new AddException("Failed to create group");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // Before deleting a group, check whether it has any child groups associated.
        LambdaQueryChainWrapper<GroupDO> wrapper = groupManager.lambdaQuery().eq(GroupDO::getParentGroupId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove group: there are subgroups under the group");
        }

        if (!groupManager.removeById(id)) {
            throw new DeleteException("Failed to remove group");
        }
    }

    @Override
    public void update(GroupBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        GroupDO entityDO = groupBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!groupManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update group");
        }
    }

    @Override
    public GroupBO selectById(Long id) {
        GroupDO entityDO = getDOById(id, true);
        return groupBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<GroupBO> selectByPage(GroupQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<GroupDO> entityPageDO = groupManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return groupBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for group search.
     *
     * @param entityQuery {@link GroupQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link GroupDO}
     */
    private LambdaQueryWrapper<GroupDO> fuzzyQuery(GroupQuery entityQuery) {
        LambdaQueryWrapper<GroupDO> wrapper = Wrappers.<GroupDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getGroupName()), GroupDO::getGroupName, entityQuery.getGroupName());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), GroupDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * Check whether a group is duplicated under the same tenant and parent group.
     *
     * @param entityBO       {@link GroupBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(GroupBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<GroupDO> wrapper = Wrappers.<GroupDO>query().lambda();
        wrapper.eq(GroupDO::getGroupName, entityBO.getGroupName());
        wrapper.eq(GroupDO::getGroupTypeFlag, entityBO.getGroupTypeFlag());
        wrapper.eq(GroupDO::getParentGroupId, entityBO.getParentGroupId());
        wrapper.eq(GroupDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        GroupDO one = groupManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Group has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get group data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link GroupDO} if found, otherwise {@code null} when {@code throwException} is false
     */
    private GroupDO getDOById(Long id, boolean throwException) {
        GroupDO entityDO = groupManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Group does not exist");
        }
        return entityDO;
    }

}

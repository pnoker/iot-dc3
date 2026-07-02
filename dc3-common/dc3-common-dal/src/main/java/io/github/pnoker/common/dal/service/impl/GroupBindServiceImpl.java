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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.dal.dal.GroupBindManager;
import io.github.pnoker.common.dal.entity.bo.GroupBindBO;
import io.github.pnoker.common.dal.entity.builder.GroupBindBuilder;
import io.github.pnoker.common.dal.entity.model.GroupBindDO;
import io.github.pnoker.common.dal.entity.query.GroupBindQuery;
import io.github.pnoker.common.dal.service.GroupBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Business service implementation for group binding operations.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBindServiceImpl implements GroupBindService {

    private final GroupBindBuilder groupBindBuilder;

    private final GroupBindManager groupBindManager;

    @Override
    public void add(GroupBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        GroupBindDO entityDO = groupBindBuilder.buildDOByBO(entityBO);
        if (!groupBindManager.save(entityDO)) {
            throw new AddException("Failed to create group bind");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!groupBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove group bind");
        }
    }

    @Override
    public void update(GroupBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        GroupBindDO entityDO = groupBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!groupBindManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update group bind");
        }
    }

    @Override
    public GroupBindBO getById(Long id) {
        GroupBindDO entityDO = getDOById(id, true);
        return groupBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<GroupBindBO> list(GroupBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<GroupBindDO> entityPageDO = groupBindManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return groupBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for group binding search.
     *
     * @param entityQuery {@link GroupBindQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link GroupBindDO}
     */
    private LambdaQueryWrapper<GroupBindDO> fuzzyQuery(GroupBindQuery entityQuery) {
        LambdaQueryWrapper<GroupBindDO> wrapper = Wrappers.<GroupBindDO>query().lambda();
        wrapper.eq(Objects.nonNull(entityQuery.getEntityTypeFlag()), GroupBindDO::getEntityTypeFlag,
                Objects.isNull(entityQuery.getEntityTypeFlag()) ? null : entityQuery.getEntityTypeFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getGroupId()), GroupBindDO::getGroupId,
                entityQuery.getGroupId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getEntityId()), GroupBindDO::getEntityId,
                entityQuery.getEntityId());
        return wrapper;
    }

    /**
     * Check whether an entity already has a group binding under the same tenant.
     *
     * @param entityBO       {@link GroupBindBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(GroupBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<GroupBindDO> wrapper = Wrappers.<GroupBindDO>query().lambda();
        wrapper.eq(GroupBindDO::getEntityTypeFlag,
                Objects.isNull(entityBO.getEntityTypeFlag()) ? null : entityBO.getEntityTypeFlag().getIndex());
        wrapper.eq(GroupBindDO::getEntityId, entityBO.getEntityId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        GroupBindDO one = groupBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Entity has been bound to a group");
        }
        return duplicate;
    }

    /**
     * Get group binding data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link GroupBindDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private GroupBindDO getDOById(Long id, boolean throwException) {
        GroupBindDO entityDO = groupBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Group bind does not exist");
        }
        return entityDO;
    }

}

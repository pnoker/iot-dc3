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
import io.github.pnoker.common.dal.dal.LabelBindManager;
import io.github.pnoker.common.dal.entity.bo.LabelBindBO;
import io.github.pnoker.common.dal.entity.builder.LabelBindBuilder;
import io.github.pnoker.common.dal.entity.model.LabelBindDO;
import io.github.pnoker.common.dal.entity.query.LabelBindQuery;
import io.github.pnoker.common.dal.service.LabelBindService;
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
 * Business service implementation for label binding operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelBindServiceImpl implements LabelBindService {

    private final LabelBindBuilder labelBindBuilder;

    private final LabelBindManager labelBindManager;

    @Override
    public void add(LabelBindBO entityBO) {
        checkDuplicate(entityBO, false, true);

        LabelBindDO entityDO = labelBindBuilder.buildDOByBO(entityBO);
        if (!labelBindManager.save(entityDO)) {
            throw new AddException("Failed to create label bind");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!labelBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove label bind");
        }
    }

    @Override
    public void update(LabelBindBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        LabelBindDO entityDO = labelBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!labelBindManager.updateById(entityDO)) {
            throw new UpdateException("The label bind update failed");
        }
    }

    @Override
    public LabelBindBO getById(Long id) {
        LabelBindDO entityDO = getDOById(id, true);
        return labelBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<LabelBindBO> list(LabelBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<LabelBindDO> entityPageDO = labelBindManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return labelBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for label binding search.
     *
     * @param entityQuery {@link LabelBindQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link LabelBindDO}
     */
    private LambdaQueryWrapper<LabelBindDO> fuzzyQuery(LabelBindQuery entityQuery) {
        LambdaQueryWrapper<LabelBindDO> wrapper = Wrappers.<LabelBindDO>query().lambda();
        wrapper.eq(Objects.nonNull(entityQuery.getEntityTypeFlag()), LabelBindDO::getEntityTypeFlag,
                Objects.isNull(entityQuery.getEntityTypeFlag()) ? null : entityQuery.getEntityTypeFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getLabelId()), LabelBindDO::getLabelId,
                entityQuery.getLabelId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getEntityId()), LabelBindDO::getEntityId,
                entityQuery.getEntityId());
        return wrapper;
    }

    /**
     * Check whether a label binding is duplicated under the same tenant.
     *
     * @param entityBO       {@link LabelBindBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(LabelBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<LabelBindDO> wrapper = Wrappers.<LabelBindDO>query().lambda();
        wrapper.eq(LabelBindDO::getEntityTypeFlag,
                Objects.isNull(entityBO.getEntityTypeFlag()) ? null : entityBO.getEntityTypeFlag().getIndex());
        wrapper.eq(LabelBindDO::getLabelId, entityBO.getLabelId());
        wrapper.eq(LabelBindDO::getEntityId, entityBO.getEntityId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        LabelBindDO one = labelBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Label bind has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get label binding data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link LabelBindDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private LabelBindDO getDOById(Long id, boolean throwException) {
        LabelBindDO entityDO = labelBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Label bind does not exist");
        }
        return entityDO;
    }

}

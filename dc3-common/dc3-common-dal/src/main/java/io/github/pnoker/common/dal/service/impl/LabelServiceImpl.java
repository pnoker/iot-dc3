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
import io.github.pnoker.common.dal.dal.LabelManager;
import io.github.pnoker.common.dal.entity.bo.LabelBO;
import io.github.pnoker.common.dal.entity.builder.LabelBuilder;
import io.github.pnoker.common.dal.entity.model.LabelBindDO;
import io.github.pnoker.common.dal.entity.model.LabelDO;
import io.github.pnoker.common.dal.entity.query.LabelQuery;
import io.github.pnoker.common.dal.service.LabelService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Business service implementation for label operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {

    private final LabelBuilder labelBuilder;

    private final LabelManager labelManager;

    private final LabelBindManager labelBindManager;

    @Override
    public void add(LabelBO entityBO) {
        validateEntityType(entityBO.getEntityTypeFlag());
        checkDuplicate(entityBO, false, true);

        LabelDO entityDO = labelBuilder.buildDOByBO(entityBO);
        if (!labelManager.save(entityDO)) {
            throw new AddException("Failed to create label");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        // Before deleting a label, check whether it is already bound to any entity.
        LambdaQueryWrapper<LabelBindDO> wrapper = Wrappers.<LabelBindDO>query().lambda();
        wrapper.eq(LabelBindDO::getLabelId, id);
        long count = labelBindManager.count(wrapper);
        if (count > 0) {
            throw new AssociatedException("The label has been bound by another entity");
        }

        if (!labelManager.removeById(id)) {
            throw new DeleteException("Failed to remove label");
        }
    }

    @Override
    public void update(LabelBO entityBO) {
        getDOById(entityBO.getId(), true);

        validateEntityType(entityBO.getEntityTypeFlag());
        checkDuplicate(entityBO, true, true);

        LabelDO entityDO = labelBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!labelManager.updateById(entityDO)) {
            throw new UpdateException("The label update failed");
        }
    }

    @Override
    public LabelBO getById(Long id) {
        LabelDO entityDO = getDOById(id, false);
        return labelBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<LabelBO> list(LabelQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<LabelDO> entityPageDO = labelManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return labelBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for label search.
     *
     * @param entityQuery {@link LabelQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link LabelDO}
     */
    private LambdaQueryWrapper<LabelDO> fuzzyQuery(LabelQuery entityQuery) {
        LambdaQueryWrapper<LabelDO> wrapper = Wrappers.<LabelDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getLabelName()), LabelDO::getLabelName,
                entityQuery.getLabelName());
        wrapper.eq(Objects.nonNull(entityQuery.getEntityTypeFlag()), LabelDO::getEntityTypeFlag,
                Objects.isNull(entityQuery.getEntityTypeFlag()) ? null : entityQuery.getEntityTypeFlag().getIndex());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getColor()), LabelDO::getLabelColor, entityQuery.getColor());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), LabelDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    /**
     * Check whether a label is duplicated under the same tenant and entity type.
     *
     * @param entityBO       {@link LabelBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(LabelBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<LabelDO> wrapper = Wrappers.<LabelDO>query().lambda();
        wrapper.eq(LabelDO::getLabelName, entityBO.getLabelName());
        wrapper.eq(LabelDO::getEntityTypeFlag,
                Objects.isNull(entityBO.getEntityTypeFlag()) ? null : entityBO.getEntityTypeFlag().getIndex());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        LabelDO one = labelManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Label has been duplicated");
        }
        return duplicate;
    }

    /**
     * Validate that the entity type supports labelling.
     *
     * @param entityTypeFlag the entity type to check
     */
    private void validateEntityType(EntityTypeEnum entityTypeFlag) {
        if (!isSupportedEntityType(entityTypeFlag)) {
            throw new RequestException("Label entity type is not supported");
        }
    }

    /**
     * Return whether an entity type supports labelling (driver, profile, point, device).
     *
     * @param entityTypeFlag the entity type to check
     * @return true if supported
     */
    private boolean isSupportedEntityType(EntityTypeEnum entityTypeFlag) {
        return EntityTypeEnum.DRIVER == entityTypeFlag
                || EntityTypeEnum.PROFILE == entityTypeFlag
                || EntityTypeEnum.POINT == entityTypeFlag
                || EntityTypeEnum.DEVICE == entityTypeFlag;
    }

    /**
     * Get label data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link LabelDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private LabelDO getDOById(Long id, boolean throwException) {
        LabelDO entityDO = labelManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Label does not exist");
        }
        return entityDO;
    }

}

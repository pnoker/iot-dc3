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

package io.github.pnoker.common.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.data.dal.RuleStateManager;
import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.builder.RuleStateBuilder;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.data.entity.query.RuleStateQuery;
import io.github.pnoker.common.data.service.RuleStateService;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Rule runtime state service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleStateServiceImpl implements RuleStateService {

    private final RuleStateBuilder ruleStateBuilder;

    private final RuleStateManager ruleStateManager;

    @Override
    public void add(RuleStateBO entityBO) {
        checkDuplicate(entityBO, false, true);

        RuleStateDO entityDO = ruleStateBuilder.buildDOByBO(entityBO);
        if (!ruleStateManager.save(entityDO)) {
            throw new AddException("Failed to create rule state");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!ruleStateManager.removeById(id)) {
            throw new DeleteException("Failed to remove rule state");
        }
    }

    @Override
    public void update(RuleStateBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        RuleStateDO entityDO = ruleStateBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!ruleStateManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update rule state");
        }
    }

    @Override
    public RuleStateBO getById(Long id) {
        RuleStateDO entityDO = getDOById(id, true);
        return ruleStateBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<RuleStateBO> list(RuleStateQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RuleStateDO> entityPageDO = ruleStateManager.page(
                PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return ruleStateBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for rule state search.
     *
     * @param entityQuery {@link RuleStateQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link RuleStateDO}
     */
    private LambdaQueryWrapper<RuleStateDO> fuzzyQuery(RuleStateQuery entityQuery) {
        LambdaQueryWrapper<RuleStateDO> wrapper = Wrappers.<RuleStateDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getRuleId()), RuleStateDO::getRuleId,
                entityQuery.getRuleId());
        wrapper.eq(Objects.nonNull(entityQuery.getAlarmTargetTypeFlag()), RuleStateDO::getAlarmTargetTypeFlag,
                Objects.isNull(entityQuery.getAlarmTargetTypeFlag()) ? null
                        : entityQuery.getAlarmTargetTypeFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getEntityId()), RuleStateDO::getEntityId,
                entityQuery.getEntityId());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getFingerprint()), RuleStateDO::getFingerprint,
                entityQuery.getFingerprint());
        wrapper.eq(Objects.nonNull(entityQuery.getEntityStateFlag()), RuleStateDO::getEntityStateFlag,
                Objects.isNull(entityQuery.getEntityStateFlag()) ? null : entityQuery.getEntityStateFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getAlarmId()), RuleStateDO::getAlarmId,
                entityQuery.getAlarmId());
        return wrapper;
    }

    /**
     * Check whether a rule state is duplicated by rule, target type, entity, and
     * fingerprint.
     *
     * @param entityBO       {@link RuleStateBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(RuleStateBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<RuleStateDO> wrapper = Wrappers.<RuleStateDO>query().lambda();
        wrapper.eq(RuleStateDO::getRuleId, entityBO.getRuleId());
        wrapper.eq(RuleStateDO::getAlarmTargetTypeFlag,
                Objects.nonNull(entityBO.getAlarmTargetTypeFlag())
                        ? entityBO.getAlarmTargetTypeFlag().getIndex()
                        : null);
        wrapper.eq(RuleStateDO::getEntityId, entityBO.getEntityId());
        wrapper.eq(RuleStateDO::getFingerprint, entityBO.getFingerprint());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RuleStateDO one = ruleStateManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Rule state has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get rule state data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link RuleStateDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private RuleStateDO getDOById(Long id, boolean throwException) {
        RuleStateDO entityDO = ruleStateManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Rule state does not exist");
        }
        return entityDO;
    }

}

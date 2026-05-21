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
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.data.biz.alarm.RuleRegistry;
import io.github.pnoker.common.data.biz.alarm.WindowSpec;
import io.github.pnoker.common.data.biz.alarm.WindowSpecParser;
import io.github.pnoker.common.data.dal.RuleManager;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.data.entity.query.RuleQuery;
import io.github.pnoker.common.data.service.RuleService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UnSupportException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**

 * Business service implementation for alarm rule operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleBuilder ruleBuilder;

    private final RuleManager ruleManager;

    private final RuleRegistry ruleRegistry;

    @Override
    public void add(RuleBO entityBO) {
        validateWindowMode(entityBO);
        checkDuplicate(entityBO, false, true);

        RuleDO entityDO = ruleBuilder.buildDOByBO(entityBO);
        if (!ruleManager.save(entityDO)) {
            throw new AddException("Failed to create alarm rule");
        }
        ruleRegistry.invalidateTenant(entityBO.getTenantId());
    }

    @Override
    public void delete(Long id) {
        RuleDO existing = getDOById(id, true);

        // Alarm ruleAlarm rule
        LambdaQueryChainWrapper<RuleDO> wrapper = ruleManager.lambdaQuery().eq(RuleDO::getEntityId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove alarm rule: some sub alarm rules exists in the alarm rule");
        }

        if (!ruleManager.removeById(id)) {
            throw new DeleteException("Failed to remove alarm rule");
        }
        ruleRegistry.invalidateTenant(existing.getTenantId());
    }

    @Override
    public void update(RuleBO entityBO) {
        getDOById(entityBO.getId(), true);
        validateWindowMode(entityBO);

        checkDuplicate(entityBO, true, true);

        RuleDO entityDO = ruleBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!ruleManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update alarm rule");
        }
        ruleRegistry.invalidateTenant(entityBO.getTenantId());
    }

    @Override
    public RuleBO getById(Long id) {
        RuleDO entityDO = getDOById(id, true);
        return ruleBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<RuleBO> list(RuleQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RuleDO> entityPageDO = ruleManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return ruleBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link RuleQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<RuleDO> fuzzyQuery(RuleQuery entityQuery) {
        LambdaQueryWrapper<RuleDO> wrapper = Wrappers.<RuleDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getRuleName()), RuleDO::getRuleName,
                entityQuery.getRuleName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getRuleCode()), RuleDO::getRuleCode,
                entityQuery.getRuleCode());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getEntityId()), RuleDO::getEntityId,
                entityQuery.getEntityId());
        wrapper.eq(Objects.nonNull(entityQuery.getAlarmTargetTypeFlag()), RuleDO::getAlarmTargetTypeFlag,
                Objects.isNull(entityQuery.getAlarmTargetTypeFlag()) ? null
                        : entityQuery.getAlarmTargetTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), RuleDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), RuleDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * @param entityBO       {@link RuleBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(RuleBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<RuleDO> wrapper = Wrappers.<RuleDO>query().lambda();
        wrapper.eq(RuleDO::getRuleName, entityBO.getRuleName());
        wrapper.eq(RuleDO::getRuleCode, entityBO.getRuleCode());
        wrapper.eq(RuleDO::getEntityId, entityBO.getEntityId());
        wrapper.eq(RuleDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        RuleDO one = ruleManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Alarm rule has been duplicated");
        }
        return duplicate;
    }

    /**
     * Validate the rule's window block by parsing it through {@link WindowSpecParser}.
     * Rejects unknown modes, missing or non-positive durations on
     * aggregation modes, and malformed ISO-8601 duration strings — the same
     * rules the runtime evaluator will apply later.
     */
    private void validateWindowMode(RuleBO entityBO) {
        if (Objects.isNull(entityBO) || Objects.isNull(entityBO.getRuleExt())
                || Objects.isNull(entityBO.getRuleExt().getContent())) {
            return;
        }
        RuleExt.Window window = entityBO.getRuleExt().getContent().getWindow();
        if (Objects.isNull(window)) {
            return;
        }
        WindowSpec spec = WindowSpecParser.parse(window);
        if (!spec.valid()) {
            throw new UnSupportException("Invalid rule window: {}", spec.reason());
        }
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link RuleDO}
     */
    private RuleDO getDOById(Long id, boolean throwException) {
        RuleDO entityDO = ruleManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Alarm rule does not exist");
        }
        return entityDO;
    }

}

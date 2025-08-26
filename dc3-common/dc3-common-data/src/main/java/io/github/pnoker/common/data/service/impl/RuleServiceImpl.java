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

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.data.dal.RuleManager;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.data.entity.query.RuleQuery;
import io.github.pnoker.common.data.service.RuleService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * AlarmRule Service Impl
 * </p>
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class RuleServiceImpl implements RuleService {

    @Resource
    private RuleBuilder ruleBuilder;

    @Resource
    private RuleManager ruleManager;

    @Override
    public void save(RuleBO entityBO) {
        checkDuplicate(entityBO, false, true);

        RuleDO entityDO = ruleBuilder.buildDOByBO(entityBO);
        if (!ruleManager.save(entityDO)) {
            throw new AddException("Failed to create alarm rule");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除报警规则之前需要检查该报警规则是否存在关联
        LambdaQueryChainWrapper<RuleDO> wrapper = ruleManager.lambdaQuery().eq(RuleDO::getEntityId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove alarm rule: some sub alarm rules exists in the alarm rule");
        }

        if (!ruleManager.removeById(id)) {
            throw new DeleteException("Failed to remove alarm rule");
        }
    }

    @Override
    public void update(RuleBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        RuleDO entityDO = ruleBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!ruleManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update alarm rule");
        }
    }

    @Override
    public RuleBO selectById(Long id) {
        RuleDO entityDO = getDOById(id, true);
        return ruleBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<RuleBO> selectByPage(RuleQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<RuleDO> entityPageDO = ruleManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return ruleBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link RuleQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<RuleDO> fuzzyQuery(RuleQuery entityQuery) {
        LambdaQueryWrapper<RuleDO> wrapper = Wrappers.<RuleDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAlarmRuleName()), RuleDO::getRuleName, entityQuery.getAlarmRuleName());
        wrapper.eq(RuleDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link RuleBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
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
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
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

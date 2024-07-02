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

package io.github.pnoker.common.data.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.data.dal.AlarmRuleManager;
import io.github.pnoker.common.data.entity.bo.AlarmRuleBO;
import io.github.pnoker.common.data.entity.builder.AlarmRuleBuilder;
import io.github.pnoker.common.data.entity.model.AlarmRuleDO;
import io.github.pnoker.common.data.entity.query.AlarmRuleQuery;
import io.github.pnoker.common.data.service.AlarmRuleService;
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
 * @since 2022.1.0
 */
@Slf4j
@Service
public class AlarmRuleServiceImpl implements AlarmRuleService {

    @Resource
    private AlarmRuleBuilder alarmRuleBuilder;

    @Resource
    private AlarmRuleManager alarmRuleManager;

    @Override
    public void save(AlarmRuleBO entityBO) {
        checkDuplicate(entityBO, false, true);

        AlarmRuleDO entityDO = alarmRuleBuilder.buildDOByBO(entityBO);
        if (!alarmRuleManager.save(entityDO)) {
            throw new AddException("Failed to create alarm rule");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除报警规则之前需要检查该报警规则是否存在关联
        LambdaQueryChainWrapper<AlarmRuleDO> wrapper = alarmRuleManager.lambdaQuery().eq(AlarmRuleDO::getPointId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove alarm rule: some sub alarm rules exists in the alarm rule");
        }

        if (!alarmRuleManager.removeById(id)) {
            throw new DeleteException("Failed to remove alarm rule");
        }
    }

    @Override
    public void update(AlarmRuleBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        AlarmRuleDO entityDO = alarmRuleBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!alarmRuleManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update alarm rule");
        }
    }

    @Override
    public AlarmRuleBO selectById(Long id) {
        AlarmRuleDO entityDO = getDOById(id, true);
        return alarmRuleBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<AlarmRuleBO> selectByPage(AlarmRuleQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<AlarmRuleDO> entityPageDO = alarmRuleManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return alarmRuleBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link AlarmRuleQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<AlarmRuleDO> fuzzyQuery(AlarmRuleQuery entityQuery) {
        LambdaQueryWrapper<AlarmRuleDO> wrapper = Wrappers.<AlarmRuleDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAlarmRuleName()), AlarmRuleDO::getAlarmRuleName, entityQuery.getAlarmRuleName());
        wrapper.eq(AlarmRuleDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link AlarmRuleBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(AlarmRuleBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<AlarmRuleDO> wrapper = Wrappers.<AlarmRuleDO>query().lambda();
        wrapper.eq(AlarmRuleDO::getAlarmRuleName, entityBO.getAlarmRuleName());
        wrapper.eq(AlarmRuleDO::getAlarmRuleCode, entityBO.getAlarmRuleCode());
        wrapper.eq(AlarmRuleDO::getPointId, entityBO.getPointId());
        wrapper.eq(AlarmRuleDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        AlarmRuleDO one = alarmRuleManager.getOne(wrapper);
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
     * @return {@link AlarmRuleDO}
     */
    private AlarmRuleDO getDOById(Long id, boolean throwException) {
        AlarmRuleDO entityDO = alarmRuleManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Alarm rule does not exist");
        }
        return entityDO;
    }

}

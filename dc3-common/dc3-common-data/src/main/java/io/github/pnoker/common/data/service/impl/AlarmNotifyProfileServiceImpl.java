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
import io.github.pnoker.common.data.dal.AlarmNotifyProfileManager;
import io.github.pnoker.common.data.entity.bo.AlarmNotifyProfileBO;
import io.github.pnoker.common.data.entity.builder.AlarmNotifyProfileBuilder;
import io.github.pnoker.common.data.entity.model.AlarmNotifyProfileDO;
import io.github.pnoker.common.data.entity.query.AlarmNotifyProfileQuery;
import io.github.pnoker.common.data.service.AlarmNotifyProfileService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * AlarmNotifyProfile Service Impl
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class AlarmNotifyProfileServiceImpl implements AlarmNotifyProfileService {

    @Resource
    private AlarmNotifyProfileBuilder alarmNotifyProfileBuilder;

    @Resource
    private AlarmNotifyProfileManager alarmNotifyProfileManager;

    @Override
    public void save(AlarmNotifyProfileBO entityBO) {
        checkDuplicate(entityBO, false, true);

        AlarmNotifyProfileDO entityDO = alarmNotifyProfileBuilder.buildDOByBO(entityBO);
        if (!alarmNotifyProfileManager.save(entityDO)) {
            throw new AddException("Failed to create alarm notify profile");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除报警通知模板之前需要检查该报警通知模板是否存在关联
        LambdaQueryChainWrapper<AlarmNotifyProfileDO> wrapper = alarmNotifyProfileManager.lambdaQuery().eq(AlarmNotifyProfileDO::getTenantId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove alarm notify profile: some sub alarm notify profiles exists in the alarm notify profile");
        }

        if (!alarmNotifyProfileManager.removeById(id)) {
            throw new DeleteException("Failed to remove alarm notify profile");
        }
    }

    @Override
    public void update(AlarmNotifyProfileBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        AlarmNotifyProfileDO entityDO = alarmNotifyProfileBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!alarmNotifyProfileManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update alarm notify profile");
        }
    }

    @Override
    public AlarmNotifyProfileBO selectById(Long id) {
        AlarmNotifyProfileDO entityDO = getDOById(id, true);
        return alarmNotifyProfileBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<AlarmNotifyProfileBO> selectByPage(AlarmNotifyProfileQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<AlarmNotifyProfileDO> entityPageDO = alarmNotifyProfileManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return alarmNotifyProfileBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link AlarmNotifyProfileQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<AlarmNotifyProfileDO> fuzzyQuery(AlarmNotifyProfileQuery entityQuery) {
        LambdaQueryWrapper<AlarmNotifyProfileDO> wrapper = Wrappers.<AlarmNotifyProfileDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAlarmNotifyName()), AlarmNotifyProfileDO::getAlarmNotifyName, entityQuery.getAlarmNotifyName());
        wrapper.eq(AlarmNotifyProfileDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link AlarmNotifyProfileBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(AlarmNotifyProfileBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<AlarmNotifyProfileDO> wrapper = Wrappers.<AlarmNotifyProfileDO>query().lambda();
        wrapper.eq(AlarmNotifyProfileDO::getAlarmNotifyName, entityBO.getAlarmNotifyName());
        wrapper.eq(AlarmNotifyProfileDO::getAlarmNotifyCode, entityBO.getAlarmNotifyCode());
        wrapper.eq(AlarmNotifyProfileDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        AlarmNotifyProfileDO one = alarmNotifyProfileManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Alarm notify profile has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link AlarmNotifyProfileDO}
     */
    private AlarmNotifyProfileDO getDOById(Long id, boolean throwException) {
        AlarmNotifyProfileDO entityDO = alarmNotifyProfileManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Alarm notify profile does not exist");
        }
        return entityDO;
    }

}

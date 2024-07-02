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
import io.github.pnoker.common.data.dal.AlarmMessageProfileManager;
import io.github.pnoker.common.data.entity.bo.AlarmMessageProfileBO;
import io.github.pnoker.common.data.entity.builder.AlarmMessageProfileBuilder;
import io.github.pnoker.common.data.entity.model.AlarmMessageProfileDO;
import io.github.pnoker.common.data.entity.query.AlarmMessageProfileQuery;
import io.github.pnoker.common.data.service.AlarmMessageProfileService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * AlarmMessageProfile Service Impl
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class AlarmMessageProfileServiceImpl implements AlarmMessageProfileService {

    @Resource
    private AlarmMessageProfileBuilder alarmMessageProfileBuilder;

    @Resource
    private AlarmMessageProfileManager alarmMessageProfileManager;

    @Override
    public void save(AlarmMessageProfileBO entityBO) {
        checkDuplicate(entityBO, false, true);

        AlarmMessageProfileDO entityDO = alarmMessageProfileBuilder.buildDOByBO(entityBO);
        if (!alarmMessageProfileManager.save(entityDO)) {
            throw new AddException("Failed to create group");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除分组之前需要检查该分组是否存在关联
        LambdaQueryChainWrapper<AlarmMessageProfileDO> wrapper = alarmMessageProfileManager.lambdaQuery()
                .eq(AlarmMessageProfileDO::getTenantId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove group: there are subgroups under the group");
        }

        if (!alarmMessageProfileManager.removeById(id)) {
            throw new DeleteException("Failed to remove group");
        }
    }

    @Override
    public void update(AlarmMessageProfileBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        AlarmMessageProfileDO entityDO = alarmMessageProfileBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!alarmMessageProfileManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update group");
        }
    }

    @Override
    public AlarmMessageProfileBO selectById(Long id) {
        AlarmMessageProfileDO entityDO = getDOById(id, true);
        return alarmMessageProfileBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<AlarmMessageProfileBO> selectByPage(AlarmMessageProfileQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<AlarmMessageProfileDO> entityPageDO = alarmMessageProfileManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return alarmMessageProfileBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link AlarmMessageProfileQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<AlarmMessageProfileDO> fuzzyQuery(AlarmMessageProfileQuery entityQuery) {
        LambdaQueryWrapper<AlarmMessageProfileDO> wrapper = Wrappers.<AlarmMessageProfileDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAlarmMessageTitle()), AlarmMessageProfileDO::getAlarmMessageTitle, entityQuery.getAlarmMessageTitle());
        wrapper.eq(AlarmMessageProfileDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link AlarmMessageProfileBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(AlarmMessageProfileBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<AlarmMessageProfileDO> wrapper = Wrappers.<AlarmMessageProfileDO>query().lambda();
        wrapper.eq(AlarmMessageProfileDO::getAlarmMessageTitle, entityBO.getAlarmMessageTitle());
        wrapper.eq(AlarmMessageProfileDO::getAlarmMessageCode, entityBO.getAlarmMessageCode());
        wrapper.eq(AlarmMessageProfileDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        AlarmMessageProfileDO one = alarmMessageProfileManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Alarm message profile has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link AlarmMessageProfileDO}
     */
    private AlarmMessageProfileDO getDOById(Long id, boolean throwException) {
        AlarmMessageProfileDO entityDO = alarmMessageProfileManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Alarm message profile does not exist");
        }
        return entityDO;
    }

}

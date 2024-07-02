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

package io.github.pnoker.common.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.manager.dal.DriverAttributeManager;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.common.manager.entity.model.DriverAttributeDO;
import io.github.pnoker.common.manager.entity.query.DriverAttributeQuery;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * DriverAttributeService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverAttributeServiceImpl implements DriverAttributeService {

    @Resource
    private DriverAttributeBuilder driverAttributeBuilder;

    @Resource
    private DriverAttributeManager driverAttributeManager;

    @Override
    public void save(DriverAttributeBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create driver attribute: driver attribute has been duplicated");
        }

        DriverAttributeDO entityDO = driverAttributeBuilder.buildDOByBO(entityBO);
        if (!driverAttributeManager.save(entityDO)) {
            throw new AddException("Failed to create driver attribute");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!driverAttributeManager.removeById(id)) {
            throw new DeleteException("Failed to remove driver attribute");
        }
    }

    @Override
    public void update(DriverAttributeBO entityBO) {
        getDOById(entityBO.getId(), true);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update driver attribute: driver attribute has been duplicated");
        }

        DriverAttributeDO entityDO = driverAttributeBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!driverAttributeManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update driver attribute");
        }
    }

    @Override
    public DriverAttributeBO selectById(Long id) {
        DriverAttributeDO entityDO = getDOById(id, true);
        return driverAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public DriverAttributeBO selectByNameAndDriverId(String name, Long driverId) {
        LambdaQueryChainWrapper<DriverAttributeDO> wrapper = driverAttributeManager.lambdaQuery()
                .eq(DriverAttributeDO::getAttributeName, name)
                .eq(DriverAttributeDO::getDriverId, driverId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeDO entityDO = wrapper.one();
        return driverAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<DriverAttributeBO> selectByDriverId(Long driverId) {
        LambdaQueryChainWrapper<DriverAttributeDO> wrapper = driverAttributeManager.lambdaQuery()
                .eq(DriverAttributeDO::getDriverId, driverId);
        List<DriverAttributeDO> entityDO = wrapper.list();
        return driverAttributeBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<DriverAttributeBO> selectByPage(DriverAttributeQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DriverAttributeDO> entityPageDO = driverAttributeManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return driverAttributeBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link DriverAttributeQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<DriverAttributeDO> fuzzyQuery(DriverAttributeQuery entityQuery) {
        LambdaQueryWrapper<DriverAttributeDO> wrapper = Wrappers.<DriverAttributeDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAttributeName()), DriverAttributeDO::getAttributeName, entityQuery.getAttributeName());
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getDisplayName()), DriverAttributeDO::getDisplayName, entityQuery.getDisplayName());
        wrapper.eq(Objects.nonNull(entityQuery.getAttributeTypeFlag()), DriverAttributeDO::getAttributeTypeFlag, entityQuery.getAttributeTypeFlag());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), DriverAttributeDO::getDriverId, entityQuery.getDriverId());
        wrapper.eq(DriverAttributeDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO {@link DriverAttributeBO}
     * @param isUpdate 是否为更新操作
     * @return 是否重复
     */
    private boolean checkDuplicate(DriverAttributeBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<DriverAttributeDO> wrapper = Wrappers.<DriverAttributeDO>query().lambda();
        wrapper.eq(DriverAttributeDO::getAttributeName, entityBO.getAttributeName());
        wrapper.eq(DriverAttributeDO::getDriverId, entityBO.getDriverId());
        wrapper.eq(DriverAttributeDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeDO one = driverAttributeManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link DriverAttributeDO}
     */
    private DriverAttributeDO getDOById(Long id, boolean throwException) {
        DriverAttributeDO entityDO = driverAttributeManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Driver attribute does not exist");
        }
        return entityDO;
    }

}

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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.dal.DriverAttributeManager;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.center.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.center.manager.entity.model.DriverAttributeDO;
import io.github.pnoker.center.manager.entity.query.DriverAttributeQuery;
import io.github.pnoker.center.manager.service.DriverAttributeService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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
        checkDuplicate(entityBO, false, true);

        DriverAttributeDO entityDO = driverAttributeBuilder.buildDOByBO(entityBO);
        if (!driverAttributeManager.save(entityDO)) {
            throw new AddException("驱动属性创建失败");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!driverAttributeManager.removeById(id)) {
            throw new DeleteException("驱动属性删除失败");
        }
    }

    @Override
    public void update(DriverAttributeBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        DriverAttributeDO entityDO = driverAttributeBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!driverAttributeManager.updateById(entityDO)) {
            throw new UpdateException("驱动属性更新失败");
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
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DriverAttributeDO> entityPageDO = driverAttributeManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return driverAttributeBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<DriverAttributeDO> fuzzyQuery(DriverAttributeQuery entityQuery) {
        LambdaQueryWrapper<DriverAttributeDO> wrapper = Wrappers.<DriverAttributeDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAttributeName()), DriverAttributeDO::getAttributeName, entityQuery.getAttributeName());
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getDisplayName()), DriverAttributeDO::getDisplayName, entityQuery.getDisplayName());
        wrapper.eq(ObjectUtil.isNotNull(entityQuery.getAttributeTypeFlag()), DriverAttributeDO::getAttributeTypeFlag, entityQuery.getAttributeTypeFlag());
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getDriverId()), DriverAttributeDO::getDriverId, entityQuery.getDriverId());
        wrapper.eq(DriverAttributeDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link DriverAttributeBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(DriverAttributeBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<DriverAttributeDO> wrapper = Wrappers.<DriverAttributeDO>query().lambda();
        wrapper.eq(DriverAttributeDO::getAttributeName, entityBO.getAttributeName());
        wrapper.eq(DriverAttributeDO::getDriverId, entityBO.getDriverId());
        wrapper.eq(DriverAttributeDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeDO one = driverAttributeManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("驱动属性重复");
        }
        return duplicate;
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
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("驱动属性不存在");
        }
        return entityDO;
    }

}

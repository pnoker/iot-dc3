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
import io.github.pnoker.center.manager.dal.PointAttributeManager;
import io.github.pnoker.center.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.center.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.center.manager.entity.model.PointAttributeDO;
import io.github.pnoker.center.manager.entity.query.PointAttributeQuery;
import io.github.pnoker.center.manager.service.PointAttributeService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * PointAttributeService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointAttributeServiceImpl implements PointAttributeService {

    @Resource
    private PointAttributeBuilder pointAttributeBuilder;

    @Resource
    private PointAttributeManager pointAttributeManager;

    @Override
    public void save(PointAttributeBO entityBO) {
        checkDuplicate(entityBO, false, true);

        PointAttributeDO entityDO = pointAttributeBuilder.buildDOByBO(entityBO);
        if (!pointAttributeManager.save(entityDO)) {
            throw new AddException("位号属性创建失败");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!pointAttributeManager.removeById(id)) {
            throw new DeleteException("位号属性删除失败");
        }
    }

    @Override
    public void update(PointAttributeBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        PointAttributeDO entityDO = pointAttributeBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!pointAttributeManager.updateById(entityDO)) {
            throw new UpdateException("位号属性更新失败");
        }
    }

    @Override
    public PointAttributeBO selectById(Long id) {
        PointAttributeDO entityDO = getDOById(id, true);
        return pointAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public PointAttributeBO selectByNameAndDriverId(String name, Long driverId) {
        LambdaQueryChainWrapper<PointAttributeDO> wrapper = pointAttributeManager.lambdaQuery()
                .eq(PointAttributeDO::getAttributeName, name)
                .eq(PointAttributeDO::getDriverId, driverId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeDO entityDO = wrapper.one();
        return pointAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<PointAttributeBO> selectByDriverId(Long driverId, boolean throwException) {
        LambdaQueryChainWrapper<PointAttributeDO> wrapper = pointAttributeManager.lambdaQuery()
                .eq(PointAttributeDO::getDriverId, driverId);
        List<PointAttributeDO> entityDO = wrapper.list();
        return pointAttributeBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<PointAttributeBO> selectByPage(PointAttributeQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointAttributeDO> entityPageDO = pointAttributeManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return pointAttributeBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<PointAttributeDO> fuzzyQuery(PointAttributeQuery entityQuery) {
        LambdaQueryWrapper<PointAttributeDO> wrapper = Wrappers.<PointAttributeDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAttributeName()), PointAttributeDO::getAttributeName, entityQuery.getAttributeName());
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getDisplayName()), PointAttributeDO::getDisplayName, entityQuery.getDisplayName());
        wrapper.eq(ObjectUtil.isNotNull(entityQuery.getAttributeTypeFlag()), PointAttributeDO::getAttributeTypeFlag, entityQuery.getAttributeTypeFlag());
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getDriverId()), PointAttributeDO::getDriverId, entityQuery.getDriverId());
        wrapper.eq(PointAttributeDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link PointAttributeBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(PointAttributeBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<PointAttributeDO> wrapper = Wrappers.<PointAttributeDO>query().lambda();
        wrapper.eq(PointAttributeDO::getAttributeName, entityBO.getAttributeName());
        wrapper.eq(PointAttributeDO::getDriverId, entityBO.getDriverId());
        wrapper.eq(PointAttributeDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeDO one = pointAttributeManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("位号属性重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link PointAttributeDO}
     */
    private PointAttributeDO getDOById(Long id, boolean throwException) {
        PointAttributeDO entityDO = pointAttributeManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("位号属性不存在");
        }
        return entityDO;
    }

}

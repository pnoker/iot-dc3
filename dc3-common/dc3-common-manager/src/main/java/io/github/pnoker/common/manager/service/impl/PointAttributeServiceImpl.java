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
import io.github.pnoker.common.manager.dal.PointAttributeManager;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.common.manager.entity.model.PointAttributeDO;
import io.github.pnoker.common.manager.entity.query.PointAttributeQuery;
import io.github.pnoker.common.manager.service.PointAttributeService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
            throw new AddException("Failed to create point attribute");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!pointAttributeManager.removeById(id)) {
            throw new DeleteException("Failed to remove point attribute");
        }
    }

    @Override
    public void update(PointAttributeBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        PointAttributeDO entityDO = pointAttributeBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!pointAttributeManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update point attribute");
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
    public List<PointAttributeBO> selectByDriverId(Long driverId) {
        LambdaQueryChainWrapper<PointAttributeDO> wrapper = pointAttributeManager.lambdaQuery()
                .eq(PointAttributeDO::getDriverId, driverId);
        List<PointAttributeDO> entityDO = wrapper.list();
        return pointAttributeBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<PointAttributeBO> selectByPage(PointAttributeQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointAttributeDO> entityPageDO = pointAttributeManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return pointAttributeBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link PointAttributeQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<PointAttributeDO> fuzzyQuery(PointAttributeQuery entityQuery) {
        LambdaQueryWrapper<PointAttributeDO> wrapper = Wrappers.<PointAttributeDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAttributeName()), PointAttributeDO::getAttributeName, entityQuery.getAttributeName());
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getDisplayName()), PointAttributeDO::getDisplayName, entityQuery.getDisplayName());
        wrapper.eq(Objects.nonNull(entityQuery.getAttributeTypeFlag()), PointAttributeDO::getAttributeTypeFlag, entityQuery.getAttributeTypeFlag());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), PointAttributeDO::getDriverId, entityQuery.getDriverId());
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
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Point attribute has been duplicated");
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
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Point attribute does not exist");
        }
        return entityDO;
    }

}

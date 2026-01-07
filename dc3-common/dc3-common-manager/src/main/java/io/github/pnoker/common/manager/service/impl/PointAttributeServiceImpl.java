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

package io.github.pnoker.common.manager.service.impl;

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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * PointAttributeService Impl
 *
 * @author pnoker
 * @version 2025.9.0
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
                .eq(PointAttributeDO::getAttributeCode, name)
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
     *
     *
     * @param entityQuery {@link PointAttributeQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<PointAttributeDO> fuzzyQuery(PointAttributeQuery entityQuery) {
        LambdaQueryWrapper<PointAttributeDO> wrapper = Wrappers.<PointAttributeDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getAttributeCode()), PointAttributeDO::getAttributeCode, entityQuery.getAttributeCode());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getAttributeName()), PointAttributeDO::getAttributeName, entityQuery.getAttributeName());
        wrapper.eq(Objects.nonNull(entityQuery.getAttributeTypeFlag()), PointAttributeDO::getAttributeTypeFlag, entityQuery.getAttributeTypeFlag());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), PointAttributeDO::getDriverId, entityQuery.getDriverId());
        wrapper.eq(PointAttributeDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     *
     *
     * @param entityBO       {@link PointAttributeBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(PointAttributeBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<PointAttributeDO> wrapper = Wrappers.<PointAttributeDO>query().lambda();
        wrapper.eq(PointAttributeDO::getAttributeCode, entityBO.getAttributeCode());
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
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
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
